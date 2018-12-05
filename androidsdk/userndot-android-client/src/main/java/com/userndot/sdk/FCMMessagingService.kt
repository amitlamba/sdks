package com.userndot.sdk


import android.os.Bundle
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.userndot.sdk.android.Constants
import com.userndot.sdk.android.Logger
import com.userndot.sdk.android.UserNDot
import com.userndot.sdk.android.UserNDot.Companion.createNotification

class FCMMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage?) {
        var bundle = Bundle()
        if (message?.data != null) {
            if (message.data.isNotEmpty()) {
                for (data in message.data) {
                    Log.e(data.key,data.value)
                    bundle.putString(data.key, data.value)
                }
                if (bundle.containsKey(Constants.FROMUSERNDOT)) {
                    Logger.d("FcmMessageListnerService received message from UserNDot ${bundle}")
                    createNotification(applicationContext, bundle)
                }
            }
        }
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }

    override fun onNewToken(p0: String?) {
        UserNDot.tokenRefresh(p0,applicationContext)
    }
}