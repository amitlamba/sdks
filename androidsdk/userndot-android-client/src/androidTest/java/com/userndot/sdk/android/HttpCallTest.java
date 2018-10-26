package com.userndot.sdk.android;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class HttpCallTest {

    //http v1
    public HttpURLConnection getConnection() throws Exception {
        URL url = new URL("POST https://fcm.googleapis.com/v1/projects/myproject-b5ae1/messages:send");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type", "application/json; UTF-8");
        connection.setRequestProperty("Authorization", "Bearer" + getAccessToken());
        connection.connect();
        return connection;
    }
    public String getAccessToken() throws IOException {
        GoogleCredential credential=
        GoogleCredential.fromStream(new FileInputStream("")).createScoped(Arrays.asList("https://www.googleapis.com/auth/firebase.messaging"));
        credential.refreshToken();
        return credential.getAccessToken();
    }
    //http
    public void sendMessageByLegacyHttp() throws Exception{
        URL url = new URL("POST https://fcm.googleapis.com/fcm/send");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type", "application/json; UTF-8");
        connection.setRequestProperty("Authorization", "key=" + "serverkey");
        connection.connect();
    }

    private  void sendMessage(JSONObject fcmMessage) throws Exception {
        HttpURLConnection connection = getConnection();
        connection.setDoOutput(true);
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes(fcmMessage.toString());
        outputStream.flush();
        outputStream.close();

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
//            String response = inputstreamToString(connection.getInputStream());
            System.out.println("Message sent to Firebase for delivery, response:");
//            System.out.println(response);
        } else {
            System.out.println("Unable to send message to Firebase:");
//            String response = inputstreamToString(connection.getErrorStream());
//            System.out.println(response);
        }
    }

    public void sendMessage(List<String> tokens, JsonNode message){
        for (String token: tokens) {
            //here set token in message
            //take connection
            //write data in outputstream
            //check response

            //alternate way

            //create a group of 20-20 user and send to then later remove ll user from group then group delete automatically
        }
    }
}