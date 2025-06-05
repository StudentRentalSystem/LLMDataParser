package io.github.studentrentalsystem;


import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;


/**
 * <p>
 * This class is used to call ollama AI model api. <br>
 * You can use these models such as llama3:8b, mistral, nomic_embed_text. <br>
 * @author hding4915
 */
public final class LLMClient {
    public enum ModelType {
        LLAMA3_8B("llama3:8b"), MISTRAL("mistral"), NOMIC_EMBED_TEXT("nomic_embed_text");

        private final String modelName;

        ModelType(String modelName) {
            this.modelName = modelName;
        }

        public String getModelName() {
            return modelName;
        }
    }

    public static class StreamData {
        public String token;
        public StringBuilder completeText;
        public boolean completed;
    }



    /**
     * <p>
     * This function is used to call ollama model.
     * @param prompt something you want the AI to answer
     * @param model type of {@link ModelType}, used to specify the model type
     * @param model_url the url of the running address. local server may use http://localhost:11434/api/generate
     * @param stream true if you want the AI to print the results sequentially
     * @param queue A thread-safe queue (typically a {@link java.util.concurrent.BlockingQueue}) that receives
     *              {@link StreamData} objects when streaming is enabled. Each object represents either a single
     *              token or the final completion status. If streaming is false, this parameter is ignored.
     * @return JSON String of ollama model response
     */
    public static String callLocalModel(String prompt, ModelType model, String model_url, boolean stream, BlockingQueue<StreamData> queue) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model.getModelName());
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
                StringBuilder allText = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                    if (stream) {
                        JSONObject responseBody = new JSONObject(line);

                        if (responseBody.getBoolean("done")) {
                            StreamData doneData = new StreamData();
                            doneData.completed = true;
                            doneData.completeText = allText;
                            queue.put(doneData);
                            break;
                        }
                        String token = responseBody.getString("response");
                        allText.append(token);


                        StreamData tokenData = new StreamData();
                        tokenData.token = token;
                        tokenData.completed = false;
                        tokenData.completeText = null;
                        queue.put(tokenData);
                    }
                }
            }

            return response.toString();
        } catch (Exception e) {
            System.out.println("⚠️ 發生錯誤：" + e.getMessage());
            return "";
        }
    }

    /**
     * <p>
     * This function is used to call ollama model with stream specified to false. <br>
     * @param prompt something you want the AI to answer
     * @param model type of {@link ModelType}, used to specify the model type
     * @param model_url the url of the running address. local server may use http://localhost:11434/api/generate
     * @return JSON String of ollama model response
     */
    public static String callLocalModel(String prompt, ModelType model, String model_url) {
        return callLocalModel(prompt, model, model_url, false, null);
    }

}
