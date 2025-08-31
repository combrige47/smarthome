package com.smarthome.tools.lat;

import com.google.gson.Gson;
import okhttp3.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch; // 引入同步锁工具


public class XflatRecognizer extends WebSocketListener {
    private static final String hostUrl = "https://iat.xf-yun.com/v1"; // 中文识别地址
    private static final String appid = "0c08f010"; // 替换为你的AppID
    private static final String apiSecret = "Y2Y2OWRkYjJmZTdmYTY1ZTY3YWU5NzVi"; // 替换为你的API Secret
    private static final String apiKey = "21d4ab35c18240e5c37c5b7aaa422c21"; // 替换为你的API Key
    public static String file = "src/main/resources/iat/16k_10.pcm"; // 音频文件路径
    private final Gson gson = new Gson();
    private final Date dateBegin = new Date();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");

    // 关键修改1：用List存储中间结果（支持追加/替换逻辑），用CountDownLatch实现同步等待
    private List<String> segmentResults = new ArrayList<>(); // 存储分段识别结果
    private final CountDownLatch resultLatch = new CountDownLatch(1); // 1表示等待1个"最终结果"信号
    private String finalResult = ""; // 存储最终拼接后的完整结果
    private Date dateEnd;


    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        super.onOpen(webSocket, response);
        new Thread(() -> {
            int frameSize = 1280; // 40ms音频帧大小（16000Hz/16bit/单声道：16000*16*1/8*0.04=1280）
            int intervel = 40; // 模拟采样间隔（40ms）
            int status = 0; // 音频帧状态（0：第一帧，1：中间帧，2：最后一帧）
            int seq = 0; // 数据序号

            try (FileInputStream fs = new FileInputStream(file)) {
                byte[] buffer = new byte[frameSize];
                end:
                while (true) {
                    seq++;
                    int len = fs.read(buffer);
                    if (len == -1) {
                        status = 2; // 文件读完，标记为最后一帧
                    }

                    // 构建不同帧的请求JSON
                    String json;
                    switch (status) {
                        case 0: // 第一帧：携带完整参数
                            json = buildFirstFrameJson(seq, buffer, len);
                            webSocket.send(json);
                            System.out.println("第一帧音频发送完毕...");
                            status = 1; // 后续切换为中间帧
                            break;
                        case 1: // 中间帧：仅携带音频数据
                            json = buildContinueFrameJson(seq, buffer, len);
                            webSocket.send(json);
                            break;
                        case 2: // 最后一帧：标记结束，音频数据为空
                            json = buildLastFrameJson(seq);
                            webSocket.send(json);
                            System.out.println("最后一帧音频发送完毕...");
                            break end;
                    }
                    Thread.sleep(intervel); // 模拟实时采样间隔
                }
                System.out.println("所有音频发送完毕...");
            } catch (FileNotFoundException e) {
                System.err.println("音频文件未找到：" + file);
                e.printStackTrace();
                resultLatch.countDown(); // 异常时释放锁，避免死等
            } catch (IOException e) {
                System.err.println("音频文件读取失败");
                e.printStackTrace();
                resultLatch.countDown();
            } catch (InterruptedException e) {
                System.err.println("发送线程被中断");
                Thread.currentThread().interrupt();
                resultLatch.countDown();
            }
        }).start();
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        super.onMessage(webSocket, text);
        // 官方Demo核心：全局异常捕获，避免连接因异常中断
        try {
            // 1. 解析服务端返回的JSON结构（先判空，避免NullPointerException）
            if (text == null || text.trim().isEmpty()) {
                System.err.println("服务端返回空结果，跳过处理");
                return;
            }
            JsonParse jsonParse = gson.fromJson(text, JsonParse.class);
            if (jsonParse == null) {
                System.err.println("服务端返回结果解析失败，原始文本：" + text);
                return;
            }

            // 2. 处理错误码（官方Demo强制校验，避免无效流程）
            if (jsonParse.header.code != 0) {
                String errorMsg = String.format("识别错误：code=%d，message=%s，sid=%s",
                        jsonParse.header.code, jsonParse.header.message, jsonParse.header.sid);
                System.err.println(errorMsg);
                System.err.println("错误码查询：https://www.xfyun.cn/document/error-code");
                // 错误时标记结果并释放锁
                finalResult = errorMsg;
                resultLatch.countDown();
                return;
            }

            // 3. 处理识别结果（仅当payload和result非空时执行）
            if (jsonParse.payload == null || jsonParse.payload.result == null) {
                System.out.println("服务端返回无有效结果，原始文本：" + text);
                return;
            }
            Result result = jsonParse.payload.result;

            // 4. 处理中间分段结果（text非空表示有分段数据）
            if (result.text != null && !result.text.trim().isEmpty()) {
                // 4.1 解码Base64结果（官方Demo标准步骤）
                byte[] decodedBytes = Base64.getDecoder().decode(result.text);
                String decodeRes = new String(decodedBytes, StandardCharsets.UTF_8);
                // 4.2 解析分段结果的JSON结构（包含pgs、rg、ws）
                JsonParseText jsonParseText = gson.fromJson(decodeRes, JsonParseText.class);
                if (jsonParseText == null || jsonParseText.ws == null || jsonParseText.ws.isEmpty()) {
                    System.err.println("分段结果解析失败，解码后文本：" + decodeRes);
                    return;
                }

                // 4.3 提取当前分段的纯文本（从ws->cw中拼接w，官方Demo通用逻辑）
                String currentSegment = getSegmentText(jsonParseText.ws);
                // 4.4 按pgs类型处理：追加（apd）或替换（rpl）
                String mark = "";
                if ("apd".equals(jsonParseText.pgs)) {
                    // 追加模式：直接添加到结果列表末尾（官方Demo无特殊判断，简单安全）
                    segmentResults.add(currentSegment);
                    mark = "结果追加到末尾";
                } else if ("rpl".equals(jsonParseText.pgs)) {
                    // 替换模式：官方Demo核心逻辑——先校验rg参数合法性，再执行替换
                    if (jsonParseText.rg == null || jsonParseText.rg.size() < 2) {
                        System.err.println("rpl模式缺少合法rg参数，转为追加模式，解码文本：" + decodeRes);
                        segmentResults.add(currentSegment);
                        mark = "rpl参数无效，转为追加";
                    } else {
                        int start = jsonParseText.rg.get(0);
                        int end = jsonParseText.rg.get(1);
                        int listSize = segmentResults.size();
                        // 合法性校验：避免索引越界（官方Demo关键防错步骤）
                        start = Math.max(0, start); // 最小取0
                        end = Math.min(listSize, end); // 最大取列表长度
                        if (start > end) {
                            System.err.println("rpl模式范围无效（start=" + start + ", end=" + end + "），转为追加模式");
                            segmentResults.add(currentSegment);
                            mark = "rpl范围无效，转为追加";
                        } else {
                            // 执行替换：从后往前删除（避免索引移位，官方Demo标准写法）
                            for (int i = end - 1; i >= start; i--) {
                                if (i < segmentResults.size()) { // 额外保险
                                    segmentResults.remove(i);
                                }
                            }
                            segmentResults.add(start, currentSegment);
                            mark = "结果替换：" + start + "到" + (end - 1) + "位置";
                        }
                    }
                } else {
                    // 未知pgs类型：官方Demo建议转为追加，避免流程中断
                    segmentResults.add(currentSegment);
                    mark = "未知pgs类型（" + jsonParseText.pgs + "），转为追加";
                }

                // 4.5 打印中间结果（与官方Demo一致，便于调试）
                String middleResult = String.join("", segmentResults);
                System.out.printf("中间识别结果 【%s】==》%s%n", mark, middleResult);
            }

            // 5. 处理最终结果（status=2表示所有数据返回完毕，官方Demo终止条件）
            if (result.status == 2) {
                // 5.1 拼接所有分段，得到最终结果（官方Demo核心输出逻辑）
                finalResult = String.join("", segmentResults);
                // 5.2 打印最终信息（与官方Demo格式一致）
                System.out.println("\n===== 识别完成（session end） =====");
                dateEnd = new Date();
                System.out.println("开始时间：" + sdf.format(dateBegin));
                System.out.println("结束时间：" + sdf.format(dateEnd));
                System.out.println("耗时：" + (dateEnd.getTime() - dateBegin.getTime()) + "ms");
                System.out.println("本次识别SID：" + jsonParse.header.sid);
                System.out.println("===== 最终识别结果 =====");
                System.out.println(finalResult);
                System.out.println("=======================");

                // 5.3 关闭WebSocket连接（官方Demo标准操作，避免资源泄漏）
                webSocket.close(1000, "识别完成，主动关闭连接");
                // 5.4 释放锁，通知main方法可以返回结果
                resultLatch.countDown();
            }
        } catch (Exception e) {
            // 官方Demo关键：捕获所有异常，避免WebSocket线程崩溃
            String errorMsg = "处理识别结果时发生异常：" + e.getMessage();
            System.err.println(errorMsg);
            e.printStackTrace();
            // 异常时标记结果并释放锁，避免main方法无限等待
            if (finalResult.isEmpty()) {
                finalResult = errorMsg;
                resultLatch.countDown();
            }
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        super.onFailure(webSocket, t, response);
        try {
            String errorMsg = "WebSocket连接失败：" + t.getMessage();
            if (response != null) {
                errorMsg += "，响应码：" + response.code() + "，响应体：" + response.body().string();
            }
            System.err.println(errorMsg);
            finalResult = errorMsg; // 存储错误信息作为结果
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            resultLatch.countDown(); // 失败时释放锁，避免死等
        }
    }

    // ---------------------- 工具方法：构建请求JSON ----------------------

    /**
     * 构建第一帧请求JSON
     */
    private String buildFirstFrameJson(int seq, byte[] buffer, int len) {
        String audioBase64 = Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len));
        return String.format("{\n" +
                "  \"header\": {\n" +
                "    \"app_id\": \"%s\",\n" +
                "    \"status\": 0\n" +
                "  },\n" +
                "  \"parameter\": {\n" +
                "    \"iat\": {\n" +
                "      \"domain\": \"slm\",\n" +
                "      \"language\": \"zh_cn\",\n" +
                "      \"accent\": \"mandarin\",\n" +
                "      \"eos\": 6000,\n" +
                "      \"vinfo\": 1,\n" +
                "      \"dwa\": \"wpgs\",\n" +
                "      \"result\": {\n" +
                "        \"encoding\": \"utf8\",\n" +
                "        \"compress\": \"raw\",\n" +
                "        \"format\": \"json\"\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"payload\": {\n" +
                "    \"audio\": {\n" +
                "      \"encoding\": \"raw\",\n" +
                "      \"sample_rate\": 16000,\n" +
                "      \"channels\": 1,\n" +
                "      \"bit_depth\": 16,\n" +
                "      \"seq\": %d,\n" +
                "      \"status\": 0,\n" +
                "      \"audio\": \"%s\"\n" +
                "    }\n" +
                "  }\n" +
                "}", appid, seq, audioBase64);
    }

    /**
     * 构建中间帧请求JSON
     */
    private String buildContinueFrameJson(int seq, byte[] buffer, int len) {
        String audioBase64 = Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len));
        return String.format("{\n" +
                "  \"header\": {\n" +
                "    \"app_id\": \"%s\",\n" +
                "    \"status\": 1\n" +
                "  },\n" +
                "  \"payload\": {\n" +
                "    \"audio\": {\n" +
                "      \"encoding\": \"raw\",\n" +
                "      \"sample_rate\": 16000,\n" +
                "      \"channels\": 1,\n" +
                "      \"bit_depth\": 16,\n" +
                "      \"seq\": %d,\n" +
                "      \"status\": 1,\n" +
                "      \"audio\": \"%s\"\n" +
                "    }\n" +
                "  }\n" +
                "}", appid, seq, audioBase64);
    }

    /**
     * 构建最后一帧请求JSON
     */
    private String buildLastFrameJson(int seq) {
        return String.format("{\n" +
                "  \"header\": {\n" +
                "    \"app_id\": \"%s\",\n" +
                "    \"status\": 2\n" +
                "  },\n" +
                "  \"payload\": {\n" +
                "    \"audio\": {\n" +
                "      \"encoding\": \"raw\",\n" +
                "      \"sample_rate\": 16000,\n" +
                "      \"channels\": 1,\n" +
                "      \"bit_depth\": 16,\n" +
                "      \"seq\": %d,\n" +
                "      \"status\": 2,\n" +
                "      \"audio\": \"\"\n" +
                "    }\n" +
                "  }\n" +
                "}", appid, seq);
    }

    /**
     * 提取单段结果的纯文本（从Ws->Cw中拼接w）
     */
    private String getSegmentText(List<Ws> wsList) {
        StringBuilder segment = new StringBuilder();
        for (Ws ws : wsList) {
            for (Cw cw : ws.cw) {
                segment.append(cw.w);
            }
        }
        return segment.toString();
    }

    // ---------------------- 对外方法：获取最终结果 ----------------------

    /**
     * 启动识别并返回最终结果（对外调用入口）
     */
    public String startRecognize() throws Exception {
        // 1. 构建鉴权URL
        String authUrl = getAuthUrl(hostUrl, apiKey, apiSecret);
        // 2. 替换为WebSocket协议（http->ws，https->wss）
        String wsUrl = authUrl.replace("http://", "ws://").replace("https://", "wss://");
        // 3. 创建OkHttp客户端和请求
        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url(wsUrl).build();
        // 4. 建立WebSocket连接（传入当前实例作为Listener）
        client.newWebSocket(request, this);
        // 5. 等待最终结果（最多等待30秒，避免无限阻塞）
        boolean isResultReady = resultLatch.await(30, java.util.concurrent.TimeUnit.SECONDS);
        if (!isResultReady) {
            finalResult = "识别超时（超过30秒未返回结果）";
        }
        // 6. 返回最终结果（可能是完整文本或错误信息）
        return finalResult;
    }

    /**
     * 生成讯飞鉴权URL（原逻辑不变）
     */
    private String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());

        // 构建签名原始串
        StringBuilder signBuilder = new StringBuilder()
                .append("host: ").append(url.getHost()).append("\n")
                .append("date: ").append(date).append("\n")
                .append("GET ").append(url.getPath()).append(" HTTP/1.1");

        // HMAC-SHA256签名
        Charset charset = StandardCharsets.UTF_8;
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
        mac.init(spec);
        byte[] signBytes = mac.doFinal(signBuilder.toString().getBytes(charset));
        String signBase64 = Base64.getEncoder().encodeToString(signBytes);

        // 构建Authorization头
        String authorization = String.format(
                "api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                apiKey, "hmac-sha256", "host date request-line", signBase64
        );

        // 拼接鉴权URL
        return HttpUrl.parse("https://" + url.getHost() + url.getPath())
                .newBuilder()
                .addQueryParameter("authorization", Base64.getEncoder().encodeToString(authorization.getBytes(charset)))
                .addQueryParameter("date", date)
                .addQueryParameter("host", url.getHost())
                .build()
                .toString();
    }
    class JsonParse {
        Header header;
        Payload payload;
    }

    class Header {
        int code;
        String message;
        String sid;
        int status;
    }

    class Payload {
        Result result;
    }

    class Result {
        String text;
        int status;
    }

    class JsonParseText {
        List<Ws> ws;
        String pgs;
        List<Integer> rg;
    }

    class Ws {
        List<Cw> cw;
    }

    class Cw {
        String w;
    }

}
