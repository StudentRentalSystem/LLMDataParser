package io.github.studentrentalsystem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class LLMAPIRules {
    private static final Function<JSONObject, String> receiveChatModeHandler = json ->
            json.getJSONObject("message").getString("content");

    private static final Function<JSONObject, String> receiveGenerateHandler = json ->
            json.getString("response");

    private static final Function<JSONObject, List<Float>> receiveEmbeddingsHandler = json -> {
        JSONArray array = json.getJSONArray("embedding");
        List<Float> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            result.add((float) array.getDouble(i));
        }
        return result;
    };

    private static final Map<LLMConfig.LLMMode, Function<JSONObject, ?>> receiveRules = Map.of(
            LLMConfig.LLMMode.CHAT, receiveChatModeHandler,
            LLMConfig.LLMMode.GENERATE, receiveGenerateHandler,
            LLMConfig.LLMMode.EMBEDDINGS, receiveEmbeddingsHandler
    );

    public static Map<LLMConfig.LLMMode, Function<JSONObject, ?>> getReceiveRules() {
        return receiveRules;
    }
}
