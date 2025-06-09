package io.github.studentrentalsystem;

import java.util.concurrent.BlockingQueue;

public class LLMConfig {
    public boolean chatMode = false;
    public String serverAddress = "http://localhost";
    public int serverPort = 11434;
    public ModelType modelType = ModelType.LLAMA3_8B;
    public boolean stream = false;
    public BlockingQueue<LLMClient.StreamData> queue = null;
    public String requestUrl = serverAddress + ":" + serverPort + "/api/generate";

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

    public LLMConfig(boolean chatMode, String serverAddress, int serverPort, ModelType modelType, boolean stream, BlockingQueue<LLMClient.StreamData> queue) {
        this.chatMode = chatMode;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.modelType = modelType;
        this.stream = stream;
        this.queue = queue;
        String target = chatMode ? "chat" : "generate";
        requestUrl = serverAddress + ":" + serverPort + "/api/" + target;
    }

    public LLMConfig() {

    }
}