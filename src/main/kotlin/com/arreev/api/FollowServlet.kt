
package com.arreev.api

import javax.servlet.http.*

import com.google.gson.*
import com.google.cloud.datastore.*

/**
 *
 */
class FollowServlet : HttpServlet()
{
    private val datastore = DatastoreOptions.getDefaultInstance().service
    private val gson = GsonBuilder().create()

    class FollowResponse : APIResponse()
    {
        var follow: Follow? = null
        var debug: String? = null
    }

    override fun doOptions(request: HttpServletRequest, response: HttpServletResponse) {
        if (!verifySSL(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        response.status = 200
        response.setHeader("access-control-allow-headers", "Content-Type,Authorization,arreev-api-key")
        response.setHeader("access-control-allow-methods", "OPTIONS,GET,POST")
        response.setHeader("access-control-allow-origin", "*")
    }

    override fun doPost(request: HttpServletRequest, response: HttpServletResponse) {
        if (!verifySSL(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        response.status = 200
        response.contentType = "application/json"
        response.setHeader("access-control-allow-headers", "Content-Type,Authorization,arreev-api-key")
        response.setHeader("access-control-allow-methods", "OPTIONS,GET,POST")
        response.setHeader("access-control-allow-origin", "*")

        val r = FollowResponse()
        var ok = false

        var transaction: Transaction? = null
        try {
            val ownerid = request.getParameter("ownerid" )
            val fleetid = request.getParameter("fleetid" )
            val transporterid = request.getParameter("transporterid" )

            val body = body( request )
            val follow = gson.fromJson<Follow>(body ?: "{}",Follow::class.java )
            if ( follow?.equals( "{}" ) ?: false ) { throw GarbageInException( "invalid follow data" ); }

            transaction = datastore.newTransaction() // could throw DataStoreException
            val keyFactory = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "follow" );

            if ( follow.id == null ) {
                /*
                 * create
                 */
                val virignkey = keyFactory.newKey()
                val fullentity = Entity.newBuilder( virignkey )
                        .set( "name",follow.name ?: "" )
                        .set( "notifyWhenArrive",follow.notifyWhenArrive ?: false )
                        .set( "notifyWhenDepart",follow.notifyWhenDepart ?: false )
                        .set( "notifyWhenDelayed",follow.notifyWhenDelayed ?: false )
                        .set( "subscribeToMessages",follow.subscribeToMessages ?: false )
                        .set( "subscribeToWarnings",follow.subscribeToWarnings ?: false )
                        .set( "ownerid",ownerid )
                        .set( "fleetid",fleetid )
                        .set( "transporterid",transporterid )
                        .set( "status","active" )
                        .build()
                val e = transaction.add( fullentity )
                r.follow = asFollow( e )
                r.debug = "created"
            } else {
                /*
                 * update
                 */
                val immutableid = follow.id ?: ""
                val key = keyFactory.newKey( immutableid.toLong() )
                val entity = transaction.get( key )
                if ( !verifyOwnership( entity,ownerid ) ) { throw Exception( "ownerid mis-match" ) }
                val _name = follow.name ?: entity.getString("name" )
                val _notifyWhenArrive = follow.notifyWhenArrive ?: entity.getBooleanOr("notifyWhenArrive",false )
                val _notifyWhenDepart = follow.notifyWhenDepart ?: entity.getBooleanOr("notifyWhenDepart",false )
                val _notifyWhenDelayed = follow.notifyWhenDelayed ?: entity.getBooleanOr("notifyWhenDelayed",false )
                val _subscribeToMessages = follow.subscribeToMessages ?: entity.getBooleanOr("subscribeToMessages",false )
                val _subscribeToWarnings = follow.subscribeToWarnings ?: entity.getBooleanOr("subscribeToWarnings",false )
                val _status = follow.status ?: entity.getString("status" )
                val fullentity = Entity.newBuilder( entity )
                        .set( "name",_name )
                        .set( "notifyWhenArrive",_notifyWhenArrive )
                        .set( "notifyWhenDepart",_notifyWhenDepart )
                        .set( "notifyWhenDelayed",_notifyWhenDelayed )
                        .set( "subscribeToMessages",_subscribeToMessages )
                        .set( "subscribeToWarnings",_subscribeToWarnings )
                        .set( "status",_status )
                        .build()
                val e = transaction.put( fullentity )
                r.follow = asFollow( e )
                r.debug = "updated"
            }

            val commitresponse = transaction?.commit()
            r.debug += commitresponse?.toString() ?: ""

            ok = true
        } catch ( x:GarbageInException ) {
            response.sendError(400,x.message )
        } catch ( x:DatastoreException ) {
            response.sendError(500,x.message )
        } finally {
            if ( transaction?.isActive ?: false ) {
                transaction?.rollback()
            }
        }

        if ( ok ) {
            val json = gson.toJson( r,FollowServlet.FollowResponse::class.java )
            response.writer.write( json )
        }
    }
}