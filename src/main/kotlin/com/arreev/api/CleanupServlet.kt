
package com.arreev.api

import com.google.gson.*
import javax.servlet.http.*
import com.google.appengine.api.taskqueue.*

/**
 *
 */
class CleanupServlet : HttpServlet()
{
    class CleanupResponse : APIResponse()

    private val gson = GsonBuilder().create()

    override fun doGet( request:HttpServletRequest,response:HttpServletResponse ) {
        if ( !verifySSL( request ) ) {
            response.sendError( HttpServletResponse.SC_FORBIDDEN );
            return;
        }

        response.status = 200
        response.contentType = "application/json"
        response.setHeader("access-control-allow-headers","Content-Type,Authorization,arreev-api-key" )
        response.setHeader("access-control-allow-methods","GET" )
        response.setHeader("access-control-allow-origin","*" )

        val r = CleanupResponse()
        r.status = 200
        r.message = "cleanup started"

        val ownerid = request.getParameter("ownerid" )

        val queue = QueueFactory.getDefaultQueue()
        /*
         * cleanup persons
         */
        queue.add( TaskOptions.Builder
                .withUrl("/purge" )
                .param("ownerid",ownerid )
                .param("subject","persons" )
        );
        /*
         * other cleanups...
         */

        val json = gson.toJson( r, CleanupServlet.CleanupResponse::class.java )
        response.writer.write( json )
    }
}

fun main( args:Array<String> ) {
    var http: java.net.HttpURLConnection? = null
    var input: java.io.InputStream? = null
    try {
        val url = "http://localhost:8080/cleanup?ownerid=sRRE4zg1dVgTyRxISONY9PNqvYy1"
        http = java.net.URL( url ).openConnection() as java.net.HttpURLConnection
        http.doInput = true

        input = http.inputStream
        val json = readAsJson( input )
        println( json )
    } catch ( x:Exception ) {
        x.printStackTrace()
    } finally {
        input?.close()
        http?.disconnect()
    }
}