
package com.arreev.api

import javax.servlet.http.*

import com.google.gson.*
import com.google.cloud.datastore.*

/*
 * https://cloud.google.com/java/getting-started/using-cloud-datastore
 * https://console.cloud.google.com/logs/viewer
 * https://cloud.google.com/appengine/docs/standard/java/tools/using-local-server
 */
class FollowServlet : HttpServlet()
{
    private val datastore = DatastoreOptions.getDefaultInstance().service
    private val gson = GsonBuilder().create()

    class FollowResponse : APIResponse()
    {
        var follow: Follow? = null
        var id: String?  = null
        var debug: String? = null
    }

    override fun doOptions( request:HttpServletRequest,response:HttpServletResponse ) {
        if ( !verifySSL( request ) ) {
            response.sendError( HttpServletResponse.SC_FORBIDDEN );
            return;
        }

        response.status = 200
        response.setHeader("access-control-allow-headers","Content-Type,Authorization,arreev-api-key" )
        response.setHeader("access-control-allow-methods","OPTIONS,GET,POST,DELETE" )
        response.setHeader("access-control-allow-origin","*" )
    }

    override fun doGet( request:HttpServletRequest,response:HttpServletResponse ) {
        if ( !verifySSL( request ) ) {
            response.sendError( HttpServletResponse.SC_FORBIDDEN );
            return;
        }

        response.status = 200
        response.contentType = "application/json"
        response.setHeader("access-control-allow-headers","Content-Type,Authorization,arreev-api-key" )
        response.setHeader("access-control-allow-methods","GET" )
        response.setHeader("access-control-allow-origin","*" )

        val r = FollowResponse()

        try {
            val id = request.getParameter("id" )

            val keyFactory = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "follow" )
            val entity = datastore.get( keyFactory.newKey( id.toLong() ) )
            if ( entity != null ) {
                val follow = Follow()
                follow.id = "${entity.key.id}";
                follow.name = entity.getString("name" )
                follow.notifyWhenArrive = entity.getBoolean("notifyWhenArrive" )
                follow.notifyWhenDepart = entity.getBoolean("notifyWhenDepart" )
                follow.notifyWhenDelayed = entity.getBoolean("notifyWhenDelayed" )
                follow.subscribeToMessages = entity.getBoolean("subscribeToMessages" )
                follow.subscribeToWarnings = entity.getBoolean("subscribeToWarnings" )
                follow.transporterid = entity.getString("transporterid" )
                follow.status = entity.getString("status" )
                r.follow = follow;
                r.debug = "found ${r?.follow?.name}"
            } else {
                r.debug = "not found: ${id}"
            }
        } catch ( x:DatastoreException ) {
            response.sendError(500,x.message )
        } finally {
        }

        val json = gson.toJson( r,FollowResponse::class.java )
        response.writer.write( json )
    }

    /*
     * TODO: on all these upserts, should be enforcing unique ownerid,fleetid,?
     * https://cloud.google.com/datastore/docs/reference/data/rest/v1/Key
     * https://cloud.google.com/datastore/docs/concepts/entities#datastore-upsert-java
     *
     */
    override fun doPost( request:HttpServletRequest,response:HttpServletResponse ) {
        if ( !verifySSL( request ) ) {
            response.sendError( HttpServletResponse.SC_FORBIDDEN );
            return;
        }

        response.status = 200
        response.contentType = "application/json"
        response.setHeader("access-control-allow-headers","Content-Type,Authorization,arreev-api-key" )
        response.setHeader("access-control-allow-methods","POST" )
        response.setHeader("access-control-allow-origin","*" )

        val r = FollowResponse()

        var transaction: Transaction? = null
        try {
            val ownerid = request.getParameter("ownerid" )
            val fleetid = request.getParameter("fleetid" )

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
                        .set( "ownerid",ownerid )
                        .set( "fleetid",fleetid )
                        .set( "name",follow.name ?: "" )
                        .set( "notifyWhenArrive",follow.notifyWhenArrive ?: false )
                        .set( "notifyWhenDepart",follow.notifyWhenDepart ?: false )
                        .set( "notifyWhenDelayed",follow.notifyWhenDelayed ?: false )
                        .set( "subscribeToMessages",follow.subscribeToMessages ?: false )
                        .set( "subscribeToWarnings",follow.subscribeToWarnings ?: false )
                        .set( "transporterid",follow.transporterid )
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
                val _notifyWhenArrive = follow.notifyWhenArrive ?: entity.getBoolean("notifyWhenArrive" )
                val _notifyWhenDepart = follow.notifyWhenDepart ?: entity.getBoolean("notifyWhenDepart" )
                val _notifyWhenDelayed = follow.notifyWhenDelayed ?: entity.getBoolean("notifyWhenDelayed" )
                val _subscribeToMessages = follow.subscribeToMessages ?: entity.getBoolean("subscribeToMessages" )
                val _subscribeToWarnings = follow.subscribeToWarnings ?: entity.getBoolean("subscribeToWarnings" )
                val _transporterid = follow.transporterid ?: entity.getString("transporterid" )
                val _status = follow.status ?: entity.getString("status" )
                val fullentity = Entity.newBuilder( entity )
                        .set( "name",_name  )
                        .set( "notifyWhenArrive",_notifyWhenArrive )
                        .set( "notifyWhenDepart",_notifyWhenDepart )
                        .set( "notifyWhenDelayed",_notifyWhenDelayed )
                        .set( "subscribeToMessages",_subscribeToMessages )
                        .set( "subscribeToWarnings",_subscribeToWarnings )
                        .set( "transporterid",_transporterid )
                        .set( "status",_status )
                        .build()
                val e = transaction.put( fullentity )
                r.follow = asFollow( e )
                r.debug = "updated"
            }

            transaction.commit()

            val json = gson.toJson( r,FollowResponse::class.java )
            response.writer.write( json )
        } catch ( x:GarbageInException ) {
            response.sendError(400,x.message )
        } catch ( x:DatastoreException ) {
            response.sendError(500,x.message )
        } finally {
            if ( transaction?.isActive ?: false ) {
                transaction?.rollback()
            }
        }
    }

    override fun doDelete( request:HttpServletRequest,response:HttpServletResponse ) {
        if ( !verifySSL( request ) ) {
            response.sendError( HttpServletResponse.SC_FORBIDDEN );
            return;
        }

        response.status = 200
        response.contentType = "application/json"
        response.setHeader("access-control-allow-headers", "Content-Type,Authorization,arreev-api-key")
        response.setHeader("access-control-allow-methods", "DELETE")
        response.setHeader("access-control-allow-origin", "*")

        val r = FollowResponse()

        var transaction: Transaction? = null
        try {
            val ownerid = request.getParameter("ownerid" )
            val id = request.getParameter("id" )

            transaction = datastore.newTransaction() // could throw DataStoreException
            val keyFactory = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "follow" );

            val immutableid = id ?: ""
            val key = keyFactory.newKey( immutableid.toLong() )

            /*
             * TODO: get and check ownerid matches
             * TODO: error if id not found
             */

            datastore.delete( key )
            r.id = id

            transaction.commit()

            val json = gson.toJson( r,FollowResponse::class.java )
            response.writer.write( json )
        } catch ( x:GarbageInException ) {
            response.sendError(400,x.message )
        } catch ( x:DatastoreException ) {
            response.sendError(500,x.message )
        } finally {
            if ( transaction?.isActive ?: false ) {
                transaction?.rollback()
            }
        }
    }
}