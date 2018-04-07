
package com.arreev.api

import javax.servlet.http.*

import com.google.gson.*
import com.google.cloud.datastore.*

/*
 * https://cloud.google.com/java/getting-started/using-cloud-datastore
 * https://console.cloud.google.com/logs/viewer
 * https://cloud.google.com/appengine/docs/standard/java/tools/using-local-server
 */
class AssignmentServlet : HttpServlet()
{
    private val datastore = DatastoreOptions.getDefaultInstance().service
    private val gson = GsonBuilder().create()

    class AssignmentResponse : APIResponse()
    {
        var assignment: Assignment? = null
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

        val r = AssignmentResponse()

        try {
            val id = request.getParameter("id" )

            val keyFactory = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "assignment" )
            val entity = datastore.get( keyFactory.newKey( id.toLong() ) )
            if ( entity != null ) {
                val assignment = Assignment()
                assignment.id = "${entity.key.id}";
                assignment.type = entity.getString("type" )
                assignment.routeid = entity.getString("routeid" )
                assignment.transporterid = entity.getString("transporterid" )
                assignment.imageURL = entity.getString("imageURL" )
                assignment.thumbnailURL = entity.getString("thumbnailURL" )
                assignment.status = entity.getString("status" )
                r.assignment = assignment;
                r.debug = "found ${r?.assignment?.id}"
            } else {
                r.debug = "not found: ${id}"
            }
        } catch ( x:DatastoreException ) {
            response.sendError(500,x.message )
        } finally {
        }

        val json = gson.toJson( r,AssignmentResponse::class.java )
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

        val r = AssignmentResponse()

        var transaction: Transaction? = null
        try {
            val ownerid = request.getParameter("ownerid" )

            val body = body( request )
            val assignment = gson.fromJson<Assignment>(body ?: "{}",Assignment::class.java )
            if ( assignment?.equals( "{}" ) ?: false ) { throw GarbageInException( "invalid assignment data" ); }

            transaction = datastore.newTransaction() // could throw DataStoreException
            val keyFactory = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "assignment" );

            if ( assignment.id == null ) {
                /*
                 * create
                 */
                val virignkey = keyFactory.newKey()
                val fullentity = Entity.newBuilder( virignkey )
                        .set( "type",assignment.type )
                        .set( "routeid",assignment.routeid )
                        .set( "transporterid",assignment.transporterid )
                        .set( "imageURL",assignment.imageURL ?: "" )
                        .set( "thumbnailURL",assignment.thumbnailURL ?: "" )
                        .set( "status","active" )
                        .set( "ownerid",ownerid )
                        .build()
                val e = transaction.add( fullentity )
                r.assignment = asAssignment( e )
                r.debug = "created"
            } else {
                /*
                 * update
                 */
                val immutableid = assignment.id ?: ""
                val key = keyFactory.newKey( immutableid.toLong() )
                val entity = transaction.get( key )
                if ( !verifyOwnership( entity,ownerid ) ) { throw Exception( "ownerid mis-match" ) }
                val _type = assignment.type ?: entity.getString("type" )
                val _routeid = assignment.routeid ?: entity.getString("routeid" )
                val _transporterid = assignment.transporterid ?: entity.getString("transporterid" )
                val _imageURL = assignment.imageURL ?: entity.getString("imageURL" )
                val _thumbnailURL = assignment.thumbnailURL ?: entity.getString("thumbnailURL" )
                val _status = assignment.status ?: entity.getString("status" )
                val fullentity = Entity.newBuilder( entity )
                        .set( "type",_type )
                        .set( "routeid",_routeid )
                        .set( "transporterid",_transporterid )
                        .set( "imageURL",_imageURL )
                        .set( "thumbnailURL",_thumbnailURL )
                        .set( "status",_status )
                        .build()
                val e = transaction.put( fullentity )
                r.assignment = asAssignment( e )
                r.debug = "updated"
            }

            transaction.commit()

            val json = gson.toJson( r,AssignmentResponse::class.java )
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

        val r = AssignmentResponse()

        var transaction: Transaction? = null
        try {
            val ownerid = request.getParameter("ownerid" )
            val id = request.getParameter("id" )

            transaction = datastore.newTransaction() // could throw DataStoreException
            val keyFactory = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "assignment" );

            val immutableid = id ?: ""
            val key = keyFactory.newKey( immutableid.toLong() )

            /*
             * TODO: get and check ownerid matches
             */

            datastore.delete( key )

            transaction.commit()

            val json = gson.toJson( r,AssignmentResponse::class.java )
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