package com.lms.tenant;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class AnalyzeImg {

    public static void main(String[] args) throws IOException {

        String endpoint = "https://cv-app-demo-333.cognitiveservices.azure.com/";
        String key = "5MKMBpLs04HqkZk9GVoX1MDuMrlx2xCMuxBeL3I04C8JVOTp4xQUJQQJ99CDACGhslBXJ3w3AAAFACOG7lIn";
        String imageUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQWfsR6cgnaq7nMEzpl-mjoAfMJ3gCGzUhq5A&s";

        //URL url = new URL(endpoint + "vision/v3.2/analyze?visualFeatures=Description");
        URL url = new URL(endpoint + "vision/v3.2/analyze?visualFeatures=Categories");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Ocp-Apim-Subscription-Key", key);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String body = "{\"url\":\"" + imageUrl + "\"}";
        OutputStream os = conn.getOutputStream();
        os.write(body.getBytes());

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }


    }
}
