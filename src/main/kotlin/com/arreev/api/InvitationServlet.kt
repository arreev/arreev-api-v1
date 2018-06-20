
package com.arreev.api

import com.google.gson.*
import javax.servlet.http.*
import com.google.cloud.datastore.*

/**
 *
 */
class InvitationServlet : HttpServlet()
{
    private val datastore = DatastoreOptions.getDefaultInstance().service
    private val gson = GsonBuilder().create()

    class InvitationResponse : APIResponse()
    {
        var invitation: Invitation? = null
        var debug: String? = null
    }

    override fun doOptions( request:HttpServletRequest,response:HttpServletResponse ) {
        if ( !verifySSL( request ) ) {
            response.sendError( HttpServletResponse.SC_FORBIDDEN );
            return;
        }

        response.status = 200
        response.setHeader("access-control-allow-headers","Content-Type,Authorization,arreev-api-key" )
        response.setHeader("access-control-allow-methods","OPTIONS,POST" )
        response.setHeader("access-control-allow-origin","*" )
    }

    override fun doPost( request:HttpServletRequest,response:HttpServletResponse ) {
        if ( !verifySSL( request ) ) {
            response.sendError( HttpServletResponse.SC_FORBIDDEN );
            return;
        }

        response.status = 200
        response.contentType = "application/json"
        response.setHeader("access-control-allow-headers","Content-Type,Authorization,arreev-api-key" )
        response.setHeader("access-control-allow-methods","POST" )
        response.setHeader("access-control-allow-origin","*" )

        val r = InvitationResponse()

        var transaction: Transaction? = null
        try {
            val ownerid = request.getParameter("ownerid" )
            ownerid ?: throw GarbageInException( "missing ownerid" )

            val body = body( request )
            val person = gson.fromJson<Person>(body ?: "{}",Person::class.java )
            person ?: throw GarbageInException( "missing person" )
            person.id ?: throw GarbageInException( "bad person id" )

            val filters = StructuredQuery.CompositeFilter.and(
                    StructuredQuery.PropertyFilter.eq("ownerid",ownerid ),
                    StructuredQuery.PropertyFilter.eq("personid",person.id )
            )

            transaction = datastore.newTransaction()
            /**************************************************/

            val query = Query.newEntityQueryBuilder().setNamespace( "com.arreev.api" ).setKind( "invitation" )
                    .setFilter( filters )
                    .build()

            val now = com.google.cloud.Timestamp.now().toSqlTimestamp().toString()

            val entitys = datastore.run( query )
            if ( entitys.hasNext() ) {
                val key = entitys.next().key
                val entity = Entity.newBuilder( key )
                        .set( "ownerid",ownerid )
                        .set( "personid",person.id )
                        .set( "email",person.email )
                        .set( "status","pending" )
                        .set( "from",now )
                        .build()
                datastore.update( entity )
                val e = entity
                r.invitation = asInvitation( e )
                r.debug = "updated"
            } else {
                val key = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "invitation" ).newKey()
                val entity = FullEntity.newBuilder( key )
                        .set( "ownerid",ownerid )
                        .set( "personid",person.id )
                        .set( "email",person.email )
                        .set( "status","pending" )
                        .set( "from",now )
                        .build()
                val e = datastore.add( entity )
                r.invitation = asInvitation( e )
                r.debug = "added"
            }

            /**************************************************/
            transaction.commit()

            val json = gson.toJson( r,InvitationResponse::class.java )
            response.writer.write( json )
        } catch ( x:GarbageInException ) {
            response.sendError(400,x.message )
        } catch ( x: DatastoreException) {
            response.sendError(500,x.message )
        } finally {
            if ( transaction?.isActive ?: false ) {
                transaction?.rollback()
            }
        }
    }
}