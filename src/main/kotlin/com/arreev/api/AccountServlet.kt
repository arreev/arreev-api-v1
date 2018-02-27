
package com.arreev.api

import java.io.*
import java.net.*
import javax.servlet.http.*

import com.google.gson.*
import com.google.cloud.datastore.*

/*
 * https://cloud.google.com/java/getting-started/using-cloud-datastore
 * https://console.cloud.google.com/logs/viewer
 * https://cloud.google.com/appengine/docs/standard/java/tools/using-local-server
 */
class AccountServlet : HttpServlet()
{
    private val datastore = DatastoreOptions.getDefaultInstance().service
    private val gson = GsonBuilder().create()

    class AccountResponse : APIResponse()
    {
        var account: Account? = null
        var debug: String? = null
    }

    override fun doOptions( request:HttpServletRequest,response:HttpServletResponse ) {
        if ( !verifySSL( request ) ) {
            response.sendError( HttpServletResponse.SC_FORBIDDEN );
            return;
        }

        response.setHeader("access-control-allow-headers","Content-Type,Authorization,X-Amz-Date,X-Api-Key,X-Amz-Security-Token" )
        response.setHeader("access-control-allow-methods","OPTIONS,GET,POST" )
        response.setHeader("access-control-allow-origin","*" )

        response.status = 200
    }

    override fun doGet( request:HttpServletRequest,response:HttpServletResponse ) {
        if ( !verifySSL( request ) ) {
            response.sendError( HttpServletResponse.SC_FORBIDDEN );
            return;
        }

        val r = AccountResponse()

        val sub = request.getParameter("sub" )

        val query = Query.newEntityQueryBuilder().setNamespace( "com.arreev.api" ).setKind( "account" )
                .setFilter( StructuredQuery.PropertyFilter.eq("sub",sub ) )
                .build()
        val accounts = datastore.run( query )

        if ( (accounts != null) && accounts.hasNext() ) {
            val entity = accounts.next()
            if ( entity != null ) {
                r.account = Account()
                r.account?.id = "${entity.key.id}"
                r.account?.sub = entity.getString("sub" )
                r.account?.firstname = entity.getString("firstname" )
                r.account?.lastname = entity.getString("lastname" )
                r.account?.email = entity.getString("email" )
                r.account?.role = entity.getString("role" )
                r.account?.permissions = entity.getString("permissions" )
                r.account?.groups = entity.getString("groups" )
                r.account?.active = entity.getBoolean("active" )
                r.account?.imageURL = entity.getString("imageURL" )
                r.account?.thumbnailURL = entity.getString("thumbnailURL" )
            }
        }

        response.status = 200
        response.contentType = "application/json"

        response.setHeader("access-control-allow-headers","Content-Type,Authorization,X-Amz-Date,X-Api-Key,X-Amz-Security-Token" )
        response.setHeader("access-control-allow-methods","OPTIONS,GET,POST" )
        response.setHeader("access-control-allow-origin","*" )

        val json = gson.toJson( r,AccountResponse::class.java )
        response.writer.write( json )
    }

    override fun doPost( request:HttpServletRequest,response:HttpServletResponse ) {
        if ( !verifySSL( request ) ) {
            response.sendError( HttpServletResponse.SC_FORBIDDEN );
            return;
        }

        val r = AccountResponse()

        val body = body( request )
        val account = gson.fromJson<Account>( body,Account::class.java )

        var id: Long? = null

        val query = Query.newEntityQueryBuilder().setNamespace( "com.arreev.api" ).setKind( "account" )
                .setFilter( StructuredQuery.PropertyFilter.eq("sub",account.sub ) )
                .build()
        val accounts = datastore.run( query )
        if ( (accounts != null) && accounts.hasNext() ) {
            val entity = accounts.next()
            if ( entity != null ) {
                id = entity.key.id
            }
        }

        val transaction = datastore.newTransaction()
        try {
            if ( id != null ) {
                val keyFactory = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "account" )
                val entity = transaction.get( keyFactory.newKey( id ) )
                if ( entity != null ) {
                    val builder = Entity.newBuilder( entity )
                            .set( "firstname",account.firstname )
                            .set( "lastname",account.lastname )
                            .set( "imageURL",account.imageURL )
                            .build()
                    transaction.put( builder )
                } else {
                    r.debug = "entity is null"
                }
                transaction.commit()
            } else {
                r.debug = "id is null"
            }
        } finally {
            if ( transaction.isActive ) {
                transaction.rollback()
            }
        }

        run {
            val query = Query.newEntityQueryBuilder().setNamespace( "com.arreev.api" ).setKind( "account" )
                    .setFilter( StructuredQuery.PropertyFilter.eq("sub",account.sub ) )
                    .build()
            val accounts = datastore.run( query )
            if ( (accounts != null) && accounts.hasNext() ) {
                val entity = accounts.next()
                if ( entity != null ) {
                    r.account = Account()
                    r.account?.sub = entity.getString("sub" )
                    r.account?.firstname = entity.getString("firstname" )
                    r.account?.lastname = entity.getString("lastname" )
                    r.account?.email = entity.getString("email" )
                    r.account?.role = entity.getString("role" )
                    r.account?.permissions = entity.getString("permissions" )
                    r.account?.groups = entity.getString("groups" )
                    r.account?.active = entity.getBoolean("active" )
                    r.account?.imageURL = entity.getString("imageURL" )
                    r.account?.thumbnailURL = entity.getString("thumbnailURL" )
                }
            }
        }

        response.status = 200
        response.contentType = "application/json"

        response.setHeader("access-control-allow-headers","Content-Type,Authorization,arreev-api-key" )
        response.setHeader("access-control-allow-methods","OPTIONS,GET,POST" )
        response.setHeader("access-control-allow-origin","*" )

        val json = gson.toJson( r,AccountResponse::class.java )
        response.writer.write( json )
    }
}

fun main( args:Array<String> ) {
    var http: HttpURLConnection? = null
    var input: InputStream? = null
    try {
        // val url = ""
        val url = "http://localhost:8080/account?sub=0"
        http = URL( url ).openConnection() as HttpURLConnection
        http.addRequestProperty( "arreev-api-key","1" )
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