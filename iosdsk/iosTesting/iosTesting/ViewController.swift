//
//  ViewController.swift
//  iosTesting
//
//  Created by Jogender on 17/05/19.
//  Copyright © 2019 DigiCologies. All rights reserved.
//


import UIKit
import userndotIos

class ViewController: UIViewController {

    public var userndot :UserNDot?
    public var i = 0
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.
        userndot = UserNDot.getInstance()
        print(userndot)
    }

    @IBAction func logout(_ sender: UIButton) {
        userndot?.logout()
    }
    @IBAction func pushProfile(_ sender: UIButton) {
        userndot?.pushProfile()
    }
    @IBAction func pushEvent(_ sender: UIButton) {
        userndot?.pushEvent(name:"search\(i)")
        i+=1;
    }
}

