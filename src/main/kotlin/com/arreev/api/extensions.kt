
package com.arreev.api

import com.google.cloud.datastore.*

fun Entity.getBooleanOr( name:String, defaultValue:Boolean ) : Boolean {
    var value = defaultValue

    try {
        if ( names.contains( name ) ) {
            value = getBoolean( name ) ?: defaultValue
        }
    } catch ( x:DatastoreException ) {
        x.printStackTrace()
    }

    return value
}

fun Entity.getStringOr( name:String, defaultValue:String ) : String {
    var value = defaultValue

    try {
        if ( names.contains( name ) ) {
            value = getString( name ) ?: defaultValue
        }
    } catch ( x:DatastoreException ) {
        x.printStackTrace()
    }

    return value
}