
package com.arreev.api

import javax.servlet.http.*

import com.google.gson.*
import com.google.cloud.datastore.*

class FollowsServlet : HttpServlet() {
    private val datastore = DatastoreOptions.getDefaultInstance().service
    private val gson = GsonBuilder().create()

    class FollowsResponse : APIResponse() {
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
            val transporterid = request.getParameter("transporterid" )

            val query = Query.newEntityQueryBuilder().setNamespace( "com.arreev.api" ).setKind( "follow" )
                    .setFilter( StructuredQuery.PropertyFilter.eq("ownerid",ownerid ) )
                    .setFilter( StructuredQuery.PropertyFilter.eq("fleetid",fleetid ) )
                    .setFilter( StructuredQuery.PropertyFilter.eq("transporterid",transporterid ) )
                    .build()

            val follows = mutableListOf<Follow>()

            val entitys = datastore.run( query )
            while ( entitys.hasNext() ) {
                val entity = entitys.next()
                val follow = Follow()
                follow.id = "${entity.key.id}"
                follow.name = entity.getString("name" )
                follow.notifyWhenArrive = entity.getBooleanOr("notifyWhenArrive",false )
                follow.notifyWhenDepart = entity.getBooleanOr("notifyWhenDepart",false )
                follow.notifyWhenDelayed = entity.getBooleanOr("notifyWhenDelayed",false )
                follow.subscribeToMessages = entity.getBooleanOr("subscribeToMessages",false )
                follow.subscribeToWarnings = entity.getBooleanOr("subscribeToWarnings",false )
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