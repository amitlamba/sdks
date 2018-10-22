package com.userndot.sdk.android

import android.os.Build
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

open class Event {
    lateinit var name: String
    var identity: Identity = Identity()
    //    private var creationTime: LocalDateTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//        LocalDateTime.now(ZoneId.of("UTC"))
//    } else {
//        TODO("VERSION.SDK_INT < O")
//
//    }
    var ipAddress: String? = null
    var city: String? = null
    var state: String? = null
    var country: String? = null
    var latitude: String? = null
    var longitude: String? = null
    var agentString: String? = null
    var userIdentified: Boolean = false
    var lineItem: MutableList<LineItem> = mutableListOf()
    var attributes: HashMap<String, Any> = hashMapOf()
}

class LineItem {
    var price: Int = 0
    var currency: String? = null
    var product: String? = null
    var categories: MutableList<String> = mutableListOf()
    var tags: MutableList<String> = mutableListOf()
    var quantity: Int = 0
    var properties: HashMap<String, Any> = hashMapOf()
}

class Identity(
        //unique id assigned to a device, should always remain fixed, create new if not found
        var deviceId: String = "",
        //if userId is not found assign a new session id, handle change if user login changes, logouts etc
        var sessionId: String = "",
        // id of event user, this id is assigned when a user profile is identified.
        var userId: String? = null,
        var clientId: Int? = -1
)




class EventUser {

    var identity: Identity = Identity()
    var email: String? = null
    var uid: String? = null //this is id of the user client has provided
    var undId: String? = null
    var fbId: String? = null
    var googleId: String? = null
    var mobile: String? = null
    var firstName: String? = null
    var lastName: String? = null
    var gender: String? = null
    var dob: String? = null
    var country: String? = null
    var city: String? = null
    var address: String? = null
    var countryCode: String? = null
    var additionalInfo: HashMap<String, Any> = hashMapOf()
//    var creationDate: LocalDateTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//        LocalDateTime.now(ZoneId.of("UTC"))
//    } else {
//        TODO("VERSION.SDK_INT < O")
//    }

}

@Entity(tableName = "dataTable")
class Data{

    @PrimaryKey(autoGenerate = true)
    var id:Long?=null
    @ColumnInfo(name="object")
    var objectData:String?=null
    @ColumnInfo(name = "time")
    var time:String?=null
    @ColumnInfo(name = "type")
    var type:String?=null



}