
package com.arreev.api

import javax.servlet.http.*

import com.google.gson.*
import com.google.cloud.datastore.*

class WaypointsServlet : HttpServlet()
{
    private val datastore = DatastoreOptions.getDefaultInstance().service
    private val gson = GsonBuilder().create()

    class WaypointsResponse : APIResponse() {
        var waypoints: Array<Waypoint>? = null
        var debug: String? = null
    }

    override fun doOptions(request:HttpServletRequest,response:HttpServletResponse) {
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

        val r = WaypointsResponse()
        r.waypoints = arrayOf()

        try {
            val waypoints = mutableListOf<Waypoint>()

            val ownerid = request.getParameter("ownerid" )
            val routeid = request.getParameter("routeid" )
            val query = Query.newEntityQueryBuilder().setNamespace( "com.arreev.api" ).setKind( "waypoint" )
                    .setFilter( StructuredQuery.PropertyFilter.eq("ownerid",ownerid ) )
                    .setFilter( StructuredQuery.PropertyFilter.eq("routeid",routeid ) )
                    .build()

            val entitys = datastore.run( query )
            while ( entitys.hasNext() ) {
                val entity = entitys.next()
                if ( entity != null ) {
                    val waypoint = Waypoint()
                    waypoint.id = "${entity.key.id}"
                    waypoint.name = entity.getString("name" )
                    waypoint.description = entity.getString("description" )
                    waypoint.type = entity.getString("type" )
                    waypoint.category = entity.getString("category" )
                    waypoint.imageURL = entity.getString("imageURL" )
                    waypoint.thumbnailURL = entity.getString("thumbnailURL" )
                    waypoint.address = entity.getString( "address" )
                    waypoint.latitude = entity.getDouble( "latitude" );
                    waypoint.longitude = entity.getDouble( "longitude" );
                    waypoint.index = entity.getLong("index" );
                    waypoint.status = entity.getString("status" )
                    waypoints.add( waypoint )
                }
            }

            r.waypoints = waypoints.toTypedArray()
        } catch ( x:Exception ) {
            r.debug = x.message
        }

        response.status = 200
        response.contentType = "application/json"

        response.setHeader("access-control-allow-headers","Content-Type,Authorization,X-Amz-Date,X-Api-Key,X-Amz-Security-Token" )
        response.setHeader("access-control-allow-methods","GET" )
        response.setHeader("access-control-allow-origin","*" )

        val json = gson.toJson( r, WaypointsServlet.WaypointsResponse::class.java )
        response.writer.write( json )
    }
}
