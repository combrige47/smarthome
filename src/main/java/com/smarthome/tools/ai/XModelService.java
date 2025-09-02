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
                    "已知设备编码与开关规则：0 号灯 = l0、1 号灯 = l1、2 号灯 = l2、风扇 = f、电视 = TV；开关状态用 1 表示开，0 表示关；多设备指令需用英文逗号拼接（如‘打开 1 号灯、开风扇’对应指令‘l11,f1’,电视用on和off来表示）。\n" +
                            "最后只能给出对应的指令，避免使用任何中文标点或特殊符号,请先根据此规则完成示例转换：\n" +
                            "\n" +
                            "示例 1：打开 0 号灯、关闭风扇 → l01,f0\n" +
                            "示例 2：关闭 2 号灯、打开电视 → l20,TVon\n" +
                            "后续我会给出自然语言操作需求（如‘打开所有灯、关闭电视’），请参照上述规则和示例格式，将其转换为标准指令字符串。"+
                            "用户指令：%s",voiceText
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
