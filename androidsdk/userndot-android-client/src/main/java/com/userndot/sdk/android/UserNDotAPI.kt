package com.userndot.sdk.android

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

class UserNDotAPI {

    private var EXECUTOR_THREAD_ID: Long = 0
    private lateinit var context: Context
    private lateinit var es: ExecutorService
    private lateinit var config: UserNDotConfig

    private constructor(context: Context) {

    }


    private constructor(context: Context, userNDotConfig: UserNDotConfig) {
        this.context = context

        this.es = Executors.newFixedThreadPool(1)
    }

    public enum class LogLevel(val intValue: Int) {

        OFF(-1),
        INFO(0),
        DEBUG(2);

    }

    companion object {
        val debugLevel: LogLevel = LogLevel.INFO

        @Volatile
        private var INSTANCE: UserNDotAPI? = null

        fun getInstance(context: Context): UserNDotAPI =
                getInstance(context, UserNDotConfig("abc", "abc"))

        fun getInstance(context: Context, userNDotConfig: UserNDotConfig): UserNDotAPI =
                INSTANCE ?: synchronized(this) {
                    INSTANCE
                            ?: buildUserNDotAPI(context).also { INSTANCE = it }
                }

        private fun buildUserNDotAPI(context: Context): UserNDotAPI {
            return UserNDotAPI(context)
        }
    }

    /**
     * Push a profile update.
     *
     * @param profile A [Map], with keys as strings, and values as [String],
     * [Integer], [Long], [Boolean], [Float], [Double],
     * [java.util.Date], or [Character]
     */
    fun pushProfile(profile: EventUser?) {
        if (profile == null)
            return

        postAsyncSafely("profilePush", Runnable { _push(profile) })
    }

    private fun _push(profile: EventUser?) {
        if (profile == null)
            return

        try {
            val customProfile = JSONObject()
            val fieldsToUpdateLocally = JSONObject()

            // test Phone:  if no device country code, test if phone starts with +, log but always send
            if (profile.mobile != null) {
                try {
                    val countryCode = this.deviceInfo.getCountryCode()
                    if (countryCode == null || countryCode!!.isEmpty()) {
                        if (!profile.mobile?.startsWith("+")!!) {
                            val error = ValidationResult()
                            error.setErrorCode(512)
                            val err = "Device country code not available and profile phone: $profile.mobile does not appear to start with country code"
                            error.setErrorDesc(err)
                            pushValidationResult(error)
                            getConfigLogger().debug(this.config.userNDotID, err)
                        }
                    }
                    getConfigLogger().verbose(this.config.userNDotID, "Profile phone is: " + profile.mobile + " device country code is: " + if (countryCode != null) countryCode else "null")
                } catch (e: Exception) {
                    getConfigLogger().debug(this.config.userNDotID, "Invalid phone number: " + e.localizedMessage)
                }

            }

            // add to the local profile update object
            fieldsToUpdateLocally.put(key, value)
            customProfile.put(key, value)

            getConfigLogger().verbose(this.config.userNDotID, "Constructed custom profile: " + customProfile.toString())

            // update local profile values
            if (fieldsToUpdateLocally.length() > 0) {
                getLocalDataStore().setProfileFields(fieldsToUpdateLocally)
            }

            pushBasicProfile(customProfile)

        } catch (t: Throwable) {
            // Will not happen
            getConfigLogger().verbose(this.config.userNDotID, "Failed to push profile", t)
        }

    }

    private fun pushBasicProfile(customProfile: JSONObject) {

    }

    //Util
    /**
     * Use this to safely post a runnable to the async handler.
     * It adds try/catch blocks around the runnable and the handler itself.
     */
    private fun postAsyncSafely(name: String, runnable: Runnable) {
        try {
            val executeSync = Thread.currentThread().id == EXECUTOR_THREAD_ID

            if (executeSync) {
                runnable.run()
            } else {
                es.submit(Runnable {
                    EXECUTOR_THREAD_ID = Thread.currentThread().id
                    try {
                        runnable.run()
                    } catch (t: Throwable) {
                        getConfigLogger().verbose(this.config.userNDotID, "Executor service: Failed to complete the scheduled task", t)
                    }
                })
            }
        } catch (t: Throwable) {
            getConfigLogger().verbose(this.config.userNDotID, "Failed to submit task to the executor service", t)
        }

    }

    private fun getConfigLogger(): Logger {
        return this.config.logger
    }

    @SuppressLint("MissingPermission")
    private fun _getLocation(): Location? {
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (lm == null) {
                Logger.d("Location Manager is null.")
                return null
            }
            val providers = lm.getProviders(true)
            var bestLocation: Location? = null
            var l: Location? = null
            for (provider in providers) {
                try {
                    l = lm.getLastKnownLocation(provider)
                } catch (e: SecurityException) {
                    //no-op
                    Logger.v("Location security exception", e)
                }

                if (l == null) {
                    continue
                }
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            }

            return bestLocation
        } catch (t: Throwable) {
            Logger.v("Couldn't get user's location", t)
            return null
        }

    }

    private fun sendQueue(context: Context, queue: JSONArray?): Boolean {

        if (queue == null || queue.length() <= 0) return false

        if (this.config.userNDotID == null) {
            getConfigLogger().debug(this.config.userNDotID, "UserNDot Id not finalized, unable to send queue")
            return false
        }

        var conn: HttpsURLConnection? = null
        try {
            val endpoint = getEndpoint(false)

            // This is just a safety check, which would only arise
            // if upstream didn't adhere to the protocol (sent nothing during the initial handshake)
            if (endpoint == null) {
                getConfigLogger().debug(getAccountId(), "Problem configuring queue endpoint, unable to send queue")
                return false
            }

            conn = buildHttpsURLConnection(endpoint)

            var body: String

            synchronized(this) {

                val req = insertHeader(context, queue)
                if (req == null) {
                    getConfigLogger().debug(getAccountId(), "Problem configuring queue request, unable to send queue")
                    return false
                }

                getConfigLogger().debug(getAccountId(), "Send queue contains " + queue.length() + " items: " + req)
                getConfigLogger().debug(getAccountId(), "Sending queue to: " + endpoint!!)
                conn!!.doOutput = true
                conn.outputStream.write(req.toByteArray(charset("UTF-8")))

                val responseCode = conn.responseCode

                // Always check for a 200 OK
                if (responseCode != 200) {
                    throw IOException("Response code is not 200. It is $responseCode")
                }

                // Check for a change in domain
                val newDomain = conn.getHeaderField(Constants.HEADER_DOMAIN_NAME)
                if (newDomain != null && newDomain.trim { it <= ' ' }.length > 0) {
                    if (hasDomainChanged(newDomain)) {
                        // The domain has changed. Return a status of -1 so that the caller retries
                        setDomain(context, newDomain)
                        getConfigLogger().debug(getAccountId(), "The domain has changed to $newDomain. The request will be retried shortly.")
                        return false
                    }
                }

                if (processIncomingHeaders(context, conn)) {
                    val br = BufferedReader(InputStreamReader(conn.inputStream, "utf-8"))

                    val sb = StringBuilder()
                    var line: String
                    while ((line = br.readLine()) != null) {
                        sb.append(line)
                    }
                    body = sb.toString()
                    processResponse(context, body)
                }

                setLastRequestTimestamp(context, currentRequestTimestamp)
                setFirstRequestTimestampIfNeeded(context, currentRequestTimestamp)

                getConfigLogger().debug(getAccountId(), "Queue sent successfully")
            }
            mResponseFailureCount = 0
            return true
        } catch (e: Throwable) {
            getConfigLogger().debug(getAccountId(), "An exception occurred while sending the queue, will retry: " + e.localizedMessage)
            mResponseFailureCount++
            scheduleQueueFlush(context)
            return false
        } finally {
            if (conn != null) {
                try {
                    conn.inputStream.close()
                    conn.disconnect()
                } catch (t: Throwable) {
                    // Ignore
                }

            }
        }
    }

    //Networking
    private fun insertHeader(context: Context, arr: JSONArray): String? {
        try {
            val header = JSONObject()

            val deviceId = getUserNDOtID()
            if (deviceId != null && deviceId != "") {
                header.put("g", deviceId)
            } else {
                getConfigLogger().verbose(getAccountId(), "CRITICAL: Couldn't finalise on a device ID!")
            }

            header.put("type", "meta")

            val appFields = getAppLaunchedFields()
            header.put("af", appFields)

            val i = getI()
            if (i > 0) {
                header.put("_i", i)
            }

            val j = getJ()
            if (j > 0) {
                header.put("_j", j)
            }

            val accountId = getAccountId()
            val token = this.config.getAccountToken()

            if (accountId == null || token == null) {
                getConfigLogger().debug(getAccountId(), "Account ID/token not found, unable to configure queue request")
                return null
            }

            header.put("id", accountId)
            header.put("tk", token)
            header.put("l_ts", getLastRequestTimestamp())
            header.put("f_ts", getFirstRequestTimestamp())

            // Attach ARP
            try {
                val arp = getARP(context)
                if (arp != null && arp!!.length() > 0) {
                    header.put("arp", arp)
                }
            } catch (t: Throwable) {
                getConfigLogger().verbose(getAccountId(), "Failed to attach ARP", t)
            }

            val ref = JSONObject()
            try {

                val utmSource = getSource()
                if (utmSource != null) {
                    ref.put("us", utmSource)
                }

                val utmMedium = getMedium()
                if (utmMedium != null) {
                    ref.put("um", utmMedium)
                }

                val utmCampaign = getCampaign()
                if (utmCampaign != null) {
                    ref.put("uc", utmCampaign)
                }

                if (ref.length() > 0) {
                    header.put("ref", ref)
                }

            } catch (t: Throwable) {
                getConfigLogger().verbose(getAccountId(), "Failed to attach ref", t)
            }

            val wzrkParams = getWzrkParams()
            if (wzrkParams != null && wzrkParams!!.length() > 0) {
                header.put("wzrk_ref", wzrkParams)
            }

            inAppFCManager.attachToHeader(context, header)

            // Resort to string concat for backward compatibility
            return "[" + header.toString() + ", " + arr.toString().substring(1)
        } catch (t: Throwable) {
            getConfigLogger().verbose(getAccountId(), "CommsManager: Failed to attach header", t)
            return arr.toString()
        }

    }
}

data class UserNDotConfig(
        val userNDotID: String,
        val fcmSenderID: String,
        val debugLevel: Int,
        val logger: Logger,
        val sslPinning: Boolean
) {

}