package com.smarthome.tools.ai;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 封装对讯飞大模型 API 的调用逻辑。
 * 它被标记为 @Service，以便 Spring Boot 可以管理和注入它。
 */
@Service
public class XModelService {

    // 从 application.properties 中注入 API 密码
    @Value("${ai.api.password}")
    private String APIPassword;

    private static final String API_URL = "https://spark-api-open.xf-yun.com/v2/chat/completions";

    /**
     * 调用讯飞大模型 API 来获取 MQTT 控制指令。
     *
     * @param voiceText 用户语音指令
     * @return 包含 MQTT 控制命令的 JSON 字符串
     */
    public String getMqttCommand(String voiceText) {
        String userId = "10284711用户"; // 可以根据需要将这个也作为参数传入
        try {
            // 构造完整的 Prompt
            String prompt = String.format(
                    "解析用户指令为"
            );

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("user", userId);
            jsonObject.put("model", "x1");

            JSONArray messagesArray = new JSONArray();
            JSONObject messageObject = new JSONObject();
            messageObject.put("role", "user");
            messageObject.put("content", prompt);
            messageObject.put("temperature", 0.2);
            messageObject.put("top_k", 2);

            messagesArray.put(messageObject);
            jsonObject.put("messages", messagesArray);
            jsonObject.put("stream", false);
            jsonObject.put("max_tokens",1024);

            String header = "Bearer " + APIPassword;

            URL obj = new URL(API_URL);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Authorization", header);
            con.setDoOutput(true);

            try (OutputStream os = con.getOutputStream()) {
                os.write(jsonObject.toString().getBytes());
                os.flush();
            }

            int responseCode = con.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                // 如果响应不成功，抛出异常
                throw new RuntimeException("HTTP Response Code: " + responseCode);
            }

            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                // 解析 API 返回的 JSON 响应，提取核心的 content
                JSONObject fullResponse = new JSONObject(response.toString());
                JSONArray choices = fullResponse.getJSONArray("choices");

                if (choices != null && !choices.isEmpty()) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    JSONObject message = firstChoice.getJSONObject("message");
                    System.out.println(message);
                    return message.getStr("content");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 在实际项目中，应返回更友好的错误信息
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
        return "{\"error\":\"未知错误\"}";
    }
}
