
package com.arreev.api

import javax.servlet.http.*

import com.google.gson.*
import com.google.cloud.datastore.*

/*
 * https://cloud.google.com/java/getting-started/using-cloud-datastore
 * https://console.cloud.google.com/logs/viewer
 * https://cloud.google.com/appengine/docs/standard/java/tools/using-local-server
 */
class GroupServlet : HttpServlet()
{
    private val datastore = DatastoreOptions.getDefaultInstance().service
    private val gson = GsonBuilder().create()

    class GroupResponse : APIResponse()
    {
        var group: Group? = null
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

        val r = GroupResponse()

        try {
            val id = request.getParameter("id" )

            val keyFactory = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "group" )
            val entity = datastore.get( keyFactory.newKey( id.toLong() ) )
            if ( entity != null ) {
                val group = Group()
                group.id = "${entity.key.id}";
                group.name = entity.getString("name" )
                group.type = entity.getString("type" )
                group.category = entity.getString("category" )
                group.description = entity.getString("description" )
                group.imageURL = entity.getString("imageURL" )
                group.thumbnailURL = entity.getString("thumbnailURL" )
                group.status = entity.getString("status" )
                r.group = group;
                r.debug = "found ${r?.group?.name}"
            } else {
                r.debug = "not found: ${id}"
            }
        } catch ( x:DatastoreException ) {
            response.sendError(500,x.message )
        } finally {
        }

        val json = gson.toJson( r,GroupResponse::class.java )
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

        val r = GroupResponse()

        var transaction: Transaction? = null
        try {
            val ownerid = request.getParameter("ownerid" )

            val body = body( request )
            val group = gson.fromJson<Group>(body ?: "{}",Group::class.java )
            if ( group?.equals( "{}" ) ?: false ) { throw GarbageInException( "invalid group data" ); }

            transaction = datastore.newTransaction() // could throw DataStoreException
            val keyFactory = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "group" );

            if ( group.id == null ) {
                /*
                 * create
                 */
                val virignkey = keyFactory.newKey()
                val fullentity = Entity.newBuilder( virignkey )
                        .set( "name",group.name )
                        .set( "type",group.type )
                        .set( "category",group.category )
                        .set( "description",group.description )
                        .set( "imageURL",group.imageURL ?: "" )
                        .set( "thumbnailURL",group.thumbnailURL ?: "" )
                        .set( "status","active" )
                        .set( "ownerid",ownerid )
                        .build()
                val e = transaction.add( fullentity )
                r.group = asGroup( e )
                r.debug = "created"
            } else {
                /*
                 * update
                 */
                val immutableid = group.id ?: ""
                val key = keyFactory.newKey( immutableid.toLong() )
                val entity = transaction.get( key )
                if ( !verifyOwnership( entity,ownerid ) ) { throw Exception( "ownerid mis-match" ) }
                val _name = group.name ?: entity.getString("name" )
                val _type = group.type ?: entity.getString("type" )
                val _category = group.category ?: entity.getString("category" )
                val _description = group.description ?: entity.getString("description" )
                val _imageURL = group.imageURL ?: entity.getString("imageURL" )
                val _thumbnailURL = group.thumbnailURL ?: entity.getString("thumbnailURL" )
                val _status = group.status ?: entity.getString("status" )
                val fullentity = Entity.newBuilder( entity )
                        .set( "name",_name  )
                        .set( "type",_type )
                        .set( "category",_category )
                        .set( "description",_description )
                        .set( "imageURL",_imageURL )
                        .set( "thumbnailURL",_thumbnailURL )
                        .set( "status",_status )
                        .build()
                val e = transaction.put( fullentity )
                r.group = asGroup( e )
                r.debug = "updated"
            }

            transaction.commit()

            val json = gson.toJson( r,GroupResponse::class.java )
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

        val r = GroupResponse()

        var transaction: Transaction? = null
        try {
            val ownerid = request.getParameter("ownerid" )
            val id = request.getParameter("id" )

            transaction = datastore.newTransaction() // could throw DataStoreException
            val keyFactory = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "group" );

            val immutableid = id ?: ""
            val key = keyFactory.newKey( immutableid.toLong() )

            /*
             * TODO: get and check ownerid matches
             */

            datastore.delete( key )
            r.id = id

            transaction.commit()

            val json = gson.toJson( r,GroupResponse::class.java )
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