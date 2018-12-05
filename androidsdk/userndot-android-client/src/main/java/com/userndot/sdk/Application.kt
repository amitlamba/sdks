package com.userndot.sdk

import com.userndot.sdk.android.ActivityLifeCycleCallBack

class Application:android.app.Application() {

    override fun onCreate() {
        ActivityLifeCycleCallBack.register(this)
        super.onCreate()
    }
}