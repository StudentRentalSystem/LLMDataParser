package io.github.studentrentalsystem;


import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;


/**
 * <p>
 * This class is used to call ollama AI model api. <br>
 * You can use these models such as llama3:8b, mistral, nomic_embed_text. <br>
 * @author hding4915
 */
public class LLMClient {
    private final LLMConfig config;
    private final Map<LLMConfig.LLMMode, Function<JSONObject, ?>> responseRules;

    public LLMClient(LLMConfig config, Map<LLMConfig.LLMMode, Function<JSONObject, ?>> responseRules) {
        this.config = config;
        this.responseRules = responseRules;
    }
    
    public LLMClient(LLMConfig config) {
        this(config, LLMAPIRules.getMixRules());
    }

    public LLMClient() {
        this(new LLMConfig(), LLMAPIRules.getMixRules());
    }

    public static class StreamData {
        public String token;
        public StringBuilder completeText;
        public boolean completed;
    }

    private void configRequestUrl() {
        String target = config.mode.getMode();
        config.requestUrl = config.serverAddress + ":" + config.serverPort + "/api/" + target;
    }

    public void setApiType(LLMConfig.LLMMode mode) {
        config.mode = mode;
        configRequestUrl();
    }

    public void setServerAddress(String serverAddress) {
        config.serverAddress = serverAddress;
        configRequestUrl();
    }

    public void setServerPort(int serverPort) {
        config.serverPort = serverPort;
        configRequestUrl();
    }

    public void setModelType(String modelType) {
        config.modelType = modelType;
    }

    public void setQueue(BlockingQueue<StreamData> queue) {
        config.queue = queue;
    }

    public void setStream(boolean stream) {
        config.stream = stream;
    }

    public String callLocalModel(String prompt, String model, LLMConfig.LLMMode mode, String serverAddress, int serverPort, boolean stream, BlockingQueue<StreamData> queue) {
        setApiType(mode);
        setServerAddress(serverAddress);
        setServerPort(serverPort);
        setModelType(model);
        setStream(stream);
        setQueue(queue);
        return callLocalModel(prompt, model, config.requestUrl, stream, queue);
    }

    public String callLocalModel(String prompt) {
        return callLocalModel(prompt, config.modelType, config.requestUrl, config.stream, config.queue);
    }


    public String getDetailMessage(String llmResponse) {
        JSONObject jsonResponse = new JSONObject(llmResponse);
        return (String) responseRules.get(config.mode).apply(jsonResponse);
    }

    @SuppressWarnings("unchecked")
    public List<Float> getEmbedMessage(String llmResponse) {
        JSONObject jsonResponse = new JSONObject(llmResponse);
        return (List<Float>) responseRules.get(config.mode).apply(jsonResponse);
    }


    /**
     * <p>
     * This function is used to call ollama model.
     * @param prompt something you want the AI to answer
     * @param model type of {@link String}, used to specify the model type
     * @param model_url the url of the running address. local server may use http://localhost:11434/api/generate
     * @param stream true if you want the AI to print the results sequentially
     * @param queue A thread-safe queue (typically a {@link java.util.concurrent.BlockingQueue}) that receives
     *              {@link StreamData} objects when streaming is enabled. Each object represents either a single
     *              token or the final completion status. If streaming is false, this parameter is ignored.
     * @return JSON String of ollama model response
     */
    public String callLocalModel(String prompt, String model, String model_url, boolean stream, BlockingQueue<StreamData> queue) {
        try {
            boolean mode = model_url.contains("/api/chat");

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            requestBody.put("stream", stream);

            if (!mode) {
                requestBody.put("prompt", prompt);
            } else {
                JSONArray jsonArray = new JSONArray();
                JSONObject oneRequest = new JSONObject();
                oneRequest.put("role", "user");
                oneRequest.put("content", prompt);
                jsonArray.put(oneRequest);

                requestBody.put("messages", jsonArray);
            }

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
                        String token;
                        if (mode) {
                            JSONObject message = responseBody.getJSONObject("message");
                            token = message.getString("content");
                        } else {
                            token = responseBody.getString("response");
                        }
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
     * @param model type of {@link String}, used to specify the model type
     * @param model_url the url of the running address. local server may use http://localhost:11434/api/generate
     * @return JSON String of ollama model response
     */
    public String callLocalModel(String prompt, String model, String model_url) {
        return callLocalModel(prompt, model, model_url, false, null);
    }

}
