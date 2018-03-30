
package com.arreev.api

import javax.servlet.http.*

import com.google.gson.*
import com.google.cloud.datastore.*

/*
 * https://cloud.google.com/java/getting-started/using-cloud-datastore
 * https://console.cloud.google.com/logs/viewer
 * https://cloud.google.com/appengine/docs/standard/java/tools/using-local-server
 */
class WaypointServlet : HttpServlet()
{
    private val datastore = DatastoreOptions.getDefaultInstance().service
    private val gson = GsonBuilder().create()

    class WaypointResponse : APIResponse()
    {
        var waypoint: Waypoint? = null
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

        val r = WaypointResponse()

        try {
            val id = request.getParameter("id" )

            val keyFactory = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "waypoint" )
            val entity = datastore.get( keyFactory.newKey( id.toLong() ) )
            if ( entity != null ) {
                val waypoint = Waypoint()
                waypoint.id = "${entity.key.id}";
                waypoint.name = entity.getString("name" )
                waypoint.type = entity.getString("type" )
                waypoint.category = entity.getString("category" )
                waypoint.description = entity.getString("description" )
                waypoint.imageURL = entity.getString("imageURL" )
                waypoint.thumbnailURL = entity.getString("thumbnailURL" )
                waypoint.address = entity.getString("address" )
                waypoint.latitude = entity.getDouble("latitude" )
                waypoint.longitude = entity.getDouble("longitude" )
                waypoint.index = entity.getLong("index" );
                waypoint.status = entity.getString("status" )
                r.waypoint = waypoint;
                r.debug = "found ${r?.waypoint?.name}"
            } else {
                r.debug = "not found: ${id}"
            }
        } catch ( x:DatastoreException ) {
            response.sendError(500,x.message )
        } finally {
        }

        val json = gson.toJson( r,WaypointResponse::class.java )
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
        response.setHeader("access-control-allow-methods","POST" )
        response.setHeader("access-control-allow-origin","*" )

        val r = WaypointResponse()

        var transaction: Transaction? = null
        try {
            val ownerid = request.getParameter("ownerid" )
            val routeid = request.getParameter("routeid" )

            val body = body( request )
            val waypoint = gson.fromJson<Waypoint>(body ?: "{}",Waypoint::class.java )
            if ( waypoint?.equals( "{}" ) ?: false ) { throw GarbageInException( "invalid waypoint data" ); }

            transaction = datastore.newTransaction() // could throw DataStoreException
            val keyFactory = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "waypoint" );

            if ( waypoint.id == null ) {
                /*
                 * create
                 */
                val virignkey = keyFactory.newKey()
                val fullentity = Entity.newBuilder( virignkey )
                        .set( "name",waypoint.name )
                        .set( "description",waypoint.description )
                        .set( "type",waypoint.type )
                        .set( "category",waypoint.category )
                        .set( "imageURL",waypoint.imageURL ?: "" )
                        .set( "thumbnailURL",waypoint.thumbnailURL ?: "" )
                        .set( "address",waypoint.address ?: "" )
                        .set( "latitude",waypoint.latitude ?: 0.0 )
                        .set( "longitude",waypoint.longitude ?: 0.0 )
                        .set( "index",waypoint.index ?: 0 )
                        .set( "status","active" )
                        .set( "ownerid",ownerid )
                        .set( "routeid",routeid )
                        .build()
                val e = transaction.add( fullentity )
                r.waypoint = asWaypoint( e )
                r.debug = "created"
            } else {
                /*
                 * update
                 */
                val immutableid = waypoint.id ?: ""
                val key = keyFactory.newKey( immutableid.toLong() )
                val entity = transaction.get( key )
                if ( !verifyOwnership( entity,ownerid ) ) { throw Exception( "ownerid mis-match" ) }
                val _name = waypoint.name ?: entity.getString("name" )
                val _description = waypoint.description ?: entity.getString("description" )
                val _type = waypoint.type ?: entity.getString("type" )
                val _category = waypoint.category ?: entity.getString("category" )
                val _imageURL = waypoint.imageURL ?: entity.getString("imageURL" )
                val _thumbnailURL = waypoint.thumbnailURL ?: entity.getString("thumbnailURL" )
                val _address = waypoint.address ?: entity.getString("address" )
                val _latitude = waypoint.latitude ?: entity.getDouble("latitude" )
                val _longitude = waypoint.longitude ?: entity.getDouble("longitude" )
                val _index = waypoint.index ?: entity.getLong("index" )
                val _status = waypoint.status ?: entity.getString("status" )
                val fullentity = Entity.newBuilder( entity )
                        .set( "name",_name  )
                        .set( "description",_description )
                        .set( "type",_type )
                        .set( "category",_category )
                        .set( "imageURL",_imageURL )
                        .set( "thumbnailURL",_thumbnailURL )
                        .set( "address",_address )
                        .set( "latitude",_latitude )
                        .set( "longitude",_longitude )
                        .set( "index",_index )
                        .set( "status",_status )
                        .build()
                val e = transaction.put( fullentity )
                r.waypoint = asWaypoint( e )
                r.debug = "updated"
            }

            transaction.commit()

            val json = gson.toJson( r,WaypointResponse::class.java )
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

        val r = WaypointResponse()

        var transaction: Transaction? = null
        try {
            val ownerid = request.getParameter("ownerid" )
            val id = request.getParameter("id" )

            transaction = datastore.newTransaction() // could throw DataStoreException
            val keyFactory = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "waypoint" );

            val immutableid = id ?: ""
            val key = keyFactory.newKey( immutableid.toLong() )

            /*
             * TODO: get and check ownerid matches
             */

            datastore.delete( key )

            transaction.commit()

            val json = gson.toJson( r,WaypointResponse::class.java )
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