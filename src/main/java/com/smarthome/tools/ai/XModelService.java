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
import java.nio.charset.StandardCharsets; // 引入标准字符集类

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
    // 明确指定 UTF-8 字符集，避免依赖系统默认编码
    private static final String CHARSET = StandardCharsets.UTF_8.name();

    /**
     * 调用讯飞大模型 API 来获取 MQTT 控制指令。
     *
     * @param voiceText 用户语音指令（需确保已完成 URL 解码）
     * @return 标准设备控制指令（如 "f1,l10"），而非 JSON 字符串
     */
    public String getMqttCommand(String voiceText) {
        String userId = "10284711用户"; // 建议改为配置项，如 @Value("${ai.user.id}")
        try {
            // 1. 优化提示词：中英双语对照，强化中文指令匹配，去除歧义
            String prompt = String.format(
                    "【核心指令】：仅将用户的中文设备操作需求转换为标准指令字符串，不输出任何解释、分析或多余符号（包括逗号结尾）。\n" +
                            "【设备编码规则】：\n" +
                            "1. 设备映射：0号灯=l0、1号灯=l1、2号灯=l2、风扇=f、电视=TV；\n" +
                            "2. 状态映射：开=1、关=0（电视特殊：开=on、关=off）；\n" +
                            "3. 格式要求：多设备指令用英文逗号分隔，仅保留字母（小写）、数字、英文逗号，无其他字符。\n" +
                            "【正确示例】：\n" +
                            "示例1：打开0号灯、关闭风扇 → l01,f0\n" +
                            "示例2：关闭2号灯、打开电视 → l20,TVon\n" +
                            "示例3：打开风扇、关闭1号灯 → f1,l10\n" + // 新增与你的测试指令匹配的示例
                            "【错误示例】：\n" +
                            "错误1：输出 \"f1,l10,\"（末尾多逗号）；错误2：输出 \"打开风扇→f1\"（含中文）；错误3：仅输出 \"l10\"（遗漏设备）\n" +
                            "用户指令：%s",
                    voiceText
            );

            // 2. 构造请求JSON：修复参数位置（temperature/top_k 应在 messages 外，符合讯飞API规范）
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("user", userId);
            jsonObject.put("model", "x1");
            // 核心参数：temperature/top_k 是全局配置，不能放在 message 内部
            jsonObject.put("temperature", 0.2); // 降低随机性，确保指令稳定
            jsonObject.put("top_k", 2);
            jsonObject.put("stream", false);
            jsonObject.put("max_tokens", 50); // 缩减最大token，避免多余输出

            // 构造 messages 数组（讯飞API要求 role 为 "user" 或 "assistant"，message 内仅含 role/content）
            JSONArray messagesArray = new JSONArray();
            JSONObject messageObject = new JSONObject();
            messageObject.put("role", "user");
            messageObject.put("content", prompt); // 仅传入 prompt，无其他冗余参数
            messagesArray.put(messageObject);
            jsonObject.put("messages", messagesArray);

            // 打印请求体（方便调试，生产环境可注释）
            System.out.println("讯飞API请求体：" + jsonObject.toString());

            // 3. 发送HTTP请求：强制UTF-8编码，避免乱码
            String header = "Bearer " + APIPassword;
            URL obj = new URL(API_URL);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            // 关键：Content-Type 明确指定 UTF-8，告诉服务器请求体编码
            con.setRequestProperty("Content-Type", "application/json; charset=" + CHARSET);
            // 明确告诉服务器响应使用 UTF-8 编码
            con.setRequestProperty("Accept", "application/json; charset=" + CHARSET);
            con.setRequestProperty("Accept-Charset", CHARSET);
            con.setRequestProperty("Authorization", header);
            con.setDoOutput(true);

            // 写入请求体：用 UTF-8 编码字节数组
            try (OutputStream os = con.getOutputStream()) {
                byte[] requestBodyBytes = jsonObject.toString().getBytes(StandardCharsets.UTF_8);
                os.write(requestBodyBytes);
                os.flush();
            }

            // 4. 接收响应：用 UTF-8 解码，避免乱码
            int responseCode = con.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                // 读取错误响应（方便排查API报错原因）
                String errorResponse = readErrorResponse(con);
                throw new RuntimeException("讯飞API请求失败，响应码：" + responseCode + "，错误信息：" + errorResponse);
            }

            // 读取成功响应，强制 UTF-8 解码
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                String inputLine;
                StringBuilder responseSb = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    responseSb.append(inputLine);
                }
                String fullResponse = responseSb.toString();
                System.out.println("讯飞API响应体：" + fullResponse);

                // 5. 解析响应：提取 content 并清理多余字符
                JSONObject fullRespJson = new JSONObject(fullResponse);
                JSONArray choices = fullRespJson.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    JSONObject messageResp = firstChoice.getJSONObject("message");
                    if (messageResp != null && messageResp.containsKey("content")) {
                        // 清理可能的空格、换行、末尾逗号
                        String command = messageResp.getStr("content")
                                .trim() // 去除前后空格
                                .replaceAll(",$", ""); // 去除末尾多余的逗号
                        System.out.println("最终设备控制指令：" + command);
                        return command; // 直接返回指令（如 "f1,l10"），无需包装JSON
                    }
                }
                return "error: 未提取到指令";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "error: " + e.getMessage().replaceAll("\"", "'"); // 避免JSON转义问题
        }
    }

    /**
     * 读取HTTP错误响应（如401/403/500），辅助排查问题
     */
    private String readErrorResponse(HttpURLConnection con) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            return "读取错误响应失败：" + e.getMessage();
        }
    }
}