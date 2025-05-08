package com.studentrental;


import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;


public final class LLMClient {

    public static String callLocalModel(String prompt, String model, String model_url, boolean stream) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", stream);

            URL url = new URL(model_url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            StringBuilder response;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    if (!stream) {
                        response.append(line.trim());
                    } else {
                        JSONObject responseBody = new JSONObject(line);

                        if (responseBody.getBoolean("done")) {
                            System.out.println();
                            break;
                        }
                        String token = responseBody.getString("response");
                        response.append(token);

                        System.out.print(token);
                    }
                }
            }

            return response.toString();
        } catch (Exception e) {
            System.out.println("⚠️ 發生錯誤：" + e.getMessage());
            return "";
        }
    }

    public static String callLocalModel(String prompt, String model, String model_url) {
        return callLocalModel(prompt, model, model_url, false);
    }

}
