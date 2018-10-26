package com.userndot.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import com.userndot.sdk.android.Constants
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.HashMap

internal object Utils {
    val memoryConsumption: Long
        get() {
            val free = Runtime.getRuntime().freeMemory()
            val total = Runtime.getRuntime().totalMemory()
            return total - free
        }

    fun convertBundleObjectToHashMap(b: Bundle): HashMap<String, Any> {
        val map = HashMap<String, Any>()
        for (s in b.keySet()) {
            val o = b.get(s)
            if (o is Bundle) {
                map.putAll(convertBundleObjectToHashMap(o))
            } else {
                map[s] = b.get(s)
            }
        }
        return map
    }

    fun convertJSONObjectToHashMap(b: JSONObject): HashMap<String, Any> {
        val map = HashMap<String, Any>()
        val keys = b.keys()

        while (keys.hasNext()) {
            try {
                val s = keys.next()
                val o = b.get(s)
                if (o is JSONObject) {
                    map.putAll(convertJSONObjectToHashMap(o))
                } else {
                    map[s] = b.get(s)
                }
            } catch (ignored: Throwable) {
                // Ignore
            }

        }

        return map
    }

    fun getCurrentNetworkType(context: Context): String {
        try {
            // First attempt to check for WiFi connectivity
            val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    ?: return "Unavailable"
            @SuppressLint("MissingPermission") val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

            if (mWifi.isConnected) {
                return "WiFi"
            }

            // Fall back to network type
            val teleMan = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    ?: return "Unavailable"
            val networkType = teleMan.networkType
            when (networkType) {
                TelephonyManager.NETWORK_TYPE_CDMA -> return "CDMA"
                TelephonyManager.NETWORK_TYPE_EDGE -> return "EDGE"
                TelephonyManager.NETWORK_TYPE_GPRS -> return "GPRS"
                TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_UMTS -> return "3G"
                TelephonyManager.NETWORK_TYPE_LTE -> return "LTE"
                else -> return "Unknown"
            }
        } catch (t: Throwable) {
            return "Unavailable"
        }

    }

    @Throws(NullPointerException::class)
    fun getNotificationBitmap(icoPath: String?, fallbackToAppIcon: Boolean, context: Context): Bitmap? {
        var icoPath = icoPath
        // If the icon path is not specified
        if (icoPath == null || icoPath == "") {
            return if (fallbackToAppIcon) getAppIcon(context) else null
        }
        // Simply stream the bitmap
        if (!icoPath.startsWith("http")) {
            icoPath = Constants.ICON_BASE_URL + "/" + icoPath
        }
        val ic = getBitmapFromURL(icoPath)

        return ic ?: if (fallbackToAppIcon) getAppIcon(context) else null
    }

    @Throws(NullPointerException::class)
    private fun getAppIcon(context: Context): Bitmap {
        // Try to get the app logo first
        try {
            val logo = context.packageManager.getApplicationLogo(context.applicationInfo)
                    ?: throw Exception("Logo is null")
            Log.e("logo",logo.toString())
            return drawableToBitmap(logo)
        } catch (e: Exception) {
            // Try to get the app icon now
            // No error handling here - handle upstream
            return drawableToBitmap(context.packageManager.getApplicationIcon(context.applicationInfo))
        }

    }

    @Throws(NullPointerException::class)
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth,
                drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    fun getBitmapFromURL(srcUrl: String): Bitmap? {
        var srcUrl = srcUrl
        // Safe bet, won't have more than three /s
        srcUrl = srcUrl.replace("///", "/")
        srcUrl = srcUrl.replace("//", "/")
        srcUrl = srcUrl.replace("http:/", "http://")
        srcUrl = srcUrl.replace("https:/", "https://")
        var connection: HttpURLConnection? = null
        try {
            val url = URL(srcUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            return BitmapFactory.decodeStream(input)
        } catch (e: IOException) {

            //            Logger.v("Couldn't download the notification icon. URL was: " + srcUrl);
            return null
        } finally {
            try {
                connection?.disconnect()
            } catch (t: Throwable) {
                //                .v("Couldn't close connection!", t);
            }

        }
    }
}
