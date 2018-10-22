package com.userndot.sdk

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMMessagingService:FirebaseMessagingService(){

    override fun onMessageReceived(p0: RemoteMessage?) {
        Log.e("Message",p0?.notification?.title)
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }
}