
package com.arreev.api

import com.google.gson.*
import javax.servlet.http.*

import com.google.appengine.api.utils.*

class PINGServlet : HttpServlet()
{
    private val gson = GsonBuilder().create()

    class PINGResponse : APIResponse()
    {
        val version = VERSION;
        var env: String? = null
        var greeting: String? = null
    }

    override fun doGet( request:HttpServletRequest,response:HttpServletResponse ) {
        val r = PINGResponse()

        try {
            when ( SystemProperty.environment.value() ) {
                SystemProperty.Environment.Value.Development -> { r.env = "DEV" }
                SystemProperty.Environment.Value.Production -> { r.env = "PROD" }
                else -> {}
            }
        } catch ( x:Throwable ) {
            x.printStackTrace()
        }

        val name = request.getParameter("name" )
        r.greeting = if ( name != null ) "hello ${name}" else "hello"

        response.status = 200
        response.contentType = "application/json"

        val json = gson.toJson( r,PINGResponse::class.java )
        response.writer.write( json )
    }
}