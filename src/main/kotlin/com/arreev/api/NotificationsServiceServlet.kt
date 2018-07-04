
package com.arreev.api

import javax.servlet.http.*

import com.google.gson.*

/**
 *
 */
class NotificationsServiceServlet : HttpServlet()
{
    private val gson = GsonBuilder().create()

    class NSSResponse : APIResponse()

    override fun doGet( request:HttpServletRequest,response:HttpServletResponse ) {
        val r = NSSResponse()

        r.status = 0
        r.message = "NotificationsService:OK"

        response.status = 200
        response.contentType = "application/json"

        val json = gson.toJson( r, NotificationsServiceServlet.NSSResponse::class.java )
        response.writer.write( json )
    }
}