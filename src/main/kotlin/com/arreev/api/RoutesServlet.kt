
package com.arreev.api

import javax.servlet.http.*

import com.google.gson.*
import com.google.cloud.datastore.*

class RoutesServlet : HttpServlet()
{
    private val datastore = DatastoreOptions.getDefaultInstance().service
    private val gson = GsonBuilder().create()

    class RoutesResponse : APIResponse() {
        var routes: Array<Route>? = null
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

        val r = RoutesResponse()
        r.routes = arrayOf()

        try {
            val routes = mutableListOf<Route>()

            val ownerid = request.getParameter("ownerid" )
            val query = Query.newEntityQueryBuilder().setNamespace( "com.arreev.api" ).setKind( "route" )
                    .setFilter( StructuredQuery.PropertyFilter.eq("ownerid",ownerid ) )
                    .build()

            val entitys = datastore.run( query )
            while ( entitys.hasNext() ) {
                val entity = entitys.next()
                if ( entity != null ) {
                    val route = Route()
                    route.id = "${entity.key.id}"
                    route.name = entity.getString("name" )
                    route.description = entity.getString("description" )
                    route.type = entity.getString("type" )
                    route.category = entity.getString("category" )
                    route.imageURL = entity.getString("imageURL" )
                    route.thumbnailURL = entity.getString("thumbnailURL" )
                    route.status = entity.getString("status" )
                    routes.add( route )
                }
            }

            r.routes = routes.toTypedArray()
        } catch ( x:Exception ) {
            r.debug = x.message
        }

        response.status = 200
        response.contentType = "application/json"

        response.setHeader("access-control-allow-headers","Content-Type,Authorization,X-Amz-Date,X-Api-Key,X-Amz-Security-Token" )
        response.setHeader("access-control-allow-methods","GET" )
        response.setHeader("access-control-allow-origin","*" )

        val json = gson.toJson( r, RoutesServlet.RoutesResponse::class.java )
        response.writer.write( json )
    }
}