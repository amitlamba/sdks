//
//  Event.swift
//  UserndotIosSdk
//
//  Created by Jogender on 06/05/19.
//  Copyright © 2019 DigiCologies. All rights reserved.
//

import Foundation
import SwiftyJSON

class Event:Codable {
    
    var name: String? = nil
    var identity: Identity = Identity()
    var creationTime: Int64? = nil
    var ipAddress: String? = nil
    var city: String? = nil
    var state: String? = nil
    var country: String? = nil
    var latitude: String? = nil
    var longitude: String? = nil
    var agentString: String? = nil
    var userIdentified: Bool = false
    var lineItem: [LineItem] = []
    var attributes: JSON
    //var attributes: [String:String] = [:]

    init() {
        attributes = JSON(Dictionary<String,Any>())
    }
    
}

class LineItem :Codable {
    
    var price: Int = 0
    var currency: String? = nil
    var product: String? = nil
    var categories: [String] = []
    var tags: [String] = []
    var quantity: Int = 0
    var properties: JSON
    //var properties: [String:String] = [:]
    init() {
        properties = JSON(Dictionary<String,Any>())
    }
}


class UEvent {
    var name:String;
    var attributes:[String:Any] ;
    convenience init(name:String) {
        self.init(name: name,attributes: [:])
    }
    init(name:String,attributes:[String:Any]) {
        self.name=name
        self.attributes=attributes
    }
}
