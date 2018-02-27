
package com.arreev.api

import javax.servlet.http.*

import com.google.gson.*
import com.google.cloud.datastore.*

/*
 * https://cloud.google.com/java/getting-started/using-cloud-datastore
 * https://console.cloud.google.com/logs/viewer
 * https://cloud.google.com/appengine/docs/standard/java/tools/using-local-server
 */
class FleetServlet : HttpServlet()
{
    private val datastore = DatastoreOptions.getDefaultInstance().service
    private val gson = GsonBuilder().create()

    class FleetResponse : APIResponse()
    {
        var fleet: Fleet? = null
        var debug: String? = null
    }

    override fun doOptions( request:HttpServletRequest,response:HttpServletResponse ) {
        if ( !verifySSL( request ) ) {
            response.sendError( HttpServletResponse.SC_FORBIDDEN );
            return;
        }

        response.status = 200
        response.setHeader("access-control-allow-headers","Content-Type,Authorization,arreev-api-key" )
        response.setHeader("access-control-allow-methods","OPTIONS,GET,POST" )
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
        response.setHeader("access-control-allow-methods","OPTIONS,GET,POST" )
        response.setHeader("access-control-allow-origin","*" )

        val r = FleetResponse()

        try {
            val id = request.getParameter("id" )

            val keyFactory = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "fleet" )
            val entity = datastore.get( keyFactory.newKey( id.toLong() ) )
            if ( entity != null ) {
                val fleet = Fleet()
                fleet.id = "${entity.key.id}";
                fleet.name = entity.getString("name" )
                fleet.type = entity.getString("type" )
                fleet.category = entity.getString("category" )
                fleet.description = entity.getString("description" )
                fleet.imageURL = entity.getString("imageURL" )
                fleet.thumbnailURL = entity.getString("thumbnailURL" )
                fleet.status = entity.getString("status" )
                r.fleet = fleet;
                r.debug = "found ${r?.fleet?.name}"
            } else {
                r.debug = "not found: ${id}"
            }
        } catch ( x:DatastoreException ) {
            response.sendError(500,x.message )
        } finally {
        }

        val json = gson.toJson( r,FleetResponse::class.java )
        response.writer.write( json )
    }

    override fun doPost( request:HttpServletRequest,response:HttpServletResponse ) {
        if ( !verifySSL( request ) ) {
            response.sendError( HttpServletResponse.SC_FORBIDDEN );
            return;
        }

        response.status = 200
        response.contentType = "application/json"
        response.setHeader("access-control-allow-headers","Content-Type,Authorization,arreev-api-key" )
        response.setHeader("access-control-allow-methods","OPTIONS,GET,POST" )
        response.setHeader("access-control-allow-origin","*" )

        val r = FleetResponse()

        var transaction: Transaction? = null
        try {
            val ownerid = request.getParameter("ownerid" )

            val body = body( request )
            val fleet = gson.fromJson<Fleet>(body ?: "{}",Fleet::class.java )
            if ( fleet?.equals( "{}" ) ?: false ) { throw GarbageInException( "invalid fleet data" ); }

            transaction = datastore.newTransaction() // could throw DataStoreException
            val keyFactory = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "fleet" );

            if ( fleet.id == null ) {
                /*
                 * create
                 */
                val virignkey = keyFactory.newKey()
                val fullentity = Entity.newBuilder( virignkey )
                        .set( "name",fleet.name )
                        .set( "description",fleet.description )
                        .set( "type",fleet.type )
                        .set( "category",fleet.category )
                        .set( "imageURL",fleet.imageURL ?: "" )
                        .set( "thumbnailURL",fleet.thumbnailURL ?: "" )
                        .set( "status","active" )
                        .set( "ownerid",ownerid )
                        .build()
                val e = transaction.add( fullentity )
                r.fleet = asFleet( e )
                r.debug = "add spork"
            } else {
                /*
                 * update
                 */
                val immutableid = fleet.id ?: ""
                val key = keyFactory.newKey( immutableid.toLong() )
                val entity = transaction.get( key )
                if ( !verifyOwnership( entity,ownerid ) ) { throw Exception( "ownerid mis-match" ) }
                val _name = fleet.name ?: entity.getString("name" )
                val _description = fleet.description ?: entity.getString("description" )
                val _type = fleet.type ?: entity.getString("type" )
                val _category = fleet.category ?: entity.getString("category" )
                val _imageURL = fleet.imageURL ?: entity.getString("imageURL" )
                val _thumbnailURL = fleet.thumbnailURL ?: entity.getString("thumbnailURL" )
                val _status = fleet.status ?: entity.getString("status" )
                val fullentity = Entity.newBuilder( entity )
                        .set( "name",_name  )
                        .set( "description",_description )
                        .set( "type",_type )
                        .set( "category",_category )
                        .set( "imageURL",_imageURL )
                        .set( "thumbnailURL",_thumbnailURL )
                        .set( "status",_status )
                        .build()
                val e = transaction.put( fullentity )
                r.fleet = asFleet( e )
                r.debug = "put gulch"
            }

            transaction.commit()

            val json = gson.toJson( r,FleetResponse::class.java )
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