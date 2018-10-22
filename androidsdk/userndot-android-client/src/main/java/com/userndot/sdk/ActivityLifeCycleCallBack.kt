package com.userndot.sdk.android

import android.annotation.TargetApi
import android.app.Activity
import android.os.Build
import android.os.Bundle
import java.util.logging.Logger

class ActivityLifeCycleCallBack{

    companion object {
        var registered = false
        /**
         * Enables lifecycle callbacks for Android devices
         * @param application App's Application object
         */

        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        fun register(application: android.app.Application?) {
            if (application == null) {
//            Logger.i("Application instance is null/system API is too old")
                return
            }

            if (registered) {
//            Logger.v("Lifecycle callbacks have already been registered")
                return
            }

            registered = true
            application.registerActivityLifecycleCallbacks(
                    object : android.app.Application.ActivityLifecycleCallbacks {

                        override fun onActivityCreated(activity: Activity, bundle: Bundle) {
//                        UserNDot.onActivityCreated(activity)
                        }

                        override fun onActivityStarted(activity: Activity) {}

                        override fun onActivityResumed(activity: Activity) {
//                        UserNDot.onActivityResumed(activity)
                        }

                        override fun onActivityPaused(activity: Activity) {
//                        UserNDot.onActivityPaused()
                        }

                        override fun onActivityStopped(activity: Activity) {}

                        override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}

                        override fun onActivityDestroyed(activity: Activity) {}
                    }

            )
//        Logger.i("Activity Lifecycle Callback successfully registered")
        }
    }

}