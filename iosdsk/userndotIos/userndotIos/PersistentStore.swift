//
//  PersistentStore.swift
//  userndotIos
//
//  Created by Jogender on 18/05/19.
//  Copyright © 2019 DigiCologies. All rights reserved.
//

import Foundation
import SQLite3

class PersistentStore {
    
    private var db:OpaquePointer?
    private var storageLocation:String = ""
    private let sendQueue:DispatchQueue
    private let eventSendInterval:Int
    
    init(eventSendInterval:Int,dispatchQueue:DispatchQueue) {
        self.eventSendInterval = eventSendInterval
        self.sendQueue = dispatchQueue
        do {
            storageLocation = try FileManager.default.url(for: .cachesDirectory, in: .userDomainMask, appropriateFor: nil, create: false).appendingPathComponent("userndot.sqlite").path
        }catch{
            print("Error during storagelocation.")
        }
        openDataBase()
        initializeDataBase()
    }
    
     func deleteFromLocalStorage(id:Int64){
        let deleteSql = "delete from data_model where id = \(id)"
        let result = sqlite3_exec(db, deleteSql, nil, nil, nil)
        if result == SQLITE_OK {
            print("delete \(sqlite3_changes(db)) for id \(id)")
        }else{
            print(String(cString: sqlite3_errstr(result)))
        }
    }
    private  func initializeDataBase(){
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
    func saveToLocalStorage(dataModel:DataModel,userNDot:UserNDot,fromUserInteraction:Bool = false){
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
                    userNDot.sendQueue(dataModels: self.getAllDataFromLocalStorage())
                })
            }else{
                //schedule a task immediately
                sendQueue.asyncAfter(deadline: .now(), execute: {
                    userNDot.sendQueue(dataModels: self.getAllDataFromLocalStorage())
                })
            }
            //we can do this also here we are sending as soon as event occur.
            //            sendQueue.async {
            //                    self.sendQueue(dataModels: self.getDataFromLocalStorageById(id:id))
            //            }
        }else{
            print("Error during save data model.")
            print(String(cString: sqlite3_errmsg(db)))
            print(String(cString: sqlite3_errstr(result)))
        }
    }
     func getAllDataFromLocalStorage() -> Array<DataModel>{
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
     func getDataFromLocalStorageById(id:Int64) -> Array<DataModel>{
        let selectSql = "select * from data_model where id = \(id)"
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
    deinit {
        closeDataBase()
    }
}
