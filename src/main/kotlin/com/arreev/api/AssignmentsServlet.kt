
package com.arreev.api

import javax.servlet.http.*

import com.google.gson.*
import com.google.cloud.datastore.*

class AssignmentsServlet : HttpServlet()
{
    private val datastore = DatastoreOptions.getDefaultInstance().service
    private val gson = GsonBuilder().create()

    class AssignmentsResponse : APIResponse() {
        var assignments: Array<Assignment>? = null
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

        val r = AssignmentsResponse()
        r.assignments = arrayOf()

        try {
            val assignments = mutableListOf<Assignment>()

            val ownerid = request.getParameter("ownerid" )
            val type = request.getParameter("type" )
            val routeid = request.getParameter("routeid" )
            val query = Query.newEntityQueryBuilder().setNamespace( "com.arreev.api" ).setKind( "assignment" )
                    .setFilter( StructuredQuery.PropertyFilter.eq("ownerid",ownerid ) )
                    .setFilter( StructuredQuery.PropertyFilter.eq("type",type ) )
                    .setFilter( StructuredQuery.PropertyFilter.eq("routeid",routeid ) )
                    .build()

            val entitys = datastore.run( query )
            while ( entitys.hasNext() ) {
                val entity = entitys.next()
                if ( entity != null ) {
                    val assignment = Assignment()
                    assignment.id = "${entity.key.id}"
                    assignment.type = entity.getString("type" )
                    assignment.routeid = entity.getString("routeid" )
                    assignment.transporterid = entity.getString("transporterid" )
                    assignment.imageURL = entity.getString("imageURL" )
                    assignment.thumbnailURL = entity.getString("thumbnailURL" )
                    assignment.status = entity.getString("status" )
                    assignments.add( assignment )
                }
            }

            r.assignments = assignments.toTypedArray()
        } catch ( x:Exception ) {
            r.debug = x.message
        }

        response.status = 200
        response.contentType = "application/json"

        response.setHeader("access-control-allow-headers","Content-Type,Authorization,X-Amz-Date,X-Api-Key,X-Amz-Security-Token" )
        response.setHeader("access-control-allow-methods","GET" )
        response.setHeader("access-control-allow-origin","*" )

        val json = gson.toJson( r, AssignmentsServlet.AssignmentsResponse::class.java )
        response.writer.write( json )
    }
}