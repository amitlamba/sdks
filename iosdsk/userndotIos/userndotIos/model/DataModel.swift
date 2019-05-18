//
//  Data.swift
//  UserndotIosSdk
//
//  Created by Jogender on 07/05/19.
//  Copyright © 2019 DigiCologies. All rights reserved.
//

import Foundation
import  CoreData
//class DataModel : NSManagedObject {
//    @NSManaged var id: NSManagedObjectID
//    @NSManaged var data: String
//}

class DataModel {
    private let id:Int64
    private let type:String
    private let data:String
    
    var getId : Int64{
        return id
    }
    
    var getType : String {
        get {
            return type
        }
    }
    var getData : String {
        get{
            return data
        }
    }
    
    init(id:Int64 = -1,type:String,data:String) {
        self.id = id
        self.type = type
        self.data = data
    }
}
