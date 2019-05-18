//
//  EventUser.swift
//  UserndotIosSdk
//
//  Created by Jogender on 06/05/19.
//  Copyright © 2019 DigiCologies. All rights reserved.
//

import Foundation
import SwiftyJSON

class EventUser: Codable {
    
    var identity: Identity = Identity()
    var androidFcmToken:String? = nil
    var iosFcmToken:String? = nil
    var webFcmToken:String? = nil
    var email: String? = nil
    var uid: String? = nil //this is id of the user client has provided
    var undId: String? = nil
    var fbId: String? = nil
    var googleId: String? = nil
    var mobile: String? = nil
    var firstName: String? = nil
    var lastName: String? = nil
    var gender: String? = nil
    var dob: String? = nil
    var country: String? = nil
    var city: String? = nil
    var address: String? = nil
    var countryCode: String? = nil
    var additionalInfo: JSON
    //var additionalInfo: [String:String] = [:]
    var creationDate:Int64? = nil
    init() {
        additionalInfo = JSON(Dictionary<String,Any>())
    }
}

class UEventUser {
    var email: String? = nil
    var uid: String? = nil //this is id of the user client has provided
    var fbId: String? = nil
    var googleId: String? = nil
    var mobile: String? = nil
    var firstName: String? = nil
    var lastName: String? = nil
    var gender: String? = nil
    var dob: String? = nil
    var country: String? = nil
    var city: String? = nil
    var address: String? = nil
    var countryCode: String? = nil
    var additionalInfo: [String:Any] = [:]
    
    init() {
    }
}
//struct Customer: Codable {
//    let id: String
//    let email: String
//    let metadata: [String: Any]
//
//    enum CustomerKeys: String, CodingKey
//    {
//        case id, email, metadata
//    }
//
//    init (from decoder: Decoder) throws {
//        let container =  try decoder.container (keyedBy: CustomerKeys.self)
//        id = try container.decode (String.self, forKey: .id)
//        email = try container.decode (String.self, forKey: .email)
//        metadata = try container.decode ([String: Any].self, forKey: .metadata)
//        //container.decode([String:Any].self, forKey: Customer.CustomerKeys.metadata)
//
//    }
//
//    func encode (to encoder: Encoder) throws
//    {
//        var container = encoder.container (keyedBy: CustomerKeys.self)
//        try container.encode (id, forKey: .id)
//        try container.encode (email, forKey: .email)
//        try container.encode (metadata, forKey: .metadata)
//    }
//}
