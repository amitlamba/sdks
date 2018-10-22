package com.userndot.androidsdk

import android.app.Application
//import com.userndot.sdk.android.ActivityLifeCycleCallBack

class MyApplication : Application(){

    public  override fun onCreate(){
        super.onCreate()
//        ActivityLifeCycleCallBack.register(this)
    }

}
