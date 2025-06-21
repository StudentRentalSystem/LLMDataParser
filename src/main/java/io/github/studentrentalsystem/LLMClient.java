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
    private LLMConfig config;
    private final Map<LLMConfig.LLMMode, Function<JSONObject, ?>> responseRules;

    public LLMClient(LLMConfig config, Map<LLMConfig.LLMMode, Function<JSONObject, ?>> responseRules) {
        this.config = config;
        this.responseRules = responseRules;
    }
    
    public LLMClient(LLMConfig config) {
        this(config, LLMAPIRules.getReceiveRules());
    }

    public LLMClient() {
        this(new LLMConfig(), LLMAPIRules.getReceiveRules());
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

    public String getDetailMessage(String llmResponse) {
        JSONObject jsonResponse = new JSONObject(llmResponse);
        return (String) responseRules.get(config.mode).apply(jsonResponse);
    }

    @SuppressWarnings("unchecked")
    public List<Float> getEmbedMessage(String llmResponse) {
        JSONObject jsonResponse = new JSONObject(llmResponse);
        return (List<Float>) responseRules.get(config.mode).apply(jsonResponse);
    }

    /***
     * <p>
     * Set new llm config and override the original llm config
     * @param config ollama model configuration
     * @see LLMConfig
     */
    public void setLLMConfig(LLMConfig config) {
        this.config = config;
    }

    /***
     * <p>
     * Call ollama model api with the settings in the {@link LLMConfig}
     * @param prompt something you want the AI to answer
     * @return JSON String of ollama model response
     * Internal implementation: see private overloaded method for details.
     */
    public String callLocalModel(String prompt) {
        return callLocalModel(prompt, config.mode, config.modelType, config.requestUrl, config.stream, config.queue);
    }


    /**
     * <p>
     * This function is used to call ollama model.
     * @param prompt something you want the AI to answer
     * @param mode {@link io.github.studentrentalsystem.LLMConfig.LLMMode} AI model mode
     * @param model_url the url of the running address. local server may use http://localhost:11434/api/generate
     * @param model type of {@link String}, used to specify the model type
     * @param stream true if you want the AI to print the results sequentially
     * @param queue A thread-safe queue (typically a {@link java.util.concurrent.BlockingQueue}) that receives
     *              {@link StreamData} objects when streaming is enabled. Each object represents either a single
     *              token or the final completion status. If streaming is false, this parameter is ignored.
     * @return JSON String of ollama model response
     */
    private String callLocalModel(String prompt, LLMConfig.LLMMode mode, String model_url, String model, boolean stream, BlockingQueue<StreamData> queue) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            requestBody.put("stream", stream);

            if (mode == LLMConfig.LLMMode.CHAT) {
                JSONArray jsonArray = new JSONArray();
                JSONObject oneRequest = new JSONObject();
                oneRequest.put("role", "user");
                oneRequest.put("content", prompt);
                jsonArray.put(oneRequest);

                requestBody.put("messages", jsonArray);
            } else {
                requestBody.put("prompt", prompt);
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

                        String token = getDetailMessage(line);

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
}
