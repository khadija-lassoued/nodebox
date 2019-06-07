package prtocol;

import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import tn.insat.nodebox.Device;

public class DeviceConfigProtocol{

    private static int port = 7805;
    private Socket socket;

    private Runnable runnable;

    private String response;
    private BufferedReader bufferedReader;

    private String IP;
    private Device device;

    public DeviceConfigProtocol(){
        this.device = new Device();
    }


    public void helloMessage(String IP) {
        try {
            Log.d("config_dev", "begin test " + IP);
            //send hello message
            sendMessage(IP,"hello who are you?");
            Log.d("config_dev", "waiting for resp");
            //receive param response : iot,name,topic,state
            response = receiveMessage(IP);
            Log.d("config_dev", response);
            if (response.contains("iot")) {
                String[] info = response.split(",");
                device.setName(info[0]);
                device.setState(info[1]);
                device.setTopic(info[2]);
                device.setIP(IP);
                if (info[4].equalsIgnoreCase("true")) {
                    device.setIsDevice(true);
                } else {
                    device.setIsDevice(false);
                }
                Log.d("config_dev", "OK");
            } else {
                Log.d("config_dev", IP + "is not IOT");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void configurationMessage(String IP,final String ssid, final String password) {
        try {
            //send config message
            sendMessage(IP,ssid + "," + password);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String IP,String message) throws IOException {
        //open a new socket
        socket = new Socket(IP, port);
        //send  message
        PrintWriter pw = new PrintWriter((socket.getOutputStream()));
        pw.write(message);
        pw.flush();
        pw.close();
        socket.close();
    }

    public String receiveMessage(String IP) throws IOException {
        //accept message
        socket = new Socket(IP, port);
        //get response
        InputStreamReader isr = new InputStreamReader(socket.getInputStream());
        bufferedReader = new BufferedReader(isr);
        return (bufferedReader.readLine());
    }

    public Device getDevice() {
        return device;
    }
}