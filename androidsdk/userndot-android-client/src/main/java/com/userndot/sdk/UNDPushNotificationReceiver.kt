package com.userndot.sdk

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.userndot.sdk.android.Constants
import com.userndot.sdk.android.Logger
import com.userndot.sdk.android.UserNDot
import java.lang.Exception

class UNDPushNotificationReceiver :BroadcastReceiver(){

    override fun onReceive(context: Context?, intent: Intent?) {

        if(context!=null && intent!=null){
            try{
            var launchIntent:Intent
            var extras=intent.extras
            if(extras==null) return
            if(extras.containsKey(Constants.DEEP_LINK_KEY)){
                Logger.i("und_link",extras.getString(Constants.DEEP_LINK_KEY))
                launchIntent=Intent(Intent.ACTION_VIEW, Uri.parse(extras.getString(Constants.DEEP_LINK_KEY)))
            }else{
                launchIntent=(context.packageManager).getLaunchIntentForPackage(context.packageName)
                if(launchIntent==null) { Logger.i("USERNDOT","Not able to get default app intent."); return}
            }

            UserNDot.handleNotificationClicked(context,extras)

            launchIntent.flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            launchIntent.putExtras(extras)
            //if activity not found then throw exception
            context.startActivity(launchIntent)
            Logger.i("USERNDOT","UNDPushNotification click is handled")

        }catch (ex: ActivityNotFoundException){
            Logger.i("USERNDOT","UNDPushNotification click handle excception ${ex.localizedMessage}")
        }
        }

    }
}