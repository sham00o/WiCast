package org.sham00o.wicast;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {

    final int MAX_DATAGRAM_LEN = 1024;
    final int HOST_PORT = 6666;
    final int PORT = 5555;

    TextView mMessageView;
    Button mSendButton;
    ReceiveThread mReceiver;
    SendThread mSender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupConnection();

        mMessageView = (TextView) findViewById(R.id.messageView);
        mSendButton = (Button) findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSender.send("uhh.. hi?");
            }
        });
    }

    void setupConnection() {
        try {
            mReceiver = new ReceiveThread();
            mReceiver.start();
            mSender = new SendThread();
            mSender.start();
        } catch(Exception e) {
            Log.e("Socket", "Failed to setup sockets");
        }
    }

    private Runnable updateMessageView = new Runnable() {
        @Override
        public void run() {
            mMessageView.append("\n"+mReceiver.getLastMessage()+"\n");
        }
    };

    private class SendThread extends Thread {
        DatagramSocket socket;

        public void run() {
            socket = null;

            try {
                socket = new DatagramSocket(HOST_PORT);
                socket.setBroadcast(true);
                String udpMsg = "hello world to "+HOST_PORT;
                DatagramPacket dp;
                dp = new DatagramPacket(udpMsg.getBytes(), udpMsg.length(), getBroadcastAddress(), HOST_PORT);
                socket.send(dp);

            } catch(Throwable e) {
                e.printStackTrace();
            }
        }

        public void send(String msg) {
            try {
                DatagramPacket dp = new DatagramPacket(msg.getBytes(), msg.length(), getBroadcastAddress(), HOST_PORT);
                socket.send(dp);

            } catch(Throwable e) {
                e.printStackTrace();
            }
        }

        public void kill() {
            socket.close();
        }

        private InetAddress getBroadcastAddress() throws IOException {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            DhcpInfo dhcp = wifi.getDhcpInfo(); // handle null somehow

            int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
            byte[] quads = new byte[4];
            for (int k = 0; k < 4; k++)
                quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
            return InetAddress.getByAddress(quads);
        }
    }

    private class ReceiveThread extends Thread {
        private boolean bRun = true;
        private String lastMessage = "";

        public void run() {
            String message;
            byte[] buf = new byte[MAX_DATAGRAM_LEN];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            DatagramSocket socket = null;

            try {
                socket = new DatagramSocket(HOST_PORT);

                Log.i("Receive", "Listening on port "+HOST_PORT);
                while(bRun) {
                    Log.i("Receive", "Running..");
                    socket.receive(packet);
                    message = new String(buf, 0, packet.getLength());
                    Log.i("Receive", "Incoming message: "+message);
                    lastMessage = message;
                    runOnUiThread(updateMessageView);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }

            if (socket != null) {
                socket.close();
            }
        }

        public void kill() {
            bRun = false;
        }

        public String getLastMessage() {
            return lastMessage;
        }

    }
}
