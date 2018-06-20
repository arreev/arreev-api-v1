
package com.arreev.api

import javax.servlet.http.*

import com.google.gson.*
import com.google.cloud.datastore.*

class FleetsServlet : HttpServlet()
{
    private val datastore = DatastoreOptions.getDefaultInstance().service
    private val gson = GsonBuilder().create()

    class FleetsResponse : APIResponse()
    {
        var fleets: Array<Fleet>? = null
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

        val r = FleetsResponse()
        r.fleets = arrayOf()

        try {
            val fleets = mutableListOf<Fleet>()

            val ownerid = request.getParameter("ownerid" )
            val query = Query.newEntityQueryBuilder().setNamespace( "com.arreev.api" ).setKind( "fleet" )
                    .setFilter( StructuredQuery.PropertyFilter.eq("ownerid",ownerid ) )
                    .build()

            val entitys = datastore.run( query )
            while ( entitys.hasNext() ) {
                val entity = entitys.next()
                if ( entity != null ) {
                    val fleet = Fleet()
                    fleet.id = "${entity.key.id}"
                    fleet.ownerid = entity.getString("ownerid" )
                    fleet.name = entity.getString("name" )
                    fleet.description = entity.getString("description" )
                    fleet.type = entity.getString("type" )
                    fleet.category = entity.getString("category" )
                    fleet.imageURL = entity.getString("imageURL" )
                    fleet.thumbnailURL = entity.getString("thumbnailURL" )
                    fleet.status = entity.getString("status" )
                    fleets.add( fleet )
                }
            }

            r.fleets = fleets.toTypedArray()
        } catch ( x:Exception ) {
            r.debug = x.message
        }

        response.status = 200
        response.contentType = "application/json"

        response.setHeader("access-control-allow-headers","Content-Type,Authorization,X-Amz-Date,X-Api-Key,X-Amz-Security-Token" )
        response.setHeader("access-control-allow-methods","OPTIONS,GET,POST" )
        response.setHeader("access-control-allow-origin","*" )

        val json = gson.toJson( r, FleetsServlet.FleetsResponse::class.java )
        response.writer.write( json )
    }
}

