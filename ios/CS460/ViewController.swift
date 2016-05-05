//
//  ViewController.swift
//  CS460
//
//  Created by Samuel Liu on 4/20/16.
//  Copyright © 2016 iSam. All rights reserved.
//

import UIKit
import CocoaAsyncSocket

class ViewController: UIViewController, GCDAsyncUdpSocketDelegate, GCDAsyncSocketDelegate, ReceiveDelegate {
    
    @IBOutlet weak var status : UILabel!
    @IBOutlet weak var nameField: UITextField!
    @IBOutlet weak var messageView: UITextView!
    @IBOutlet weak var messageField: UITextField!
    
    var outSocket : SendSocket!
    var inSocket : ReceiveSocket!
    
    var animationDuration : Double!
    var keyboardHeight : CGFloat!
    var alertDisplayed = false
    var isConnected = false
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Initialize message log content
        messageView.text = ""
        
        // Set up keyboard show/hide listeners to trigger local functions
        NSNotificationCenter.defaultCenter().addObserver(self, selector: #selector(ViewController.keyboardWillShow(_:)), name: UIKeyboardWillShowNotification, object: nil)
        NSNotificationCenter.defaultCenter().addObserver(self, selector: #selector(ViewController.keyboardWillHide(_:)), name: UIKeyboardWillHideNotification, object: nil)
    }
    
    // Initialize in and out sockets
    func setupConnection(){
        outSocket = SendSocket()
        inSocket = ReceiveSocket()
        inSocket.delegate = self
    }
    
    // Trigger alert to user
    func alert(title: String, msg: String) {
        let alert = UIAlertController(title: title, message: msg, preferredStyle: .Alert)
        alert.addAction(UIAlertAction(title: "Okay", style: .Default, handler: nil))
        self.presentViewController(alert, animated: true, completion: nil)
    }
    
    // Handle connect/disconnect events on button press
    @IBAction func connect(sender: UIButton) {
        if !isConnected {
            print("Wifi: \(getWiFiAddress())")
            if getWiFiAddress() == nil {
                alert("No WiFi Detected", msg: "Connect to a WiFi network first")
                return
            }
            setupConnection()
            status.text = "STATUS: connected"
            sender.setTitle("Disconnect", forState: .Normal)
            isConnected = true
        } else {
            outSocket.close()
            inSocket.close()
            status.text = "STATUS: disconnected"
            sender.setTitle("Connect", forState: .Normal)
            isConnected = false
        }
    }
    
    // Handle broadcast event to send message through SendSocket
    @IBAction func broadcast(sender: UIButton) {
        if !isConnected {
            alert("Not connected", msg: "Tap the 'connect' button below")
            return
        }
        if messageField.text == "" {
            alert("Empty field", msg: "Type something in order to broadcast")
            return
        }
        let username = nameField.text == "" ? "Anonymous" : nameField.text!
        let message = messageField.text!
        outSocket.send("\(username): \(message)")
        messageField.text = ""
    }
    
    func getWiFiAddress() -> String? {
        var address : String?
        
        // Get list of all interfaces on the local machine:
        var ifaddr : UnsafeMutablePointer<ifaddrs> = nil
        if getifaddrs(&ifaddr) == 0 {
            
            // For each interface ...
            var ptr = ifaddr
            while ptr != nil {
                defer { ptr = ptr.memory.ifa_next }
                
                let interface = ptr.memory
                
                // Check for IPv4 or IPv6 interface:
                let addrFamily = interface.ifa_addr.memory.sa_family
                if addrFamily == UInt8(AF_INET) || addrFamily == UInt8(AF_INET6) {
                    
                    // Check interface name:
                    if let name = String.fromCString(interface.ifa_name) where name == "en0" {
                        
                        // Convert interface address to a human readable string:
                        var addr = interface.ifa_addr.memory
                        var hostname = [CChar](count: Int(NI_MAXHOST), repeatedValue: 0)
                        getnameinfo(&addr, socklen_t(interface.ifa_addr.memory.sa_len),
                                    &hostname, socklen_t(hostname.count),
                                    nil, socklen_t(0), NI_NUMERICHOST)
                        address = String.fromCString(hostname)
                    }
                }
            }
            freeifaddrs(ifaddr)
        }
        
        return address
    }
    
    // Helper function for keyboard methods
    func getKeyboardHeight(notification: NSNotification) {
        let beginRect = notification.userInfo![UIKeyboardFrameBeginUserInfoKey]?.CGRectValue()
        let endRect = notification.userInfo![UIKeyboardFrameEndUserInfoKey]?.CGRectValue()
        
        keyboardHeight = abs(beginRect!.origin.y - endRect!.origin.y)
        animationDuration = notification.userInfo![UIKeyboardAnimationDurationUserInfoKey]?.doubleValue
    }
    
    // MARK: ReceiveSocket delegate methods
    
    func didReceiveMessage(msg: NSString) {
        messageView.text = messageView.text+"\n\(msg)\n"
    }
    
    // MARK: Keyboard delegate methods
    
    func keyboardWillShow(notification: NSNotification) {
        getKeyboardHeight(notification)
        // shrink messageView up the distance of the keyboards height
        var viewFrame = view.frame
        viewFrame.size.height -= keyboardHeight
        
        UIView.animateWithDuration(animationDuration, animations: {
            self.view.frame = viewFrame
        })
    }
    
    func keyboardWillHide(notification: NSNotification) {
        getKeyboardHeight(notification)
        // grow messageView back to normal
        var viewFrame = view.frame
        viewFrame.size.height += keyboardHeight
        
        UIView.animateWithDuration(animationDuration, animations: {
            self.view.frame = viewFrame
        })
    }

}

