package com.userndot.sdk.android

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings.Global.getString
import android.util.Log
import androidx.annotation.IntegerRes
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.iid.FirebaseInstanceId
import com.userndot.sdk.R
import com.userndot.sdk.UNDPushNotificationIntentService
import com.userndot.sdk.UNDPushNotificationReceiver
import com.userndot.sdk.Utils
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.lang.NullPointerException
import java.lang.NumberFormatException
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import javax.net.ssl.HttpsURLConnection
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class UserNDot {
    private var config: UserNDotConfig
    private var context: Context
    private var handler: Handler
    private var executor: ExecutorService
    private var lock: Lock
    private var commsRunnable: Runnable? = null
    private lateinit var database: MyDatabase
    private var BASE_URL = "http://userndot.com"
    private var DEFAULT_URL = BASE_URL + "/event/check"
    private var INITIALIZE_URL = BASE_URL + "/event/event/initialize"
    private var PROFILE_URL = BASE_URL + "/event/push/profile"
    private var EVENT_URL = BASE_URL + "/event/push/event"
    private var PATH = "USERNDOT"
    private val DELAY = 3000L
    private val CONNTIMEOUT=5000
    private val DEFAULT_NOTIFICATION_ID=-1000
    private lateinit var deviceInfo: DeviceInformation
    private var identity: Identity? = Identity()
    private var mapper = ObjectMapper()
    private var logger: Logger
    private var CHANNEL_ID:String="1000"
    private var DEFAULT_NOTIFICATION_CHANNEL="USERNDOT"
    private var DEFAULT_CHANNEL_ID="0"

    enum class LogLevel(val intValue: Int) {
        OFF(-1),
        INFO(0),
        DEBUG(2);
    }

    companion object {

        var debugLevel = LogLevel.INFO
        var instance:UserNDot?=null
        fun getDefaultInstance(context: Context): UserNDot? {
            try {
                var token = getManifestInfo(context)   //getting manifest info
                var map = HashMap<String, Any?>()
                map.put("TOKEN", token)
                saveSharedPreference(context, map)     //saving token in shared preference
                var config = getConfigInstance(token)  //build config object
                return getInstanceWithConfig(context, config)
            } catch (ex: PackageManager.NameNotFoundException) {
                return null
            } catch (ex: NullPointerException) {
                return null
            }
        }

        fun getInstanceWithConfig(context: Context, config: UserNDotConfig): UserNDot {
            Log.e("Thread inside inst conf", Thread.currentThread().name)
            instance=UserNDot(context, config)
            return instance as UserNDot
        }

        /*
         return Token from manifest file
         */
        private fun getManifestInfo(context: Context): String {
            var packageManager = context.packageManager
            var applicationInfo = packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            var bundle = applicationInfo.metaData
            return bundle.getString("STAGGING_TOKEN")
        }

        /*
        save value in shared preference
         */
        private fun saveSharedPreference(context: Context, pair: HashMap<String, Any?>) {
            var pref = context.getSharedPreferences("USERNDOT", Context.MODE_PRIVATE)
            var prefEditor = pref.edit()
            pair.forEach{
                var key=it.key
                var value=it.value
                try{
                    var newvalue=Integer.parseInt(value.toString())
                    prefEditor.putInt(key,newvalue)
                }catch (ex:NumberFormatException){
                    prefEditor.putString(key,value?.toString())
                }
            }
            prefEditor.commit()
        }
        /*
        return UserNDotConfig object
         */
        private fun getConfigInstance(token:String):UserNDotConfig{
            var logger:Logger=Logger.getInstance(UserNDot.LogLevel.INFO)
            return UserNDotConfig(userNDotID = token, fcmSenderID = "", debugLevel = 0, logger = logger, sslPinning = false)
        }

        fun createNotification(context: Context,bundle: Bundle){
            Log.e("create ","notification ${instance}")
            instance?.buildNotification(context,bundle)
        }
        
        fun handleNotificationClicked(context: Context?,notification:Bundle){
            //push notification click event on server
        }

    }

    private constructor(context: Context, config: UserNDotConfig) {
        this.config = config
        this.context = context
        this.logger = config.logger!!
        this.handler = Handler(Looper.getMainLooper())
        this.executor = Executors.newFixedThreadPool(1)
        this.lock = ReentrantLock()
//        postAsync(Runnable {
//            initializeIdentity()
//        })
    }
    /*
    return value of int and string type store in shared preference
     */
    private fun getSharedPreference(values: HashMap<String,Any?>):HashMap<String,Any>{
        var pref = context.getSharedPreferences(PATH, Context.MODE_PRIVATE)
        var map:HashMap<String,Any> = HashMap()
        values.forEach{
            var key=it.key
            var value=it.value
            try{
                var newvalue=Integer.parseInt(value?.toString())
                map.put(key,pref.getInt(key,newvalue))
            }catch (ex:NumberFormatException){
                map.put(key,pref.getString(key, value?.toString()))
            }
        }
        return map
    }
    /*
        Initialize the identity
     */
    private fun initializeIdentity() {
        logger.info("Thread inside initialize identity", Thread.currentThread().name)
        this.database = UserNDotDatabase.getDatabase(this.context)      //getting database connection
        this.deviceInfo = DeviceInformation(context)                    //getting device info
        clearQueues(context)
        try {
            var map=HashMap<String,Any?>()
            map.put("userId",null)
            map.put("sessionId","")
            map.put("deviceId","")
            map.put("clientId",-1)
            var result=getSharedPreference(map)

            if (result["sessionId"]== "" || result["deviceId"] == "") {
                var data = Data()
                data.type = "identity"
                queueEvent(context, data)
            } else {
                identity?.clientId = Integer.parseInt(result["clientId"].toString())
                identity?.deviceId = result["deviceId"].toString()
                identity?.sessionId = result["sessionId"].toString()
                identity?.userId = result["userId"].toString()
            }
        } finally {
            logger.info("identity Initialize finish")
        }


    }

    private fun checkConectivity(): Boolean {
        logger.info("Handshaking...")
        var connection: HttpURLConnection? = null
        try {
            connection = buildConnection(DEFAULT_URL, "GET",CONNTIMEOUT)
            connection.connect()
            if (connection.responseCode != 200) {
                logger.info("Error code ${connection.responseCode}")
                return false
            }
        } catch (ex: Exception) {
            logger.info("Excetion in handshaking ${ex.message}")
            return false
        }
        return true
    }

    private fun buildConnection(url: String, methodType: String,timeout:Int=10000): HttpURLConnection {
        var url = URL(url);
        var connection = url.openConnection() as HttpURLConnection;
        connection.connectTimeout = timeout;
        connection.requestMethod = methodType;
        connection.readTimeout=timeout
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", config.userNDotID)
        connection.setRequestProperty("User-Agent", "mobile")
        if(methodType.equals("POST",true)) {
            connection.doInput = true;
            connection.doOutput = true;
            connection.setRequestProperty("deviceInfo", deviceInfo.getHeaderString())
        }
        return connection;
    }

    private fun queueEvent(context: Context, data: Data) {
        postAsync(Runnable {
            processEvent(context, data)
        })
    }

    private fun processEvent(context: Context, data: Data) {
        lock.lock()
        try {
            logger.info("Persist ", "Data")
            database.persistData().save(data)
            logger.info("schedule queue", "flush")
            scheduleQueueFlush(context)

        } catch (e: Throwable) {
            logger.info("Exception in Persist data", "${e.message}")
        }
        lock.unlock()
    }

    private fun scheduleQueueFlush(context: Context) {
        logger.info("Thread in schendule queue", Thread.currentThread().name)
        if (commsRunnable == null) {
            commsRunnable = Runnable { flushQueueAsync(context) }
        }
        // Cancel any outstanding send runnables, and issue a new delayed one
        handler.removeCallbacks(commsRunnable)
        handler.postDelayed(commsRunnable, DELAY)
    }

    private fun flushQueueAsync(context: Context) {
        postAsync(Runnable { flushDBQueue(context) })
    }


    private fun flushDBQueue(context: Context) {
        logger.info("Getting data from database")
        var data = database.persistData().get()
        logger.info("${data.size} record are ready to send in Queue")
        if (data.size > 0) {
            sendQueue(context, data)
        }

    }
    /*
    send data to backend
     */
    private fun sendQueue(context: Context, data: List<Data>) {
        var connectivity = checkConectivity()       //check connectivity
        logger.info("Handshake To Server ${connectivity}")
        if (connectivity) {
            //check is it possible to build connection one time
            var eventUserConn = buildConnection(PROFILE_URL, "POST")
            var eventConn = buildConnection(EVENT_URL, "POST")
            var identityConn = buildConnection(INITIALIZE_URL, "POST")
            try {
                for (data in data) {
                    logger.info("Entity Type", "${data.type}")
                    logger.info("Data id send to server", "${data.id}")
                    when (data.type) {
                        "eventUser" -> {
                            var eventUser = mapper.readValue(data.objectData, EventUser::class.java)
                            if (eventUser.identity.sessionId == "") {
                                eventUser.identity = identity!!
                            }
                            data.objectData = mapper.writeValueAsString(eventUser)
                            logger.info("EventUser for processing", data.objectData as String)
                            sendToServer(eventUserConn, data, true)
                        }
                        "event" -> {
                            var event = mapper.readValue(data.objectData, Event::class.java)
                            if (event.identity.sessionId == "") {
                                event.identity = identity!!
                            }
                            data.objectData = mapper.writeValueAsString(event)
                            logger.info("Event for processing", data.objectData as String)
                            sendToServer(eventConn, data, false)
                        }
                        "identity" -> {
                            sendToServer(identityConn, data, true)
                        }
                    }
                }
            } catch (ex: Exception) {
                logger.debug("Data sending fail", ex.message.toString())
                scheduleQueueFlush(context)
            }
        } else {
            logger.info("Problem to establish a connection to server")
            scheduleQueueFlush(context)
        }
    }

    private fun sendToServer(conn: HttpURLConnection, data: Data?, process: Boolean) {

        if (data?.type != "identity") {
            var outputStream = conn.outputStream
            outputStream.write((data?.objectData)!!.toByteArray())
            outputStream.flush()
            outputStream.close()
        }
        if (conn.responseCode != 200) {
            logger.info("Response Code ${conn.responseCode}")
            logger.info("Response Message ${conn.responseMessage}")
            throw Exception()
        }
        logger.info("Trying to open Input Stream on Connection")
        var inputStream = conn.inputStream
        if (inputStream != null) {
            var reader = BufferedReader(InputStreamReader(inputStream))
            val sb = StringBuilder()
            var line: String? = ""
            do {
                sb.append(line)
                line = reader.readLine()
            } while (line != null)
            reader.close()
            processResponse(sb.toString(), process, data?.id)
        }
    }

    private fun processResponse(response: String?, process: Boolean, id: Long?) {

        if (id != null) {
            logger.info("Delete data for id ${id} from local database")
            database.persistData().delete(id!!)
            logger.info("Delete for id ${id} is successfull")
        }
        if (process) {
            logger.info("Processing the response")
            var jsonNode = mapper.readTree(response)
            jsonNode = jsonNode.get("data")
            jsonNode = jsonNode.get("value")
            identity = mapper.readValue(jsonNode.toString(), Identity::class.java)
            var map=HashMap<String,Any?>()
            map.put("userId",identity?.userId)
            map.put("sessionId",identity?.sessionId)
            map.put("deviceId",identity?.deviceId)
            map.put("clientId",identity?.clientId)
            saveSharedPreference(context,map)       //override previous identity with newone
        }
    }

    private fun postAsync(runnable: Runnable) {
        Log.e("Thread in async", Thread.currentThread().name)
        try {
            executor.submit(runnable)
        } catch (ex: InterruptedException) {
            logger.debug("Thread Interrupted", ex)
        } catch (ex: ExecutionException) {
            logger.debug("Exception occur in executor", ex)
        }
        logger.info("Task Submitted Successfully")
    }

    private fun pushBasicProfile(data: Data) {
        queueEvent(context, data)
    }

    private fun pushProfile(profile: EventUser?) {
        logger.info("inside push profile")
        if (profile == null) return
        profile.identity = identity!!
        //add token
        profile.token=getFcmToken()
        var type = "eventUser"
        var data: Data = Data()

        var jsonObject = mapper.writeValueAsString(profile)
        var time = getDate()        //getting current creation time but not used

        data.objectData = jsonObject;
        data.time = time
        data.type = type

        logger.info("Profile for processing", data.objectData as String)
        postAsync(Runnable { pushBasicProfile(data) })
    }

    fun pushEvent(event: Event?) {
        logger.info("Inside push event")
        postAsync(Runnable {
            pushEventData(event)
        })

    }

    private fun getDate(millis: Long = System.currentTimeMillis()): String {
        var date = Date(millis)
        var formatter = SimpleDateFormat("yyyy-mm-dd'T'hh:mm:ssZ")
        return formatter.format(date)

    }

    private fun pushEventData(event: Event?) {

        if (event == null) return
        event.identity = identity!!
        var type = "event"
        var location = getLocation()
        var longitude = (location?.longitude)?.toString()
        var latitude = (location?.latitude)?.toString()
        logger.info("Location", " Provider ${location?.provider} long $longitude lat $latitude accuracy ${location?.accuracy}")
        event.longitude = if (longitude != null) longitude else "90.00"
        event.latitude = if (latitude != null) latitude else "90.00"
        var data: Data = Data()
        var jsonObject = mapper.writeValueAsString(event)
        var time = getDate()

        data.objectData = jsonObject;
        data.time = time
        data.type = type
        queueEvent(context, data)
    }

    fun onUserLogin(profile: EventUser?) {
        if (profile == null) return
        postAsync(Runnable() {
            //            flushDBQueue(context);
//            if we want to clear previous data in database on user login
//            clearQueues(context);
            pushProfile(profile);
        });

    }

    fun onLogout() {

        var map=HashMap<String,Any?>()
        map.put("userId",null)
        map.put("sessionId","")
        map.put("deviceId","")
        map.put("clientId",-1)
        saveSharedPreference(context,map)       //remove identity of user
        postAsync(Runnable {
            initializeIdentity()
        })
        identity?.clientId = -1
        identity?.userId = null
        identity?.sessionId = ""

    }

    private fun clearQueues(context: Context) {
        database.persistData().deleteAll()
    }

    private fun getLocation(): Location? {
        try {

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (locationManager == null) {
                return null
            }
            var bestLocation: Location? = null
            var lastLocation: Location

            var enabledLocProvider: List<String> = locationManager.getProviders(true)
            for (provider in enabledLocProvider) {
                try {
                    lastLocation = locationManager.getLastKnownLocation(provider)
                    if (lastLocation != null) {
                        if (bestLocation == null || (bestLocation).accuracy < lastLocation.accuracy) {
                            bestLocation = lastLocation
                        }
                    }
                } catch (ex: SecurityException) {

                }
            }
            return bestLocation
        } catch (ex: SecurityException) {
            return null
        } catch (ex: Exception) {
            return null
        }

    }

    fun findLocationOnUserNDotEnd() {
        logger.info("UserNDot are trying to get user location")

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 9)
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
        }

        var locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        var locationListner = object : LocationListener {
            override fun onLocationChanged(location: Location?) {
                logger.info("static loaction ${location?.longitude}")
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

            }

            override fun onProviderDisabled(provider: String?) {

            }

            override fun onProviderEnabled(provider: String?) {

            }
        }
        try {
            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListner, null)
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListner, null)
        } catch (ex: SecurityException) {
            locationManager.removeUpdates(locationListner)
        }


    }

    private fun getFcmToken():String?{
        var token:String?=null
        token=getCachedFcmToken()
        if(token==null){
            FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener {
                if (it.isSuccessful) {
                    token = it.result?.token
                    logger.info("Token is ${token}")
                }
            }.addOnFailureListener {
                logger.info("Token exception ${it.cause}")
            }
        }
        return token
    }

    private fun getCachedFcmToken():String?{
        var map=HashMap<String,Any?>()
        map.put("token",null)
        var result=getSharedPreference(map)
        return result.get("token")?.toString()?:null
    }

    private fun createNotificationChannel(channelId: String?, channelName: String?) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        var chName = channelName
        var chId = channelId
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (chName == null) {
                chName = DEFAULT_NOTIFICATION_CHANNEL
            }
            if (chId == null) {
                chId = DEFAULT_CHANNEL_ID
            }

            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(chId, chName, importance)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(context: Context, bundle: Bundle) {
        postAsync(Runnable {
            //TODO save notification event call push event  clever tap has type of each event


            var channelId = bundle.getString("channel_id", null)
            if (channelId == null) {
                channelId = DEFAULT_CHANNEL_ID
            }
            //creating notification builder
            var nBuilder = NotificationCompat.Builder(context, channelId)
            //adding title
            var contentTite = bundle.getString("title", null)
            if (contentTite == null) contentTite = context.applicationInfo.name
            //adding body
            var notifMessage = bundle.getString("body")
            // take notification data and add in intent
            var intent = Intent(context, UNDPushNotificationReceiver::class.java)
            intent.putExtras(bundle)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            val pendintIntent = PendingIntent.getBroadcast(context, 6, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            //adding small icon to notification
            var smallIcon: Int
            try {
                val x = ManifestInfo.getInstance(context)?.getNotificationIcon()
                        ?: throw IllegalArgumentException()
                smallIcon = context.resources.getIdentifier(x, "drawable", context.packageName)
                if (smallIcon == 0) throw IllegalArgumentException()
            } catch (t: Throwable) {
                smallIcon = deviceInfo.getAppIconAsIntId(context)
            }
            //adding priority 
            var priority = bundle.getString("pr")
            var priorityInt = NotificationCompat.PRIORITY_DEFAULT
            if (priority != null) {
                if (priority == "high") {
                    priorityInt = NotificationCompat.PRIORITY_HIGH
                }
                if (priority == "max") {
                    priorityInt = NotificationCompat.PRIORITY_MAX
                }
            }
            // add large icon
            var largeIcon = bundle.getString("lg_icon", null)
            nBuilder.setLargeIcon(Utils.getNotificationBitmap(largeIcon, true, context))
            //adding style to notification
            var style: NotificationCompat.Style
            var bigPictureUrl = bundle.getString("bg_pic", null)
            if (bigPictureUrl != null) {
                try {
                    var bpMap = Utils.getNotificationBitmap(bigPictureUrl, false, context)
                            ?: throw Exception("Failed to fetch big picture!")
                    style = NotificationCompat.BigPictureStyle()
                            .setSummaryText(notifMessage)
                            .bigPicture(bpMap)
                            .bigLargeIcon(null)
                } catch (t: Throwable) {
                    style = NotificationCompat.BigTextStyle()
                            .bigText(notifMessage)
                            .setSummaryText(notifMessage)
                }

            } else {
                style = NotificationCompat.BigTextStyle()
                        .bigText(notifMessage)
            }
            //add badge icon
            val badgeIconParam = bundle.getString("badge_icon", null)
            if (badgeIconParam != null) {
                try {
                    val badgeIconType = Integer.parseInt(badgeIconParam!!)
                    if (badgeIconType >= 0) {
                        nBuilder.setBadgeIconType(badgeIconType)
                    }
                } catch (t: Throwable) {
                    // no-op
                }
            }
            //TODO notificationid do this in create notification channel
            //add ringtone of notification
            try {
                if (bundle.containsKey("sound")) {
                    var soundUri: Uri? = null

                    val o = bundle.get("sound")

                    if (o is Boolean && o) {
                        soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    } else if (o is String) {
                        var s: String = o
                        if (s == "true") {
                            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        } else if (!s.isEmpty()) {
                            if (s.contains(".mp3") || s.contains(".ogg") || s.contains(".wav")) {
                                s = s.substring(0, s.length - 4)
                            }
                            soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.packageName + "/raw/" + s)

                        }
                    }

                    if (soundUri != null) {
                        nBuilder.setSound(soundUri)
                    }
                }
            } catch (t: Throwable) {
                logger.info("Could not process sound parameter")
            }
            //add action in notification builder
            var action = addAction(context, bundle, DEFAULT_NOTIFICATION_ID,nBuilder)
            nBuilder.setAutoCancel(true)
            nBuilder.setContentIntent(pendintIntent)
            nBuilder.setSmallIcon(smallIcon)
            nBuilder.setContentText(notifMessage)
            nBuilder.setContentTitle(contentTite)
            nBuilder.setPriority(priorityInt)
            nBuilder.setStyle(style)

            triggerNotification(nBuilder, bundle)
        })
    }
    
    private fun addAction(context: Context,bundle: Bundle,notificationId:Int,builder: NotificationCompat.Builder){
        var isUNDIntentServiceAvailable=isServiceAvailable(context,
                UNDPushNotificationIntentService.MAIN_ACTION)
        Log.e("is service available",isUNDIntentServiceAvailable.toString())
        var actionString=bundle.getString("action",null)
        var actionGroup:JSONArray
        if(actionString!=null){
            actionGroup=JSONArray(actionString)
        }else{
            actionGroup=JSONArray()
        }
        if(actionGroup!=null&&actionGroup.length()>0) {
            var i = 0
            while (i < actionGroup.length()) {
                //action must be json object and actionGroup is list of jsonArray
                var action:JSONObject = actionGroup.getJSONObject(i)
                var autoCancel = action.optBoolean("autoCancel")
                var deepLink=action.optString("deep_link")
                var lable=action.optString("label")   //mandatory
                var icon=action.optString("icon")
                var id=action.optString("id") //mandatory
                var ico=0
                if(!icon.isEmpty()){
                   try{
                       ico=context.resources.getIdentifier(icon,"drawable",context.packageName)
                   }catch(es:Exception){

                   }
                }
                var sendToUNDIntentService = autoCancel && isUNDIntentServiceAvailable
                var actionIntent: Intent
                if (sendToUNDIntentService) {
                    actionIntent = Intent(UNDPushNotificationIntentService.MAIN_ACTION)
                    if(!deepLink.isEmpty())
                        actionIntent.putExtra("deep_link",deepLink)

                    actionIntent.putExtra("type",UNDPushNotificationIntentService.TYPE_BUTTON_CLICK)
                } else {
                    if (!deepLink.isEmpty()) {
                        actionIntent = Intent(Intent.ACTION_VIEW, Uri.parse("dl"))
                    } else {
                        actionIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    }
                }
                if (actionIntent != null) {
                    actionIntent.putExtras(bundle)
                    actionIntent.putExtra("autoCancel", autoCancel)
                    actionIntent.putExtra("actionId",id)
                    //TODO notification is
                    actionIntent.putExtra("notification_id", notificationId)
                    actionIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                var pendingIntent: PendingIntent
                if (sendToUNDIntentService) {
                    pendingIntent = PendingIntent.getService(context, System.currentTimeMillis() as Int, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                } else {
                    pendingIntent = PendingIntent.getActivity(context, System.currentTimeMillis() as Int, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                }
                builder.addAction(ico,lable,pendingIntent)
            }
        }
    }
    /*
    * checking is service running or not is any broadcast reciver register for this action
    * */
    private fun isServiceAvailable(context: Context,action:String):Boolean{
        var packageManager=context.packageManager
        var intent=Intent(action)
        var registerServicesForThisIntent=packageManager.queryIntentServices(intent,0)
        if(registerServicesForThisIntent.size>0){
            return true
        }else{
            return false
        }
    }
    
    private fun triggerNotification(builder:NotificationCompat.Builder,bundle: Bundle){
        var nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var channelId = bundle.getString("channel_id", null)
        var channelName = bundle.getString("channel_name", null)
        var notificationId:Int?=null
        try {
            notificationId = Integer.parseInt(bundle.getString("notification_id", null))
        }catch (ex:NumberFormatException){

        }
        if(notificationId==null) {
            notificationId = (1..100).shuffled().first()
        }
        createNotificationChannel(channelId, channelName)
        if (channelName == null) {
            nm.notify(DEFAULT_NOTIFICATION_CHANNEL, notificationId, builder.build())
        } else {
            if (notificationId > 0) {
                nm.notify(channelName, notificationId, builder.build())
            }
        }
    }
}
