package io.github.studentrentalsystem;


import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * <p>
 * This class is used to call ollama AI model api. <br>
 * You can use these models such as llama3:8b, mistral, nomic_embed_text. <br>
 * @author hding4915
 */
public final class LLMClient {
    private static boolean _chatMode = false;
    private static String _serverAddress = "http://localhost";
    private static int _serverPort = 11434;
    private static String _requestURL = _serverAddress + ":" + _serverPort + "/api/generate";
    private static ModelType _modelType = ModelType.LLAMA3_8B;
    private static boolean _stream = false;
    private static BlockingQueue<StreamData> _queue = null;

    public enum ModelType {
        LLAMA3_8B("llama3:8b"),
        MISTRAL("mistral"),
        NOMIC_EMBED_TEXT("nomic_embed_text"),
        LLAVA("llava"),
        LLAVA_13B("llava:13b");

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

    private static void configRequestUrl() {
        String target = _chatMode ? "chat" : "generate";
        _requestURL = _serverAddress + ":" + _serverPort + "/api/" + target;
    }

    public static void setApiType(boolean chatMode) {
        _chatMode = chatMode;
        configRequestUrl();
    }

    public static void setServerAddress(String serverAddress) {
        _serverAddress = serverAddress;
        configRequestUrl();
    }

    public static void setServerPort(int serverPort) {
        _serverPort = serverPort;
        configRequestUrl();
    }

    public static void setModelType(ModelType modelType) {
        _modelType = modelType;
    }

    public static void setQueue(BlockingQueue<StreamData> queue) {
        _queue = queue;
    }

    public static void setStream(boolean stream) {
        _stream = stream;
    }

    public static String callLocalModel(String prompt, ModelType model, boolean chatMode, String serverAddress, int serverPort, boolean stream, BlockingQueue<StreamData> queue) {
        setApiType(chatMode);
        setServerAddress(serverAddress);
        setServerPort(serverPort);
        setModelType(model);
        setStream(stream);
        setQueue(queue);
        return callLocalModel(prompt, model, _requestURL, stream, queue);
    }

    public static String callLocalModel(String prompt) {
        return callLocalModel(prompt, _modelType, _requestURL, _stream, _queue);
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
            boolean chatMode = model_url.contains("/api/chat");

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model.getModelName());
            requestBody.put("stream", stream);

            if (!chatMode) {
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
                        if (chatMode) {
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
     * @param model type of {@link ModelType}, used to specify the model type
     * @param model_url the url of the running address. local server may use http://localhost:11434/api/generate
     * @return JSON String of ollama model response
     */
    public static String callLocalModel(String prompt, ModelType model, String model_url) {
        return callLocalModel(prompt, model, model_url, false, null);
    }

}
