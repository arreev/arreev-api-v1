
package com.arreev.api

import javax.servlet.http.*

import com.google.gson.*
import com.google.cloud.datastore.*

/*
 * https://cloud.google.com/java/getting-started/using-cloud-datastore
 * https://console.cloud.google.com/logs/viewer
 * https://cloud.google.com/appengine/docs/standard/java/tools/using-local-server
 */
class PersonServlet : HttpServlet()
{
    private val datastore = DatastoreOptions.getDefaultInstance().service
    private val gson = GsonBuilder().create()

    class PersonResponse : APIResponse()
    {
        var person: Person? = null
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

        val r = PersonResponse()

        try {
            val id = request.getParameter("id" )

            val keyFactory = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "person" )
            val entity = datastore.get( keyFactory.newKey( id.toLong() ) )
            if ( entity != null ) {
                val person = Person()
                person.id = "${entity.key.id}";
                person.name = entity.getString("name" )
                person.type = entity.getString("type" )
                person.category = entity.getString("category" )
                person.description = entity.getString("description" )
                person.imageURL = entity.getString("imageURL" )
                person.thumbnailURL = entity.getString("thumbnailURL" )
                person.status = entity.getString("status" )
                r.person = person;
                r.debug = "found ${r?.person?.name}"
            } else {
                r.debug = "not found: ${id}"
            }
        } catch ( x:DatastoreException ) {
            response.sendError(500,x.message )
        } finally {
        }

        val json = gson.toJson( r,PersonResponse::class.java )
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

        val r = PersonResponse()

        var transaction: Transaction? = null
        try {
            val ownerid = request.getParameter("ownerid" )
            val groupid = request.getParameter("groupid" )

            val body = body( request )
            val person = gson.fromJson<Person>(body ?: "{}",Person::class.java )
            if ( person?.equals( "{}" ) ?: false ) { throw GarbageInException( "invalid person data" ); }

            transaction = datastore.newTransaction() // could throw DataStoreException
            val keyFactory = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "person" );

            if ( person.id == null ) {
                /*
                 * create
                 */
                val virignkey = keyFactory.newKey()
                val fullentity = Entity.newBuilder( virignkey )
                        .set( "name",person.name )
                        .set( "description",person.description )
                        .set( "type",person.type )
                        .set( "category",person.category )
                        .set( "imageURL",person.imageURL ?: "" )
                        .set( "thumbnailURL",person.thumbnailURL ?: "" )
                        .set( "status","active" )
                        .set( "ownerid",ownerid )
                        .set( "groupid",groupid )
                        .build()
                val e = transaction.add( fullentity )
                r.person = asPerson( e )
                r.debug = "created"
            } else {
                /*
                 * update
                 */
                val immutableid = person.id ?: ""
                val key = keyFactory.newKey( immutableid.toLong() )
                val entity = transaction.get( key )
                if ( !verifyOwnership( entity,ownerid ) ) { throw Exception( "ownerid mis-match" ) }
                val _name = person.name ?: entity.getString("name" )
                val _description = person.description ?: entity.getString("description" )
                val _type = person.type ?: entity.getString("type" )
                val _category = person.category ?: entity.getString("category" )
                val _imageURL = person.imageURL ?: entity.getString("imageURL" )
                val _thumbnailURL = person.thumbnailURL ?: entity.getString("thumbnailURL" )
                val _status = person.status ?: entity.getString("status" )
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
                r.person = asPerson( e )
                r.debug = "updated"
            }

            transaction.commit()

            val json = gson.toJson( r,PersonResponse::class.java )
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

        val r = PersonResponse()

        var transaction: Transaction? = null
        try {
            val ownerid = request.getParameter("ownerid" )
            val id = request.getParameter("id" )

            transaction = datastore.newTransaction() // could throw DataStoreException
            val keyFactory = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "person" );

            val immutableid = id ?: ""
            val key = keyFactory.newKey( immutableid.toLong() )

            /*
             * TODO: get and check ownerid matches
             * TODO: error if id not found
             */

            datastore.delete( key )
            r.id = id

            transaction.commit()

            val json = gson.toJson( r,PersonResponse::class.java )
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