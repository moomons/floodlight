
package net.floodlightcontroller.monsmodule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
//import java.net.HttpURLConnection;
//import java.net.MalformedURLException;
//import java.net.URL;

/**
 * Created by mo on 3/13/16.
 *
 * Latest updated on 4/21/16
 */
public class FLReport {

    private static String ServerIP = "192.168.109.213";
    private static int ServerPort = 7999;
    private static int timeout = 2000; // in ms

    public static void doPing() {
        boolean portAvailable = false;
        int timeout = 200;
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ServerIP, ServerPort), timeout);
            portAvailable = socket.isConnected();
            socket.close();
        } catch (UnknownHostException uhe) {
            uhe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        System.out.println("Connection possible: " + portAvailable);
    }

    public static void doPost(int srcPort, String payload, String srcIP) {

        try {
            URL url = new URL("http://" + ServerIP + ":" + Integer.toString(ServerPort));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            // payload like this: job=job_1461107404635_0002&reduce=0&map=attempt_1461107404635_0002_m_000014_0

            // Parse the payload and send
            String kwjob = "job=";
            int locstart = payload.indexOf(kwjob);
            String parajob = payload.substring(locstart + kwjob.length(), payload.indexOf('&', locstart));

            String kwreduce = "reduce=";
            locstart = payload.indexOf(kwreduce);
            String parareduce = payload.substring(locstart + kwreduce.length(), payload.indexOf('&', locstart));

            String kwmap = "map=";
            locstart = payload.indexOf(kwmap);
            String paramap = payload.substring(locstart + kwmap.length());

//            String input = "{" +
//                    "\"srcPort\": " + srcPort + ", " +
//                    parsedPayload +
//                    "}";

            String input = "{" +
                    "\"srcPort\": " + srcPort + ", " +
                    "\"para_job\": \"" + parajob + "\", " +
                    "\"para_map\": \"" + paramap + "\", " +
                    "\"ip_dst\": \"" + srcIP + "\", " +
                    "\"para_reduce\": " + parareduce + ", " +
                    "}";

            OutputStream os = conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("POST failed: HTTP code " + conn.getResponseCode());
            }
            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            String output;
            System.out.println("Output from Server:");
            while ((output = br.readLine()) != null) {
                System.out.println(output);
            }
            conn.disconnect();
        } catch (SocketTimeoutException e) {
            System.out.println("Timeout.");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
