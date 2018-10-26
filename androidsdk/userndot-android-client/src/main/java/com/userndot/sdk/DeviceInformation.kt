package com.userndot.sdk.android

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

class DeviceInformation {
    private  var appVersionName:String?
    private  var osName:String?
    private  var osVersion:String?
    private  var manufacturer:String?
    private  var model:String?
    private  var appPackageName:String?
    private  var sdkVersion:Int

    constructor(context:Context){
        appVersionName=setAppVersionName(context)
        osName=setOsName(context)
        osVersion=setOsVersion()
        manufacturer=setManufacturer(context)
        model=setModel(context)
        appPackageName=setAppPackageName(context)
        sdkVersion=setSdkVersion()
    }
    /*
    flag default 0, MATCH_UNINSTALLED_PACKAGES in this case package name not found in installed application
    it is searched in uninstalled application list
     */
    private fun setAppVersionName(context: Context):String?{
        var packageInfo:PackageInfo
        try{
            packageInfo=context.packageManager.getPackageInfo(context.packageName,0)
            return packageInfo.versionName
        }catch (ex:PackageManager.NameNotFoundException){

        }
        return null

    }
    private fun setAppPackageName(context: Context):String?{
        var packageName:String
        packageName=context.packageName
        return packageName

    }
    private fun setOsName(context: Context):String{
        return "Android"
    }
    private fun setModel(context: Context):String{
        return Build.MODEL
    }
    private fun setManufacturer(context: Context):String{
        return Build.MANUFACTURER
    }
    private fun setOsVersion():String{
        return Build.VERSION.RELEASE
    }
    private fun setSdkVersion():Int{
        return Build.VERSION.SDK_INT
    }
    fun getHeaderString(): String {
        var sb = StringBuilder()
        sb.append("appVersionName").append("=").append(this.appVersionName).append(";")
                .append("osName").append("=").append(this.osName).append(";")
                .append("osVersion").append("=").append(this.osVersion).append(";")
                .append("manufacturer").append("=").append(this.manufacturer).append(";")
                .append("sdkVersion").append("=").append(this.sdkVersion).append(";")
                .append("model").append("=").append(this.model).append(";")
                .append("appPackageName").append("=").append(this.appPackageName).append(";")
        return sb.toString()
    }

    fun getAppIconAsIntId(context: Context):Int{
        val ai = context.applicationInfo
        return ai.icon
    }
}