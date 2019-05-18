//
//  DeviceInfo.swift
//  UserndotIosSdk
//
//  Created by Jogender on 07/05/19.
//  Copyright © 2019 DigiCologies. All rights reserved.
//

import Foundation
import UIKit

class DeviceInfo: NSObject {
    private  var appVersionName:String? = nil    // CFBundleShortVersionString
    private  var osName:String? = nil          //ios
    private  var osVersion:String? = nil       // ios 10 , 12
    private  var manufacturer:String? = nil    //Apple
    private  var model:String? = nil            //iphone 6,7   name
    private  var appPackageName:String? = nil    //Bundle.main.bundleIdentifier
    private  var sdkVersion:Int? = nil            // ios 10 ,12   systemversion
    
    override init() {
        super.init()
        appVersionName = setAppVersionName()
        osName = setOsName()
        osVersion = setOsVersion()
        manufacturer = setManufacturer()
        model = setModel()
        appPackageName = setAppPackageName()
        sdkVersion = setSdkVersion()
    }
    
    private func setAppVersionName() -> String{
        return Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as! String
    }
    private func setOsName() -> String?{
        return "Ios"
    }
    private func setOsVersion() -> String?{
        return UIDevice.current.systemVersion
    }
    private func setManufacturer() -> String?{
        return "Apple"
    }
    private func setModel() -> String?{
        return UIDevice.current.name
    }
    private func setAppPackageName() -> String?{
        return Bundle.main.bundleIdentifier
    }
    
    public func getAppPackageName() -> String {
        return self.appPackageName ?? ""
    }
    private func setSdkVersion() -> Int{
        guard let value = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") else {
            return 1
        }
        print(value)
        return 1
    }
    
    func toString() -> String {
    return "Mobile-Agent/ \(self.osName)/ \(self.osVersion) /App Embedded Browser/1/Mobile/ \(self.model)/ \(self.appPackageName)/\(self.appVersionName)/ \(self.manufacturer)/ \(self.sdkVersion)"
    }
    
}

