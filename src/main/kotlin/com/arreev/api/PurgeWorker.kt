
package com.arreev.api

import javax.servlet.http.*
import com.google.cloud.datastore.*

class PurgeWorker : HttpServlet()
{
    private val datastore = DatastoreOptions.getDefaultInstance().service

    override fun doPost( request:HttpServletRequest,response:HttpServletResponse ) {
        val ownerid = request.getParameter("ownerid" )
        val subject = request.getParameter("subject" )
        println( "<<<<< PurgeWorker.doPost $ownerid $subject >>>>>" )

        when ( subject ) {
            "persons" -> cleanupPersons( ownerid )
        }
    }

    private fun cleanupPersons( ownerid:String ) {
        println( "<<<<< PurgeWorker.cleanupPersons >>>>>" )

        var transaction: Transaction? = null
        try {
            val deleted = mutableListOf<String>()

            transaction = datastore.newTransaction()

            val filters = StructuredQuery.CompositeFilter.and(
                    StructuredQuery.PropertyFilter.eq("ownerid",ownerid )
            )
            val query = Query.newEntityQueryBuilder().setNamespace( "com.arreev.api" ).setKind( "person" )
                    .setFilter( filters )
                    .setLimit( 500 ) // TODO: this is not scalable
                    .build()

            /*
             * query all persons for ownerid
             */
            val entitys = datastore.run( query )
            while ( entitys.hasNext() ) {
                val entity = entitys.next()
                val personid = "${entity.key.id}"
                val groupid = entity.getString("groupid" )
                /*
                 * see if a group by this id exists
                 */
                val groupKeyFactory = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "group" )
                val groupEntity = datastore.get( groupKeyFactory.newKey( groupid.toLong() ) )
                /*
                 * if no such group, delete this person
                 */
                if ( groupEntity == null ) {
                    val personKeyFactory = datastore.newKeyFactory().setNamespace( "com.arreev.api" ).setKind( "person" );
                    val personKey = personKeyFactory.newKey( personid.toLong() )
                    datastore.delete( personKey )
                    deleted.add( personid )
                }
            }

            transaction.commit()

            deleted.forEach { p -> println( "DELETED $p" ) }
        } catch ( x:DatastoreException ) {

        } finally {
            if ( transaction?.isActive ?: false ) {
                transaction?.rollback()
            }
        }
    }
}
