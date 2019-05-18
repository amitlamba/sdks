//
//  Model.swift
//  UserndotIosSdk
//
//  Created by Jogender on 06/05/19.
//  Copyright © 2019 DigiCologies. All rights reserved.
//

import Foundation

class Identity : Codable {
    var deviceId: String
    var sessionId: String
    var userId: String?
    var clientId: Int?
    var idf:Int
    
    init(deviceId:String,sessionId:String,userId:String?,clientId:Int,idf:Int) {
        self.deviceId = deviceId
        self.sessionId = sessionId
        self.userId = userId
        self.clientId = clientId
        self.idf = idf
    }
    
    init() {
        self.deviceId = ""
        self.sessionId = ""
        self.userId = nil
        self.clientId = nil
        self.idf = 0
    }
    init(identity:Identity) {
        self.deviceId = identity.deviceId
        self.sessionId = identity.sessionId
        self.userId = identity.userId
        self.clientId = identity.clientId
        self.idf = identity.idf
        
    }
//    enum CodingKeys: String , CodingKey {
//        case deviceId , sessionId , userId , clientId, idf
//    }
    
}

//extension Identity: Decodable {
//    init(from decoder: Decoder) throws {
//        let container = try decoder.container(keyedBy: CodingKeys.self)
//
//        deviceId = try container.decode(String.self, forKey: .deviceId)
//    }
//}
