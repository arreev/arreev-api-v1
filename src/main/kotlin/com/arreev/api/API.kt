
package com.arreev.api

import java.io.*
import javax.servlet.http.*

import com.google.cloud.datastore.*
import com.google.appengine.api.utils.*

val VERSION = "1.00.1"

class API
{
}

fun verifySSL(request:HttpServletRequest ) : Boolean {
    var verified = request.isSecure

    try {
        if ( SystemProperty.environment.value() == SystemProperty.Environment.Value.Development ) {
            verified = true
        }
    } catch ( x:Throwable ) {
        x.printStackTrace()
    }

    return verified
}

fun verifyOwnership(entity: Entity?, ownerid: String? ) : Boolean {
    var verified = false

    val entityownerid = entity?.getString("ownerid" )
    verified = entity?.getString("ownerid" )?.equals( ownerid,false ) ?: false

    return verified
}

@Throws( IOException::class )
fun body( request: HttpServletRequest ) : String {
    var json = "{}"

    val output = ByteArrayOutputStream(4096 )
    val input = request.inputStream
    try {
        val chunk = ByteArray(4096 )
        var nbr = 0
        while ( nbr != -1 ) {
            nbr = input.read( chunk )
            if ( nbr != - 1 ) {
                output.write( chunk,0,nbr )
            }
        }
        json = String( output.toByteArray() )
    } finally {
        output.close()
    }

    return json
}

@Throws( IOException::class )
fun readAsJson( input:InputStream ) : String {
    var json = ""

    val output = ByteArrayOutputStream(4096 )
    try {
        val chunk = ByteArray(4096 )
        var nbr = 0
        while ( nbr != -1 ) {
            nbr = input.read( chunk )
            if ( nbr > 0 ) {
                output.write( chunk,0,nbr )
            }
        }
        json = String( output.toByteArray() ) // kotlin is always UTF-8
    } finally {
        output.close()
    }

    return json
}

@Throws( DatastoreException::class )
fun asFleet( entity: Entity? ) : Fleet? {
    var fleet: Fleet? = null

    if ( entity != null ) {
        fleet = Fleet()
        fleet.id = "${entity.key.id}"
        fleet.name = entity.getString("name" )
        fleet.description = entity.getString("description" )
        fleet.type = entity.getString("type" )
        fleet.category = entity.getString("category" )
        fleet.imageURL = entity.getString("imageURL" )
        fleet.thumbnailURL = entity.getString("thumbnailURL" )
        fleet.status = entity.getString("status" )
    }

    return fleet
}

@Throws( DatastoreException::class )
fun asTransporter( entity: Entity? ) : Transporter? {
    var transporter: Transporter? = null

    if ( entity != null ) {
        transporter = Transporter()
        transporter.id = "${entity.key.id}"
        transporter.name = entity.getString("name" )
        transporter.number = entity.getLong("number" );
        transporter.marquee = entity.getString("marquee" )
        transporter.diatribe = entity.getString("diatribe" )
        transporter.latitude = entity.getDouble("latitude" );
        transporter.longitude = entity.getDouble("longitude" );
        transporter.description = entity.getString("description" )
        transporter.type = entity.getString("type" )
        transporter.category = entity.getString("category" )
        transporter.imageURL = entity.getString("imageURL" )
        transporter.thumbnailURL = entity.getString("thumbnailURL" )
        transporter.status = entity.getString("status" )
    }

    return transporter
}
