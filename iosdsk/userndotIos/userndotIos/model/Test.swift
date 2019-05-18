//
//  Test.swift
//  UserndotIosSdk
//
//  Created by Jogender on 16/05/19.
//  Copyright © 2019 DigiCologies. All rights reserved.
//

import Foundation
import SwiftyJSON

public class Test:Codable{
    
    var name:String=""
    var attributes:JSON
    var lineItem :[LineItem1] = []
    init() {
        attributes = JSON(Dictionary<String,Any>())
    }
}

class LineItem1: Codable {
    var attributes:JSON
    var name:String = ""
    init() {
        attributes = JSON(Dictionary<String,Any>())
    }
}
