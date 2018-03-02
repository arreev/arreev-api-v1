
package com.arreev.api

import javax.servlet.http.*

import com.google.gson.*
import com.google.cloud.datastore.*

class TransportersServlet : HttpServlet()
{
    private val datastore = DatastoreOptions.getDefaultInstance().service
    private val gson = GsonBuilder().create()

    class TransportersResponse : APIResponse()
    {
        var transporters: Array<Transporter>? = null
        var debug: String? = null
    }

    override fun doOptions( request:HttpServletRequest,response:HttpServletResponse ) {
        if ( !verifySSL( request ) ) {
            response.sendError( HttpServletResponse.SC_FORBIDDEN );
            return;
        }

        response.setHeader("access-control-allow-headers","Content-Type,Authorization,arreev-api-key" )
        response.setHeader("access-control-allow-methods","OPTIONS,GET" )
        response.setHeader("access-control-allow-origin","*" )

        response.status = 200
    }

    override fun doGet( request:HttpServletRequest,response:HttpServletResponse ) {
        if ( !verifySSL( request ) ) {
            response.sendError( HttpServletResponse.SC_FORBIDDEN );
            return;
        }

        response.status = 200
        response.contentType = "application/json"
        response.setHeader("access-control-allow-headers","Content-Type,Authorization,X-Amz-Date,X-Api-Key,X-Amz-Security-Token" )
        response.setHeader("access-control-allow-methods","OPTIONS,GET" )
        response.setHeader("access-control-allow-origin","*" )

        val r = TransportersResponse()
        r.transporters = arrayOf()

        try {
            val transporters = mutableListOf<Transporter>()

            val ownerid = request.getParameter("ownerid" )
            val fleetid = request.getParameter("fleetid" )
            val query = Query.newEntityQueryBuilder().setNamespace( "com.arreev.api" ).setKind( "transporter" )
                    .setFilter( StructuredQuery.PropertyFilter.eq("ownerid",ownerid ) )
                    .setFilter( StructuredQuery.PropertyFilter.eq("fleetid",fleetid ) )
                    .build()

            val entitys = datastore.run( query )
            while ( entitys.hasNext() ) {
                val entity = entitys.next()
                if ( entity != null ) {
                    val transporter = Transporter()
                    transporter.id = "${entity.key.id}"
                    transporter.name = entity.getString("name" )
                    transporter.number = entity.getLong("number" )
                    transporter.marquee = entity.getString("marquee" )
                    transporter.diatribe = entity.getString("diatribe" )
                    transporter.latitude = entity.getDouble("latitude" )
                    transporter.longitude = entity.getDouble("longitude" )
                    transporter.description = entity.getString("description" )
                    transporter.type = entity.getString("type" )
                    transporter.category = entity.getString("category" )
                    transporter.imageURL = entity.getString("imageURL" )
                    transporter.thumbnailURL = entity.getString("thumbnailURL" )
                    transporter.status = entity.getString("status" )
                    transporters.add( transporter )
                }
            }

            r.transporters = transporters.toTypedArray()
        } catch ( x:Exception ) {
            r.debug = x.message
        }

        val json = gson.toJson( r, TransportersServlet.TransportersResponse::class.java )
        response.writer.write( json )
    }
}