
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
    val entityownerid = entity?.getString("ownerid" )
    val verified = entity?.getString("ownerid" )?.equals( ownerid,false ) ?: false
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
fun asRoute( entity: Entity? ) : Route? {
    var route: Route? = null

    if ( entity != null ) {
        route = Route()
        route.id = "${entity.key.id}"
        route.name = entity.getString("name" )
        route.description = entity.getString("description" )
        route.type = entity.getString("type" )
        route.category = entity.getString("category" )
        route.imageURL = entity.getString("imageURL" )
        route.thumbnailURL = entity.getString("thumbnailURL" )
        route.begAddress = entity.getString("begAddress" )
        route.endAddress = entity.getString("endAddress" )
        route.status = entity.getString("status" )
    }

    return route
}

@Throws( DatastoreException::class )
fun asWaypoint( entity: Entity? ) : Waypoint? {
    var waypoint: Waypoint? = null

    if ( entity != null ) {
        waypoint = Waypoint()
        waypoint.id = "${entity.key.id}"
        waypoint.name = entity.getString("name" )
        waypoint.description = entity.getString("description" )
        waypoint.type = entity.getString("type" )
        waypoint.category = entity.getString("category" )
        waypoint.imageURL = entity.getString("imageURL" )
        waypoint.thumbnailURL = entity.getString("thumbnailURL" )
        waypoint.address = entity.getString("address" )
        waypoint.latitude = entity.getDouble("latitude" )
        waypoint.longitude = entity.getDouble("longitude" )
        waypoint.index = entity.getLong("index" )
        waypoint.status = entity.getString("status" )
    }

    return waypoint
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
        transporter.inservice = entity.getBoolean("inservice" )
        transporter.description = entity.getString("description" )
        transporter.type = entity.getString("type" )
        transporter.category = entity.getString("category" )
        transporter.imageURL = entity.getString("imageURL" )
        transporter.thumbnailURL = entity.getString("thumbnailURL" )
        transporter.status = entity.getString("status" )
    }

    return transporter
}

@Throws( DatastoreException::class )
fun asFollow( entity: Entity? ) : Follow? {
    var follow: Follow? = null

    if ( entity != null ) {
        follow = Follow()
        follow.id = "${entity.key.id}"
        follow.name = entity.getString("name" )

        follow.notifyWhenArrive = entity.getBooleanOr("notifyWhenArrive",false )
        follow.notifyWhenDepart = entity.getBooleanOr("notifyWhenDepart",false )
        follow.notifyWhenDelayed = entity.getBooleanOr("notifyWhenDelayed",false )

        follow.subscribeToMessages = entity.getBooleanOr("subscribeToMessages",false )
        follow.subscribeToWarnings = entity.getBooleanOr("subscribeToWarnings",false )

        follow.status = entity.getString("status" )
    }

    return follow
}

@Throws( DatastoreException::class )
fun asAssignment( entity: Entity? ) : Assignment? {
    var assignment: Assignment? = null

    if ( entity != null ) {
        assignment = Assignment()
        assignment.id = "${entity.key.id}"
        assignment.type = entity.getString("type" )
        assignment.routeid = entity.getString("routeid" )
        assignment.transporterid = entity.getString("transporterid" )
        assignment.imageURL = entity.getString("imageURL" )
        assignment.thumbnailURL = entity.getString("thumbnailURL" )
        assignment.status = entity.getString("status" )
    }

    return assignment
}

@Throws( DatastoreException::class )
fun asGroup( entity: Entity? ) : Group? {
    var group: Group? = null

    if ( entity != null ) {
        group = Group()
        group.id = "${entity.key.id}"
        group.name = entity.getString("name" )
        group.type = entity.getString("type" )
        group.category = entity.getString("category" )
        group.description = entity.getString("description" )
        group.imageURL = entity.getString("imageURL" )
        group.thumbnailURL = entity.getString("thumbnailURL" )
        group.status = entity.getString("status" )
    }

    return group
}
