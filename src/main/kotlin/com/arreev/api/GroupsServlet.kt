
package com.arreev.api

import javax.servlet.http.*

import com.google.gson.*
import com.google.cloud.datastore.*

class GroupsServlet : HttpServlet()
{
    private val datastore = DatastoreOptions.getDefaultInstance().service
    private val gson = GsonBuilder().create()

    class GroupsResponse : APIResponse()
    {
        var groups: Array<Group>? = null
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

        val r = GroupsResponse()
        r.groups = arrayOf()

        var ok = false

        try {
            val ownerid = request.getParameter("ownerid" )
            val type = request.getParameter("type" )

            val query = Query.newEntityQueryBuilder().setNamespace( "com.arreev.api" ).setKind( "group" )
                    .setFilter( StructuredQuery.PropertyFilter.eq("ownerid",ownerid ) )
                    .setFilter( StructuredQuery.PropertyFilter.eq("type",type ) )
                    .setLimit( 500 ) // TODO: paging support
                    .build()

            val groups = mutableListOf<Group>()

            val entitys = datastore.run( query )
            while ( entitys.hasNext() ) {
                val entity = entitys.next()
                val group = Group()
                group.id = "${entity.key.id}"
                group.name = entity.getString("name" )
                group.type = entity.getString("type" )
                group.category = entity.getString("category" )
                group.description = entity.getString("description" )
                group.imageURL = entity.getString("imageURL" )
                group.thumbnailURL = entity.getString("thumbnailURL" )
                group.status = entity.getString("status" )
                groups.add( group )
            }

            r.groups = groups.toTypedArray()
            ok = true
        } catch ( x:DatastoreException ) {
            response.sendError(500,x.message )
        } finally {}

        if ( ok ) {
            val json = gson.toJson( r, GroupsServlet.GroupsResponse::class.java )
            response.writer.write( json )
        }
    }
}