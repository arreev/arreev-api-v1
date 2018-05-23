
package com.arreev.api

import javax.servlet.http.*

import com.google.gson.*
import com.google.cloud.datastore.*

class FollowsServlet : HttpServlet()
{
    private val datastore = DatastoreOptions.getDefaultInstance().service
    private val gson = GsonBuilder().create()

    class FollowsResponse : APIResponse()
    {
        var follows: Array<Follow>? = null
        var debug: String? = null
    }

    override fun doOptions(request: HttpServletRequest, response: HttpServletResponse) {
        if (!verifySSL(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        response.setHeader("access-control-allow-headers", "Content-Type,Authorization,arreev-api-key")
        response.setHeader("access-control-allow-methods", "OPTIONS,GET")
        response.setHeader("access-control-allow-origin", "*")

        response.status = 200
    }

    override fun doGet(request: HttpServletRequest, response: HttpServletResponse) {
        if (!verifySSL(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        response.status = 200
        response.contentType = "application/json"
        response.setHeader("access-control-allow-headers", "Content-Type,Authorization,X-Amz-Date,X-Api-Key,X-Amz-Security-Token")
        response.setHeader("access-control-allow-methods", "OPTIONS,GET")
        response.setHeader("access-control-allow-origin", "*")

        val r = FollowsResponse()
        r.follows = arrayOf()

        var ok = false

        try {
            val ownerid = request.getParameter("ownerid" )
            val fleetid = request.getParameter("fleetid" )

            val filters = StructuredQuery.CompositeFilter.and(
                    StructuredQuery.PropertyFilter.eq("ownerid",ownerid ),
                    StructuredQuery.PropertyFilter.eq("fleetid",fleetid )
            )

            val query = Query.newEntityQueryBuilder().setNamespace( "com.arreev.api" ).setKind( "follow" )
                    .setFilter( filters )
                    .setLimit( 500 ) // TODO: paging support
                    .build()

            val follows = mutableListOf<Follow>()

            val entitys = datastore.run( query )
            while ( entitys.hasNext() ) {
                val entity = entitys.next()
                val follow = Follow()
                follow.id = "${entity.key.id}"
                follow.name = entity.getString("name" )
                follow.notifyWhenArrive = entity.getBoolean("notifyWhenArrive" )
                follow.notifyWhenDepart = entity.getBoolean("notifyWhenDepart" )
                follow.notifyWhenDelayed = entity.getBoolean("notifyWhenDelayed" )
                follow.subscribeToMessages = entity.getBoolean("subscribeToMessages" )
                follow.subscribeToWarnings = entity.getBoolean("subscribeToWarnings" )
                follow.transporterid = entity.getString( "transporterid" )
                follow.status = entity.getString("status" )
                follows.add( follow )
            }

            r.follows = follows.toTypedArray()
            ok = true
        } catch ( x:DatastoreException ) {
            response.sendError(500,x.message )
        } finally {}

        if ( ok ) {
            val json = gson.toJson( r, FollowsServlet.FollowsResponse::class.java )
            response.writer.write( json )
        }
    }
}