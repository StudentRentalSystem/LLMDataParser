package io.github.studentrentalsystem;

import java.util.concurrent.BlockingQueue;

public class LLMConfig {
    public LLMMode mode = LLMMode.GENERATE;
    public String serverAddress = "http://localhost";
    public int serverPort = 11434;
    public String modelType = "llama3:8b";
    public boolean stream = false;
    public BlockingQueue<LLMClient.StreamData> queue = null;
    public String requestUrl = serverAddress + ":" + serverPort + "/api/generate";

    public enum LLMMode {
        CHAT("chat"),
        GENERATE("generate"),
        EMBEDDINGS("embeddings");

        private final String mode;

        LLMMode(String mode) {
            this.mode = mode;
        }

        public String getMode() {
            return mode;
        }
    }

    public LLMConfig(LLMMode mode, String serverAddress, int serverPort, String modelType, boolean stream, BlockingQueue<LLMClient.StreamData> queue) throws IllegalArgumentException {
        this.mode = mode;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.modelType = modelType;
        this.stream = stream;
        this.queue = queue;
        String target = mode.getMode();
        requestUrl = serverAddress + ":" + serverPort + "/api/" + target;

        if (!ModelRegistry.isAvailable(serverAddress, serverPort, modelType)) {
            throw new IllegalArgumentException(modelType + " does not exist in this machine!!!");
        }
    }

    public LLMConfig() {

    }
}