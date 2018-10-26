package com.userndot.sdk

import android.app.IntentService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.userndot.sdk.android.Logger
import java.lang.Exception

class UNDPushNotificationIntentService:IntentService(UNDPushNotificationIntentService::class.simpleName) {

    companion object {
        var MAIN_ACTION="com.userndot.PUSH_EVENT"
        var TYPE_BUTTON_CLICK="com.userndot.ACTION_BUTTON_CLICK"
    }
    override fun onHandleIntent(intent: Intent?) {
        var extras=intent?.extras
        if(extras==null) return

        var type=extras.getString("type")
        if(type!=null && TYPE_BUTTON_CLICK.equals(type)){
         Logger.d("UNDNotificationIntentService handling ${TYPE_BUTTON_CLICK}")
            handleActionButtonClickEvent(extras)
        }else{
            Logger.d("UNDNotificationIntentService unhandled intent ${intent?.action}")
        }
    }

    private fun handleActionButtonClickEvent(extras:Bundle){
        try {
            var autoCancel = extras.getBoolean("autoCancel", false)
            var notification_id = extras.getInt("notification_id", -1)
            var deepLink = extras.getString("deep_link", null)

            var intent: Intent

            if (deepLink != null) {
                intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
            } else {
                intent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
            }

            if (intent == null) {
                Logger.d("UNDNotificationService create launch event")
                return
            }

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP

            intent.putExtras(extras)
            intent.removeExtra("deep_link")

            if (autoCancel && notification_id > -1) {
                var notificationManagger = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (notificationManagger != null) {
                    notificationManagger.cancel(notification_id)
                }
            }
            sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            startActivity(intent)
        }catch (ex:Exception){
            Logger.d("UNDNotificationService  unable to process action button click${ex.localizedMessage}")
        }
    }
}