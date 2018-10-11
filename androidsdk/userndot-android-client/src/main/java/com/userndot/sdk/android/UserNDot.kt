package com.userndot.sdk.android

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock


class UserNDot {
    private var config: UserNDotConfig
    private var context: Context
    private var handler: Handler
    private var executor: ExecutorService
    private var lock: Lock
    private var commsRunnable: Runnable? = null
    private var TOKEN: String
    private var database: MyDatabase
    private var DEFAULT_URL = "https://userndot.com/event/check"
    private var INITIALIZE_URL = "https://userndot.com/event/"
    private var PROFILE_URL = ""
    private var EVENT_URL = ""
    private var PATH="USERNDOT"

    companion object {
        fun getDefaultInstance(context: Context): UserNDot? {
            //take from mainfest file
            var config = UserNDotConfig(userNDotID = "", fcmSenderID = "", debugLevel = 0, logger = Logger.getInstance(), sslPinning = false)
            return getInstanceWithConfig(context, config)
        }

        fun getInstanceWithConfig(context: Context, config: UserNDotConfig): UserNDot {
            return UserNDot(context, config)
        }
    }

    private constructor(context: Context, config: UserNDotConfig) {
        this.config = config
        this.context = context
        this.database = UserNDotDatabase.getDatabase(this.context);
        this.handler = Handler(Looper.getMainLooper())
        this.executor = Executors.newFixedThreadPool(5)
        this.lock = ReentrantLock()
        this.TOKEN = config.userNDotID
        postAsync(Runnable {
            initializeIdentity()
        })
    }

    private fun initializeIdentity() {
        try {
            lock.lock()
            var pref = context.getSharedPreferences(PATH, Context.MODE_PRIVATE)

            var userid = pref.getString("userId", "")
            var sessionId = pref.getString("sessionId", "")
            var deviceId = pref.getString("deviceId", "")
            var clientId = pref.getInt("clientId", -1)

            if (sessionId != "" && deviceId != "") {
                var data=Data()
                data.type="identity"
                sendQueue(context, listOf(data))
            }
        } finally {
            lock.unlock()
        }



    }

    private fun checkConectivity(): Boolean {

        var connection: HttpURLConnection? = null
        try {
            connection = buildConnection(DEFAULT_URL, "GET")
            if (connection.responseCode != 200) {
                return false
            }
        } catch (ex: Exception) {
            return false
        }
        return true
    }

    private fun buildConnection(url: String, methodType: String): HttpURLConnection {
        var url = URL(url);
        var connection = url.openConnection() as HttpURLConnection;
        connection.connectTimeout = 10000;
        connection.requestMethod = methodType;
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", TOKEN)
        connection.doInput = true;
        connection.doOutput = true;
        return connection;
    }

//    private fun sendData() {
//
//        var db = UserNDotDatabase.getDatabase(context)
//
//        var list: List<Data> = db.persistData().get()
//        list.forEach {
//            try {
//                var future = executor.submit(Task(it, db, this))
//            } catch (ex: Exception) {
//                //call postdelay remove
//            }
//        }
//    }

//    fun sendAppLocalData(url: String, methodType: String, requestParams: HashMap<String, String>? = null, body: Any? = null): String? {
//
//        var connection: HttpURLConnection? = null;
//        if (requestParams != null) {
//            var newurl = url + "?" + getQueryString(requestParams)
//            connection = buildConnection(newurl, methodType);
//        } else {
//            connection = buildConnection(url, methodType)
//        }
//        if (methodType == "GET" && connection != null) {
//            getRequest(connection)
//        } else if (methodType == "POST" && connection != null) {
//            var stream = postRequest(connection, body)
//            if (stream != null) {
//                var reader = BufferedReader(InputStreamReader(stream))
//                val sb = StringBuilder()
//                var line: String? = ""
//                do {
//                    sb.append(line)
//                    line = reader.readLine()
//                } while (line != null)
//                reader.close()
//                return sb.toString();
//            }
//        }
//        return null
//    }
//
//    fun getRequest(connection: HttpURLConnection) {
//        var inputStrem: InputStream = connection.inputStream
//    }

//    fun postRequest(connection: HttpURLConnection, body: Any?): InputStream? {
//        if (body != null) {
//            var outputStream = connection.outputStream
//            outputStream.write(ObjectMapper().writeValueAsString(body).toByteArray())
//            outputStream.flush()
//            outputStream.close()
//        }
//        println(connection.responseCode)
//        try {
//            return connection.inputStream
//        } catch (ex: Exception) {
//            //hb is down
//            throw Exception()
//        }
//
//    }

//    fun getQueryString(value: HashMap<String, String>): String {
//        var keys = value.keys
//        var stringBuilder = StringBuilder()
//        var first = true
//        keys.forEach {
//            if (first) {
//                first = false
//            } else {
//                stringBuilder.append("&")
//            }
//            stringBuilder.append(URLEncoder.encode(it, "UTF-8"))
//            stringBuilder.append("=")
//            stringBuilder.append(URLEncoder.encode(value.get(it), "UTF-8"))
//        }
//        return stringBuilder.toString()
//    }

    private fun queueEvent(context: Context, data: Data) {
        postAsync(Runnable {
            processEvent(context, data)
        })
    }

    private fun processEvent(context: Context, data: Data) {
//        synchronized(eventLock) {
        try {
            database.persistData().save(data)
            scheduleQueueFlush(context)

        } catch (e: Throwable) {
        }

//        }
    }

    private fun scheduleQueueFlush(context: Context) {
        if (commsRunnable == null)
            commsRunnable = Runnable { flushQueueAsync(context) }
        // Cancel any outstanding send runnables, and issue a new delayed one
        handler.removeCallbacks(commsRunnable)
        handler.postDelayed(commsRunnable, 1000)
    }

    private fun flushQueueAsync(context: Context) {
        postAsync(Runnable { flushDBQueue(context) })
    }


    private fun flushDBQueue(context: Context) {
        var data = database.persistData().get()
        sendQueue(context, data)

    }

    //send data to backend
    private fun sendQueue(context: Context, data: List<Data>) {
        //check connectivity
        var connectivity=checkConectivity()
        if (connectivity){
            //build url connection for eventuser event identity
            var eventUser=buildConnection(PROFILE_URL,"POST")
            var event=buildConnection(EVENT_URL,"POST")
            var identity=buildConnection(INITIALIZE_URL,"POST")
            try {
                for (data in data){
                    when(data.type){
                        "eventUser" ->{sendToServer(eventUser,data,true)}
                        "event" ->{sendToServer(event,data,false)}
                        "identity" ->{sendToServer(identity,null,true)}
                    }
                }
            }catch (ex:Exception){
                scheduleQueueFlush(context)
            }
        }else{
            scheduleQueueFlush(context)
        }
    }

    private fun sendToServer(conn:HttpURLConnection,data:Data?,process:Boolean){

        if(data!=null) {
            var outputStream = conn.outputStream
            outputStream.write(ObjectMapper().writeValueAsString(data.objectData).toByteArray())
            outputStream.flush()
            outputStream.close()
        }
        if(conn.responseCode!=200){
            throw Exception()
        }
        var inputStream=conn.inputStream
        if (inputStream != null) {
            var reader = BufferedReader(InputStreamReader(inputStream))
            val sb = StringBuilder()
            var line: String? = ""
            do {
                sb.append(line)
                line = reader.readLine()
            } while (line != null)
            reader.close()
            processResponse(sb.toString(),process,data?.id)
        }
    }

    private fun processResponse(response:String?,process: Boolean,id:Long?){
        //delete from db
        if(id!=null)
        database.persistData().delete(id)
        if(process) {
            //and update identity
            var pref = context.getSharedPreferences(PATH, Context.MODE_PRIVATE)
            //convert reponse into identity object
            var identity=ObjectMapper().readValue(response,Identity::class.java)
            with(pref.edit()){
                putInt("clientId",(identity.clientId)?:-1)
                putString("userId",identity.userId)
                putString("deviceId",identity.deviceId)
                putString("sessionId",identity.sessionId)
                commit()

            }

        }
    }
    private fun postAsync(runnable: Runnable) {
        try {
            executor.submit(runnable)
        } catch (ex: Exception) {
            Logger.d("failed to perform task")
        }
    }

    private fun pushBasicProfile(data: Data) {
        queueEvent(context, data)
    }

    fun pushProfile(profile: EventUser?) {
        if (profile == null)
            return
        var type = "eventUser"


        this.database = UserNDotDatabase.getDatabase(this.context);

        var data: Data = Data()

        var mapper = ObjectMapper()
        var jsonObject = mapper.writeValueAsString(profile)
        var time = getDate()



        data.objectData = jsonObject;
        data.time = time
        data.type = type

        postAsync(Runnable { pushBasicProfile(data) })
    }

    fun pushEvent(event: Event?) {
        if (event == null)
            return
        var type = "event"


        this.database = UserNDotDatabase.getDatabase(this.context);

        var data: Data = Data()

        var mapper = ObjectMapper()
        var jsonObject = mapper.writeValueAsString(event)
        var time = getDate()



        data.objectData = jsonObject;
        data.time = time
        data.type = type

        queueEvent(context, data)
    }

    private fun getDate(millis: Long = System.currentTimeMillis()): String {
        var date = Date(millis)
        var formatter = SimpleDateFormat("yyyy-mm-ddThh:mm:ssZ")
        return formatter.format(date)

    }

    fun onUserLogin(profile: EventUser?) {
        if (profile==null) return
        postAsync(Runnable() {
            // try and flush and then reset the queues
            flushDBQueue(context);
            clearQueues(context);
            pushProfile(profile);
        });

    }

    private fun clearQueues(context: Context) {
        database.persistData().deleteAll()
    }

}


//class Task : Callable<String?> {
//    var data: Data
//    var db: MyDatabase
//    var obj: UserNDot
//
//    constructor(data: Data, db: MyDatabase, obj: UserNDot) {
//        this.data = data
//        this.db = db
//        this.obj = obj
//    }
//
//    override fun call(): String? {
//        try {
//            if (data.type == "eventUser") {
//                var response = obj.sendAppLocalData(obj.PROFILE_URL, "POST", null, data)
//                //deleting from db by id
//                if (response != null) {
//                    //initialize identity
//                    db.persistData().delete(data.id!!)
//                }
//                return response
//            }
//            if (data.type == "event") {
//                var response = obj.sendAppLocalData(obj.EVENT_URL, "POST", null, data)
//                //deleting from db by id
//                if (response != null) {
//                    db.persistData().delete(data.id!!)
//                }
//                return response
//            }
//        } catch (ex: Exception) {
//            throw Exception()
//        }
//        return null
//    }
//
//}
