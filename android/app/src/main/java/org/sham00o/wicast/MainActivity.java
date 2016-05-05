package org.sham00o.wicast;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {

    final int MAX_DATAGRAM_LEN = 1024;
    final int HOST_PORT = 6666;
    final int PORT = 5555;

    TextView mMessageView, mStatusView;
    EditText mMessageField, mNameField;
    Button mSendButton, mConnectButton;
    ReceiveThread mReceiver;
    SendThread mSender;

    boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStatusView = (TextView) findViewById(R.id.statusView);
        mMessageView = (TextView) findViewById(R.id.messageView);
        mMessageView.setText("");

        mNameField = (EditText) findViewById(R.id.nameField);
        mMessageField = (EditText) findViewById(R.id.messageField);
        mMessageField.clearFocus();

        mConnectButton = (Button) findViewById(R.id.connectButton);
        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isConnected) {
                    if(!setupConnection()){
                        Toast.makeText(getApplicationContext(), "Cannot connect. Make sure you are on WiFi", Toast.LENGTH_LONG).show();
                        return;
                    }
                    mConnectButton.setText("Disconnect");
                    mStatusView.setText("STATUS: connected");
                    isConnected = true;
                } else {
                    mReceiver.kill();
                    mSender.kill();
                    mConnectButton.setText("Connect");
                    mStatusView.setText("STATUS: disconnected");
                    isConnected = false;
                }
            }
        });
        mSendButton = (Button) findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isConnected) {
                    Toast.makeText(getApplicationContext(), "Not connected. Tap the connect button", Toast.LENGTH_LONG).show();
                    return;
                }
                String name = mNameField.getText().toString();
                String message = mMessageField.getText().toString();
                mSender.send(name+": "+message);
                mMessageField.setText("");
            }
        });
    }

    boolean setupConnection() {
        try {
            mReceiver = new ReceiveThread();
            mReceiver.start();
            mSender = new SendThread();
            mSender.start();
            return true;
        } catch(Exception e) {
            Log.e("Socket", "Failed to setup sockets");
            return false;
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
                socket = new DatagramSocket(PORT);
                socket.setBroadcast(true);

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
            if(socket != null)
                socket.close();
        }

        private InetAddress getBroadcastAddress() throws IOException {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            DhcpInfo dhcp = wifi.getDhcpInfo();

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
