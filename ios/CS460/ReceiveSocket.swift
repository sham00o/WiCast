//
//  ReceiveSocket.swift
//  CS460
//
//  Created by Samuel Liu on 5/2/16.
//  Copyright © 2016 iSam. All rights reserved.
//

import Foundation
import CocoaAsyncSocket

protocol ReceiveDelegate {
    func didReceiveMessage(msg: NSString)
}

class ReceiveSocket: NSObject, GCDAsyncUdpSocketDelegate {
    
    var delegate : ReceiveDelegate?
    
    //let IP = "255.255.255.255"
    let PORT:UInt16 = 6666
    var socket:GCDAsyncUdpSocket!
    
    override init(){
        super.init()
        setupConnection()
    }
    
    func setupConnection(){
        socket = GCDAsyncUdpSocket(delegate: self, delegateQueue: dispatch_get_main_queue())
        socket.setIPv4Enabled(true)
        socket.setIPv6Enabled(false)
        do {
            try socket.bindToPort(PORT)
        } catch {
            print("Bind error")
        }
        do {
            try socket.beginReceiving()
        } catch {
            print("Begin error")
        }
    }
    
    func close() {
        socket.close()
    }
    
    func udpSocket(sock: GCDAsyncUdpSocket!, didReceiveData data: NSData!, fromAddress address: NSData!,      withFilterContext filterContext: AnyObject!) {
        let message = NSString(data: data, encoding: NSUTF8StringEncoding)
        let addr = NSString(data: address, encoding: NSUTF8StringEncoding)
        delegate?.didReceiveMessage(message!)
        print("incoming message: \(message) from \(addr)")
    }
    
}