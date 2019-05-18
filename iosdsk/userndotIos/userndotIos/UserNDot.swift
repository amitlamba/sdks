//
//  UserNDot.swift
//  userndotIos
//
//  Created by Jogender on 17/05/19.
//  Copyright © 2019 DigiCologies. All rights reserved.
//

import Foundation
import UIKit
import MapKit
import CoreData
import SQLite3
import SwiftyJSON

/*
 step 1 take token from there info.plist file   and save it as key value pair
 step 2 check identity present or not
 if not then first task is initialize and save
 else do nothing
 step 3
 
 */
public class UserNDot: NSObject,CLLocationManagerDelegate {
    
    var semaphore:DispatchSemaphore
    var heartBeat:Bool = false
    var undToken:String = ""
    let queue = DispatchQueue(label: "com.userndot.save",qos: .userInitiated)
    let sendQueue = DispatchQueue(label: "com.userndot.send",qos: .userInitiated)
    private var identity: Identity
    var locationManager: CLLocationManager
    let deviceInfo:DeviceInfo
    var db:OpaquePointer?
    var storageLocation:String = ""
    let threadSleepIntervalInMilli = 0.010
    let eventSendInterval:Int
    
    private  init(_ location:Bool = false,_ eventSendInterval:Int = 10) {
        
        identity = Identity()
        locationManager = CLLocationManager()
        deviceInfo = DeviceInfo()
        semaphore = DispatchSemaphore(value: 0)
        self.eventSendInterval = eventSendInterval
        //submit task initialize to new queue.
        super.init()
        locationManager.delegate = self
        self.undToken = Bundle.main.object(forInfoDictionaryKey: "UNDToken") as! String
        do {
            storageLocation = try FileManager.default.url(for: .cachesDirectory, in: .userDomainMask, appropriateFor: nil, create: false).appendingPathComponent("userndot.sqlite").path
            print(storageLocation)
        }catch{
            print("Error during storagelocation.")
        }
        if location {
            DispatchQueue.global().async {
                self.getLocation()
            }
            DispatchQueue.global().asyncAfter(deadline: DispatchTime(uptimeNanoseconds: 5000000), execute: {
                self.disableLocationService()
            })
        }
        initializeIdentity()
    }
    //builder pattern
    public static func getInstance() -> UserNDot? {
        let token = Bundle.main.object(forInfoDictionaryKey: "UNDToken")
        guard token != nil else {
            print("token not present")
            return nil
        }
        UserNDot.saveTokenToFileSystem(token: token as! String)
        return UserNDot()
    }
    public static func getInstance(location:Bool,eventSendInterval:Int) -> UserNDot? {
        let token = Bundle.main.object(forInfoDictionaryKey: "UNDToken")
        guard token != nil else {
            print("token not present")
            return nil
        }
        UserNDot.saveTokenToFileSystem(token: token as! String)
        return UserNDot(location,eventSendInterval)
    }
    public static func getInstance(location:Bool) -> UserNDot? {
        let token = Bundle.main.object(forInfoDictionaryKey: "UNDToken")
        guard token != nil else {
            print("token not present")
            return nil
        }
        UserNDot.saveTokenToFileSystem(token: token as! String)
        return UserNDot()
    }
    public static func test(){}
    
    private static func saveTokenToFileSystem(token:String){
        UserDefaults.standard.setValue(token, forKeyPath: "UNDToken")
    }
    private func initializeIdentity(){
        openDataBase()
        initializeDataBase()
        //UserDefaults.standard.removeObject(forKey: "und_identity")
        let decodedIdentity = UserDefaults.standard.data(forKey: "und_identity")
        if decodedIdentity == nil {
            print("value is nil")
            let encodedIdentity = try! JSONEncoder().encode(identity)
            UserDefaults.standard.set(encodedIdentity, forKey: "und_identity")
            getIdentity()
        }else{
            print("value is not nil")
            let storedIdentity = try! JSONDecoder().decode(Identity.self, from: decodedIdentity!)
            print(String(data: decodedIdentity!, encoding: .utf8))
            if storedIdentity.deviceId.isEmpty || storedIdentity.sessionId.isEmpty || storedIdentity.userId == nil {
                getIdentity()
            }else{
                identity = storedIdentity
                sendQueue.async {
                    self.sendQueue(dataModels: self.getAllDataFromLocalStorage())
                }
            }
        }
    }
    
    private func getIdentity(){
        do{
            let data = try JSONEncoder().encode(identity)
            let stringJson = JSON(data).rawString()!
            let dataModel = DataModel(id: -1,type: Type.IDENTITY.rawValue,data: stringJson)
            saveToLocalStorage(dataModel: dataModel)
        }catch{
            print("Error during identity marshalling")
        }
    }
    private func saveToLocalStorage(dataModel:DataModel,fromUserInteraction:Bool = false){
        // print(dataModel.getData)
        // NSData.base64EncodedString(dataModel.getData)
        
        let insertSql = "insert into data_model (type,data) values ('\(dataModel.getType)','\(dataModel.getData)')"
        let result = sqlite3_exec(db, insertSql, nil, nil, nil)
        if result == SQLITE_OK{
            print("Saved Successfully.")
            let changes =  sqlite3_changes(self.db)
            let id = sqlite3_last_insert_rowid(self.db)
            if fromUserInteraction {
                sendQueue.asyncAfter(deadline: .now() + .seconds(eventSendInterval), execute: {
                        self.sendQueue(dataModels: self.getAllDataFromLocalStorage())
                })
            }else{
                //schedule a task immediately
                sendQueue.asyncAfter(deadline: .now(), execute: {
                        self.sendQueue(dataModels: self.getAllDataFromLocalStorage())
                })
            }
//            sendQueue.async {
//                self.sendQueue(dataModels: self.getAllDataFromLocalStorage())
//            }
        }else{
            print("Error during save data model.")
            print(String(cString: sqlite3_errmsg(db)))
            print(String(cString: sqlite3_errstr(result)))
        }
    }
    
    private func getAllDataFromLocalStorage() -> Array<DataModel>{
        let selectSql = "select id,type,data from data_model order by id asc"
        var stmt :OpaquePointer? = nil
        var result = sqlite3_prepare_v2(self.db, selectSql, -1, &stmt, nil)
        var dataModels = Array<DataModel>()
        if result == SQLITE_OK{
            result = sqlite3_step(stmt)
            while result == SQLITE_ROW {
                let id = sqlite3_column_int64(stmt, 0)
                let type = String(cString: sqlite3_column_text(stmt, 1))
                let data = String(cString: sqlite3_column_text(stmt, 2))
                let dataModel = DataModel(id: id,type: type,data: data)
                dataModels.append(dataModel)
                result = sqlite3_step(stmt)
            }
            sqlite3_finalize(stmt)
        }else{
            print(String(cString: sqlite3_errstr(result)))
        }
        return dataModels
    }
    private func deleteFromLocalStorage(id:Int64){
        let deleteSql = "delete from data_model where id = \(id)"
        let result = sqlite3_exec(db, deleteSql, nil, nil, nil)
        if result == SQLITE_OK {
            print("delete \(sqlite3_changes(db)) for id \(id)")
        }else{
            print(String(cString: sqlite3_errstr(result)))
        }
    }
    private func initializeDataBase(){
        let initialize = "create table if not exists data_model (id integer primary key,type varchar(50),data varchar(1024))"
        let result = sqlite3_exec(db, initialize, nil, nil, nil)
        if result == SQLITE_OK {
            print("db initialized.")
        }else{
            print("Error duing table creation.")
        }
    }
    
    private func openDataBase(){
        var result = SQLITE_OK
        result = sqlite3_open(storageLocation, &self.db)
        if result == SQLITE_OK {
            print("Db open successfully.")
        }else{
            print("Error opening db.")
        }
    }
    private func closeDataBase(){
        var result = SQLITE_OK
        result = sqlite3_close(db)
        if result == SQLITE_OK {
            print("Db closed successfully.")
        }else{
            print("Error closing db.")
        }
    }
    
    func checkHeartBeat()->Bool{
        var urlRequest = URLRequest(url: URL(string: EndPoint.HEATBEAT.rawValue)!)
        urlRequest.httpMethod = "GET"
        urlRequest.addValue(undToken, forHTTPHeaderField: "Authorization")
        urlRequest.addValue("IOS", forHTTPHeaderField: "type")
        urlRequest.addValue("app.android.com", forHTTPHeaderField: "iosAppId")
        //NSURLConnection.sendSynchronousRequest(urlRequest, returning: nil)  //deprecated by apple
        var heartBeat = false
        let task = URLSession.shared.dataTask(with: urlRequest,completionHandler: {
            (data,response,error) in
            let nre = response as? HTTPURLResponse
            nre?.statusCode
            print("Response is \(response)")
            guard data != nil else{
                print(error?.localizedDescription)
                self.semaphore.signal()
                return
            }
            print(String(data: data!, encoding: .utf8))
            heartBeat = true
            self.semaphore.signal()
        })
        task.resume()
        //        repeat{
        //            print("waiting")
        //            Thread.sleep(forTimeInterval: TimeInterval(floatLiteral: threadSleepIntervalInMilli))
        //        }while !complete
        print("checking connectivity..")
        semaphore.wait()
        print("wait complete.")
        //sleep(1000)
        return heartBeat
    }
    
    private func sendQueue(dataModels:[DataModel]){
        print("size of \(dataModels.count)")
        if dataModels.count>0 && checkHeartBeat(){
            for data in dataModels {
                print("sending id \(data.getId)")
                do{
                    switch data.getType {
                    case Type.EVENT.rawValue:
                        let event = try JSONDecoder().decode(Event.self, from: data.getData.data(using: .utf8)!)
                        event.identity = identity
                        let newData = try JSONEncoder().encode(event)
                        print("sending event \(String(data: newData, encoding: .utf8))")
                        var urlRequest = getUrlRequest(method: "POST", url: URL(string: EndPoint.EVENT.rawValue)!)
                        urlRequest.httpBody = newData
                        getURLSession(id:data.getId,requestType:Type.EVENT.rawValue, urlRequest: urlRequest,processResponse: processResponse(id:requestType:data:response:error:))
                        break
                    case Type.EVENTUSER.rawValue:
                        let eventUser = try JSONDecoder().decode(EventUser.self, from: data.getData.data(using: .utf8)!)
                        eventUser.identity = identity
                        let newData = try JSONEncoder().encode(eventUser)
                        print("sending eventUser \(String(data: newData, encoding: .utf8))")
                        var urlRequest = getUrlRequest(method: "POST", url: URL(string: EndPoint.EVENTUSER.rawValue)!)
                        urlRequest.httpBody = newData
                        getURLSession(id:data.getId,requestType:Type.EVENTUSER.rawValue, urlRequest: urlRequest,processResponse: processResponse(id:requestType:data:response:error:))
                        break
                    case Type.IDENTITY.rawValue:
                        var urlRequest = getUrlRequest(method: "POST", url: URL(string: EndPoint.INITIALIZE.rawValue)!)
                        print("sending identity \(data.getData)")
                        urlRequest.httpBody = data.getData.data(using: .utf8)
                        getURLSession(id:data.getId,requestType:Type.IDENTITY.rawValue, urlRequest: urlRequest,processResponse: processResponse(id:requestType:data:response:error:))
                        break
                    case Type.TRACK.rawValue:
                        //"{\"mongoId\":\"${mongoId}\",\"clientId\":\"${clientId}\"}"
                        var urlRequest = getUrlRequest(method: "POST", url: URL(string: EndPoint.TRACK.rawValue)!)
                        urlRequest.httpBody = data.getData.data(using: .utf8)
                        
                        getURLSession(id:data.getId,requestType:Type.TRACK.rawValue, urlRequest: urlRequest,processResponse: processResponse(id:requestType:data:response:error:))
                        break
                    default:
                        break
                    }
                }catch{
                    break
                }
            }
        }else{
            print("Connectivity fail")
        }
    }
    
    private func processResponse(id:Int64,requestType:String,data:Data?,response:URLResponse?,error:Error?){
        
            if error == nil{
                print(String(bytes: data!, encoding: .utf8))
                deleteFromLocalStorage(id: id)
             if requestType == Type.EVENTUSER.rawValue || requestType == Type.IDENTITY.rawValue{
                let jsonObject = try! JSONSerialization.jsonObject(with: data!, options: []) as! [String:Any]
                let d = jsonObject["data"] as! [String:Any]
                let newIdentity = d["value"] as! [String:Any]
                let userId = newIdentity["userId"] as! String
                let sessionId = newIdentity["sessionId"] as! String
                let deviceId = newIdentity["deviceId"] as! String
                let clientId = newIdentity["clientId"] as! Int
                let idf = newIdentity["idf"] as! Int
                identity = Identity(deviceId: deviceId, sessionId: sessionId, userId: userId, clientId: clientId, idf: idf)
                let encodedIdentity = try! JSONEncoder().encode(identity)
                UserDefaults.standard.set(encodedIdentity, forKey: "und_identity")
            }
            }else{
                print("Error \(error?.localizedDescription)")
                //throw UserNDotError.runtimeError(error?.localizedDescription)
            }
        
        semaphore.signal()
    }
    func getUrlRequest(method:String,url:URL) -> URLRequest{
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = method
        urlRequest.addValue(undToken, forHTTPHeaderField: "Authorization")
        urlRequest.addValue("application/json", forHTTPHeaderField: "Content-Type")
        urlRequest.addValue("mobile", forHTTPHeaderField: "User-Agent")
        urlRequest.addValue("IOS", forHTTPHeaderField: "type")
        urlRequest.addValue("app.android.com", forHTTPHeaderField: "iosAppId")
        urlRequest.timeoutInterval = 10000
        urlRequest.addValue(deviceInfo.toString(), forHTTPHeaderField: "Mobile-Agent")
        return urlRequest
    }
    
    func getURLSession(id:Int64,requestType:String,urlRequest:URLRequest,processResponse:@escaping (Int64,String,Data?,URLResponse?,Error?)->Void) {
        var complete = false
        let urlSessionDataTask = URLSession.shared.dataTask(with: urlRequest, completionHandler: {
            (data,response,error) in
            complete = true
            processResponse(id,requestType,data,response,error)
        })
        urlSessionDataTask.resume()
        //        repeat{
        //            Thread.sleep(forTimeInterval: TimeInterval(floatLiteral: threadSleepIntervalInMilli))
        //        }while !complete
        semaphore.wait()
    }
   public func pushEvent(){
        let uevent = UEvent(name: "search")
        let event = Event()
        event.name = uevent.name
        event.attributes = JSON(uevent.attributes)
        //event.identity = identity
        event.creationTime = Int64(Date().timeIntervalSince1970)*1000
    
        let location = getLocation()
        event.latitude = String(location.lat!)
        event.longitude = String(location.long!)
        queue.async {
            self.saveEvent(event)
        }
    }
    
    
    
   public func pushProfile(){
        let ueventUser = UEventUser()
        ueventUser.firstName="jogi"
        let eventUser = EventUser()
        eventUser.additionalInfo = JSON(ueventUser.additionalInfo)
        eventUser.address = ueventUser.address
        eventUser.email = ueventUser.email
        eventUser.uid = ueventUser.uid
        eventUser.fbId = ueventUser.fbId
        eventUser.googleId = ueventUser.googleId
        eventUser.city = ueventUser.city
        eventUser.country = ueventUser.country
        eventUser.firstName = ueventUser.firstName
        eventUser.lastName = ueventUser.lastName
        eventUser.dob = ueventUser.dob
        eventUser.countryCode = ueventUser.countryCode
        eventUser.gender = ueventUser.gender
        eventUser.mobile = ueventUser.mobile
        //eventUser.identity = identity
        eventUser.creationDate = Int64(Date().timeIntervalSince1970)*1000
        eventUser.iosFcmToken = getIosToken()
        queue.async {
            self.saveProfileEvent(eventUser: eventUser)
        }
    }
    
    func saveEvent(_ event:Event){
        let data = try! JSONEncoder().encode(event)
            //JSON(event).rawString()!
        let rawData = String(data: data, encoding: .utf8)!
        let dataModel = DataModel(type: Type.EVENT.rawValue, data: rawData)
        saveToLocalStorage(dataModel: dataModel,fromUserInteraction: true)
    }
    
    func saveProfileEvent(eventUser:EventUser){
        let data = try! JSONEncoder().encode(eventUser)
        let rawData = String(data: data, encoding: .utf8)!
            //JSON(eventUser).rawString()!
        let dataModel = DataModel(type: Type.EVENTUSER.rawValue, data: rawData)
        saveToLocalStorage(dataModel: dataModel,fromUserInteraction: true)    }
    
    func saveInitializeEvent(identity:Identity){
        let data = try! JSONEncoder().encode(identity)
        //let rawData = JSON(identity).rawString()!
        let rawData = String(data: data, encoding: .utf8)!
        let dataModel = DataModel(type: Type.IDENTITY.rawValue, data: rawData)
        saveToLocalStorage(dataModel: dataModel,fromUserInteraction: true)    }
    
    public func logout(){
        identity.userId = nil
        identity.sessionId = ""
        identity.idf = 0
        let newIdentity = Identity(identity: identity)
        let encodedIdentity = try! JSONEncoder().encode(identity)
        UserDefaults.standard.set(encodedIdentity, forKey: "und_identity")
        queue.async {
            self.saveInitializeEvent(identity: newIdentity)
        }
    }
    
    func getIosToken() -> String {
        return "iostoken"
    }
    
    func getLocation() -> (lat:Double?,long:Double?){
        let location = locationManager.location
        let coordinate = location?.coordinate
        var lastLocationDate:Date? = location?.timestamp
        if lastLocationDate == nil {
            lastLocationDate = Date()
        }
        let d = Calendar.current
        let newDate = d.date(byAdding: .hour, value: 6, to: lastLocationDate!)
        let currentDate = Date()
        if(newDate!.compare(currentDate).rawValue >= 0){
            //get new location
            //start updating location.
            //in queue add stop updationg location but check accuracy also.
            setUpCoreLocation()
        }
        return (lat:coordinate?.latitude ?? 90,long:coordinate?.longitude ?? 90)
    }
    
    func setUpCoreLocation(){
        print(locationManager)
        switch CLLocationManager.authorizationStatus() {
        case .notDetermined:
            print("not determined")
            locationManager.requestAlwaysAuthorization()
            break
        case .authorizedAlways:
            print("authorize always")
            enableLocationService()
        case .authorizedWhenInUse:
            print("when in use")
            enableLocationService()
            break
        default:
            print("not known")
            break
        }
    }
    func enableLocationService(){
        if CLLocationManager.locationServicesEnabled(){
            print("updating location")
            locationManager.startUpdatingLocation()
        }else{
            print("location not enabled")
        }
    }
    //schedulae a task to stop location after 1 min.
    func disableLocationService(){
        locationManager.stopUpdatingLocation()
    }
    
    public func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        switch status {
        case .authorizedAlways:
            print("authorize")
        case .denied , .restricted:
            print("not authorize")
        default:
            print(status)
            break
        }
    }
    
    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        //let lastLocation = locations.last!
        //print("corrd \(lastLocation.coordinate)")
    }
    
    public func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        //print("faile")
    }
    
    
    deinit {
        //perfrom cleanup
        print("deinitialization...")
        closeDataBase()
        db = nil
    }
}


enum EndPoint : String {
    case EVENT = "http://192.168.0.109:5464/push/event",
    EVENTUSER = "http://192.168.0.109:5464/push/profile",
    INITIALIZE = "http://192.168.0.109:5464/event/initialize",
    HEATBEAT = "http://192.168.0.109:5464/check",
    TRACK = "http://192.168.0.109:5464/event/android/tracking"
}

enum Type : String {
    case IDENTITY = "identity",
    EVENTUSER = "eventUser",
    EVENT = "event",
    TRACK = "track"
}

enum UserNDotError: Error {
    case runtimeError(String)
}
