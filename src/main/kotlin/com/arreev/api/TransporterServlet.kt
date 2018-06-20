
package com.arreev.api

import javax.servlet.http.*

import com.google.gson.*
import com.google.cloud.datastore.*

/**
 *
 */
class TransporterServlet : HttpServlet()
{
    private val datastore = DatastoreOptions.getDefaultInstance().service
    private val gson = GsonBuilder().create()

    class TransporterResponse : APIResponse() {
        var transporter: Transporter? = null
        var debug: String? = null
    }

    override fun doOptions(request: HttpServletRequest, response: HttpServletResponse) {
        if (!verifySSL(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        response.status = 200
        response.setHeader("access-control-allow-headers", "Content-Type,Authorization,arreev-api-key")
        response.setHeader("access-control-allow-methods", "OPTIONS,GET,POST,DELETE")
        response.setHeader("access-control-allow-origin", "*")
    }

    override fun doGet( request:HttpServletRequest,response:HttpServletResponse ) {
        if (!verifySSL(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        response.status = 200
        response.contentType = "application/json"
        response.setHeader("access-control-allow-headers", "Content-Type,Authorization,arreev-api-key")
        response.setHeader("access-control-allow-methods", "GET")
        response.setHeader("access-control-allow-origin", "*")

        val r = TransporterResponse()

        try {
            val id = request.getParameter("id")

            val keyFactory = datastore.newKeyFactory().setNamespace("com.arreev.api").setKind("transporter")
            val entity = datastore.get(keyFactory.newKey(id.toLong()))
            if (entity != null) {
                val transporter = Transporter()
                transporter.id = "${entity.key.id}";
                transporter.ownerid = entity.getString("ownerid" )
                transporter.fleetid = entity.getString("fleetid" )
                transporter.name = entity.getString("name")
                transporter.number = entity.getLong("number")
                transporter.marquee = entity.getString("marquee")
                transporter.diatribe = entity.getString("diatribe")
                transporter.latitude = entity.getDouble("latitude")
                transporter.longitude = entity.getDouble("longitude")
                transporter.inservice = entity.getBooleanOr("inservice",false )
                transporter.type = entity.getString("type")
                transporter.category = entity.getString("category")
                transporter.description = entity.getString("description")
                transporter.imageURL = entity.getString("imageURL")
                transporter.thumbnailURL = entity.getString("thumbnailURL")
                transporter.status = entity.getString("status")
                r.transporter = transporter;
                r.debug = "found ${r?.transporter?.name}"
            } else {
                r.debug = "not found: ${id}"
            }
        } catch (x: DatastoreException) {
            response.sendError(500, x.message)
        } finally {
        }

        // TODO: dont do this here ... use ok like in FollowServlet
        val json = gson.toJson(r, TransporterServlet.TransporterResponse::class.java)
        response.writer.write(json)
    }

    override fun doPost(request: HttpServletRequest, response: HttpServletResponse) {
        if (!verifySSL(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        response.status = 200
        response.contentType = "application/json"
        response.setHeader("access-control-allow-headers", "Content-Type,Authorization,arreev-api-key")
        response.setHeader("access-control-allow-methods", "POST")
        response.setHeader("access-control-allow-origin", "*")

        val r = TransporterResponse()

        var transaction: Transaction? = null
        try {
            val ownerid = request.getParameter("ownerid" )
            val fleetid = request.getParameter("fleetid" )

            val body = body( request )
            val transporter = gson.fromJson<Transporter>(body ?: "{}",Transporter::class.java )
            if ( transporter?.equals( "{}" ) ?: false ) { throw GarbageInException( "invalid transporter data" ); }

            transaction = datastore.newTransaction() // could throw DataStoreException
            val keyFactory = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "transporter" );

            if ( transporter.id == null ) {
                /*
                 * create
                 */
                val virignkey = keyFactory.newKey()
                val fullentity = Entity.newBuilder( virignkey )
                        .set( "name",transporter.name )
                        .set( "number",transporter.number ?: 0L )
                        .set( "marquee",transporter.marquee ?: "" )
                        .set( "diatribe",transporter.diatribe ?: "" )
                        .set( "latitude",transporter.latitude ?: 0.0 )
                        .set( "longitude",transporter.longitude ?: 0.0 )
                        .set( "inservice",transporter.inservice ?: false )
                        .set( "type",transporter.type ?: "" )
                        .set( "category",transporter.category ?: "" )
                        .set( "description",transporter.description ?: "" )
                        .set( "imageURL",transporter.imageURL ?: "" )
                        .set( "thumbnailURL",transporter.thumbnailURL ?: "" )
                        .set( "status","active" )
                        .set( "ownerid",ownerid )
                        .set( "fleetid",fleetid )
                        .build()
                val e = transaction.add( fullentity )
                r.transporter = asTransporter( e )
                r.debug = "created"
            } else {
                /*
                 * update
                 */
                val immutableid = transporter.id ?: ""
                val key = keyFactory.newKey( immutableid.toLong() )
                val entity = transaction.get( key )
                if ( !verifyOwnership( entity,ownerid ) ) { throw Exception( "ownerid mis-match" ) }
                val _name = transporter.name ?: entity.getString("name" )
                val _number = transporter.number ?: entity.getLong("number" )
                val _marquee = transporter.marquee ?: entity.getString("marquee" )
                val _diatribe = transporter.diatribe ?: entity.getString("diatribe" )
                val _latitude = transporter.latitude ?: entity.getDouble("latitude" )
                val _longitude = transporter.longitude ?: entity.getDouble("longitude" )
                val _inservice = transporter.inservice ?: entity.getBooleanOr("inservice",false )
                val _description = transporter.description ?: entity.getString("description" )
                val _type = transporter.type ?: entity.getString("type" )
                val _category = transporter.category ?: entity.getString("category" )
                val _imageURL = transporter.imageURL ?: entity.getString("imageURL" )
                val _thumbnailURL = transporter.thumbnailURL ?: entity.getString("thumbnailURL" )
                val _status = transporter.status ?: entity.getString("status" )
                val fullentity = Entity.newBuilder( entity )
                        .set( "name",_name )
                        .set( "number",_number )
                        .set( "marquee",_marquee )
                        .set( "diatribe",_diatribe )
                        .set( "latitude",_latitude )
                        .set( "longitude",_longitude )
                        .set( "inservice",_inservice ?: false )
                        .set( "description",_description )
                        .set( "type",_type )
                        .set( "category",_category )
                        .set( "imageURL",_imageURL )
                        .set( "thumbnailURL",_thumbnailURL )
                        .set( "status",_status )
                        .build()
                val e = transaction.put( fullentity )
                r.transporter = asTransporter( e )
                r.debug = "updated"
            }

            transaction?.commit()

            // TODO: dont do this here ... use ok like in FollowServlet
            val json = gson.toJson( r,TransporterResponse::class.java )
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

        val r = TransporterResponse()

        var transaction: Transaction? = null
        try {
            val ownerid = request.getParameter( "ownerid" )
            val id = request.getParameter( "id" )

            transaction = datastore.newTransaction() // could throw DataStoreException
            val keyFactory = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "transporter" );

            val immutableid = id ?: ""
            val key = keyFactory.newKey( immutableid.toLong() )

            /*
             * TODO: get and check ownerid matches
             */

            datastore.delete( key )

            transaction.commit()

            val json = gson.toJson( r,TransporterResponse::class.java )
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