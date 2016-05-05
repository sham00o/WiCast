//
//  SendSocket.swift
//  CS460
//
//  Created by Samuel Liu on 5/2/16.
//  Copyright © 2016 iSam. All rights reserved.
//

import Foundation
import CocoaAsyncSocket

class SendSocket: NSObject, GCDAsyncUdpSocketDelegate {
    
    let HOST_IP = "255.255.255.255"
    let PORT:UInt16 = 5555
    let HOST_PORT:UInt16 = 6666
    let TIMEOUT:NSTimeInterval = 2
    var socket:GCDAsyncUdpSocket!
    
    override init(){
        super.init()
        
        setupConnection()
    }
    
    func setupConnection(){
        socket = GCDAsyncUdpSocket(delegate: self, delegateQueue: dispatch_get_main_queue())
        do {
            try socket.bindToPort(PORT)
        } catch {
            print("Error connecting")
        }
        do {
            try socket.enableBroadcast(true)
        } catch {
            print("Error enabling broadcast")
        }
    }
    
    func close() {
        socket.close()
    }
    
    func send(message:String){
        let data = message.dataUsingEncoding(NSUTF8StringEncoding)
        socket.sendData(data, toHost: HOST_IP, port: HOST_PORT, withTimeout: TIMEOUT, tag: 0)
    }
    
    func udpSocket(sock: GCDAsyncUdpSocket!, didConnectToAddress address: NSData!) {
        print("didConnectToAddress");
    }
    
    func udpSocket(sock: GCDAsyncUdpSocket!, didNotConnect error: NSError!) {
        print("didNotConnect \(error)")
    }
    
    func udpSocket(sock: GCDAsyncUdpSocket!, didSendDataWithTag tag: Int) {
        print("didSendDataWithTag")
    }
    
    func udpSocket(sock: GCDAsyncUdpSocket!, didNotSendDataWithTag tag: Int, dueToError error: NSError!) {
        print("didNotSendDataWithTag \(error)")
    }

}