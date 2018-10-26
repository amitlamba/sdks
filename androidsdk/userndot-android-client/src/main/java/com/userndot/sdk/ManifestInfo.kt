package com.userndot.sdk.android

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle

class ManifestInfo {
    private var accountId: String? = null
    private var accountToken: String? = null
    private var accountRegion: String? = null
    private var gcmSenderId: String? = null
    private var useADID: Boolean = false
    private var appLaunchedDisabled: Boolean = false
    private var notificationIcon: String? = null

    private var excludedActivities: String? = null
    private var sslPinning: Boolean = false

    private fun _getManifestStringValueForKey(manifest: Bundle, name: String): String? {
        try {
            val o = manifest.get(name)
            return o?.toString()
        } catch (t: Throwable) {
            return null
        }

    }

    private constructor(context: Context) {
        var metaData: Bundle? = null
        try {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            metaData = ai.metaData
        } catch (t: Throwable) {
            // no-op
        }

        if (metaData == null) {
            metaData = Bundle()
        }
        if (accountId == null)
            accountId = _getManifestStringValueForKey(metaData, Constants.LABEL_ACCOUNT_ID)
        if (accountToken == null)
            accountToken = _getManifestStringValueForKey(metaData, Constants.LABEL_TOKEN)
        if (accountRegion == null)
            accountRegion = _getManifestStringValueForKey(metaData, Constants.LABEL_REGION)
        gcmSenderId = _getManifestStringValueForKey(metaData, Constants.LABEL_SENDER_ID)
        notificationIcon = _getManifestStringValueForKey(metaData, Constants.LABEL_NOTIFICATION_ICON)
        if (gcmSenderId != null) {
            gcmSenderId = gcmSenderId!!.replace("id:", "")
        }
        useADID = "1" == _getManifestStringValueForKey(metaData, Constants.USERNDOT_USE_GOOGLE_AD_ID)
        appLaunchedDisabled = "1" == _getManifestStringValueForKey(metaData, Constants.LABEL_DISABLE_APP_LAUNCH)
        excludedActivities = _getManifestStringValueForKey(metaData, Constants.LABEL_INAPP_EXCLUDE)
        sslPinning = "1" == _getManifestStringValueForKey(metaData, Constants.LABEL_SSL_PINNING)
    }

    companion object {
        private var instance: ManifestInfo? = null
        @Synchronized
        fun getInstance(context: Context): ManifestInfo? {
            if (instance == null) {
                instance = ManifestInfo(context)
            }
            return instance
        }
    }

    internal fun getAccountId(): String? {
        return accountId
    }

    internal fun getAcountToken(): String? {
        return accountToken
    }

    internal fun getAccountRegion(): String? {
        return accountRegion
    }

    fun getGCMSenderId(): String? {
        return gcmSenderId
    }

    fun useGoogleAdId(): Boolean {
        return useADID
    }

    fun isAppLaunchedDisabled(): Boolean {
        return appLaunchedDisabled
    }

    internal fun isSSLPinningEnabled(): Boolean {
        return sslPinning
    }

    internal fun getNotificationIcon(): String? {
        return notificationIcon
    }

    internal fun getExcludedActivities(): String? {
        return excludedActivities
    }

    internal fun changeCredentials(id: String, token: String, region: String) {
        accountId = id
        accountToken = token
        accountRegion = region
    }
}