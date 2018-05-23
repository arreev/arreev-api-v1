
package com.arreev.api

import javax.servlet.http.*

import com.google.gson.*
import com.google.cloud.datastore.*

class PersonsServlet : HttpServlet()
{
    private val datastore = DatastoreOptions.getDefaultInstance().service
    private val gson = GsonBuilder().create()

    class PersonsResponse : APIResponse()
    {
        var persons: Array<Person>? = null
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

        val r = PersonsResponse()
        r.persons = arrayOf()

        var ok = false

        try {
            val ownerid = request.getParameter("ownerid" )
            val groupid = request.getParameter("groupid" )

            val filters = if ( groupid != null ) {
                StructuredQuery.CompositeFilter.and(
                        StructuredQuery.PropertyFilter.eq("ownerid",ownerid ),
                        StructuredQuery.PropertyFilter.eq("groupid",groupid )
                )
            } else {
                StructuredQuery.CompositeFilter.and(
                        StructuredQuery.PropertyFilter.eq("ownerid",ownerid )
                )
            }

            val query = Query.newEntityQueryBuilder().setNamespace( "com.arreev.api" ).setKind( "person" )
                    .setFilter( filters )
                    .setLimit( 500 ) // TODO: paging support
                    .build()

            val persons = mutableListOf<Person>()

            val entitys = datastore.run( query )
            while ( entitys.hasNext() ) {
                val entity = entitys.next()
                val person = Person()
                person.id = "${entity.key.id}"
                person.name = entity.getString("name" )
                person.type = entity.getString("type" )
                person.category = entity.getString("category" )
                person.description = entity.getString("description" )
                person.imageURL = entity.getString("imageURL" )
                person.thumbnailURL = entity.getString("thumbnailURL" )
                person.status = entity.getString("status" )
                persons.add( person )
            }

            r.persons = persons.toTypedArray()
            ok = true
        } catch ( x:DatastoreException ) {
            response.sendError(500,x.message )
        } finally {}

        if ( ok ) {
            val json = gson.toJson( r, PersonsServlet.PersonsResponse::class.java )
            response.writer.write( json )
        }
    }
}