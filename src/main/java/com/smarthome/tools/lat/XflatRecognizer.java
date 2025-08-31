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


public class XflatRecognizer extends WebSocketListener {
    private static final String hostUrl = "https://iat.xf-yun.com/v1"; // 注意中文识别的地址
    private static final String appid = "0c08f010"; //在控制台-我的应用获取
    private static final String apiSecret = "Y2Y2OWRkYjJmZTdmYTY1ZTY3YWU5NzVi"; // 在控制台-我的应用获取
    private static final String apiKey = "21d4ab35c18240e5c37c5b7aaa422c21"; // 在控制台-我的应用获取
    public static String file = "src/main/resources/iat/16k_10.pcm"; // 识别音频位置
    public static String RESULT = "";
    public static final int StatusFirstFrame = 0;
    public static final int StatusContinueFrame = 1;
    public static final int StatusLastFrame = 2;
    public static final Gson gson = new Gson();
    // 开始时间
    private static Date dateBegin = new Date();
    // 结束时间
    private static Date dateEnd = new Date();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");

    private static List<String> totalResultList = new ArrayList<>();

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        super.onOpen(webSocket, response);
        new Thread(() -> {
            //连接成功，开始发送数据
            int frameSize = 1280; //每一帧音频的大小,建议每 40ms 发送 122B
            int intervel = 40;
            int status = 0;  // 音频的状态
            int seq = 0; //数据序号
            try (FileInputStream fs = new FileInputStream(file)) {
                byte[] buffer = new byte[frameSize];
                // 发送音频
                end:
                while (true) {
                    seq++; // 每次循环更新下seq
                    int len = fs.read(buffer);
                    if (len == -1) {
                        status = StatusLastFrame;  //文件读完，改变status 为 2
                    }
                    switch (status) {
                        case StatusFirstFrame:   // 第一帧音频status = 0
                            String json = "{\n" +
                                    "  \"header\": {\n" +
                                    "    \"app_id\": \"" + appid + "\",\n" +
                                    "    \"status\": " + StatusFirstFrame + "\n" +
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
                                    "      \"seq\": " + seq + ",\n" +
                                    "      \"status\": 0,\n" +
                                    "      \"audio\": \"" + Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)) + "\"\n" +
                                    "    }\n" +
                                    "  }\n" +
                                    "}";
                            webSocket.send(json);
                            // System.err.println(json);
                            System.out.println("第一帧音频发送完毕...");
                            System.out.println("中间音频将持续发送...");
                            status = StatusContinueFrame;  // 发送完第一帧改变status 为 1
                            break;
                        case StatusContinueFrame:  //中间帧status = 1
                            json = "{\n" +
                                    "  \"header\": {\n" +
                                    "    \"app_id\": \"" + appid + "\",\n" +
                                    "    \"status\": 1\n" +
                                    "  },\n" +
                                    "  \"payload\": {\n" +
                                    "    \"audio\": {\n" +
                                    "      \"encoding\": \"raw\",\n" +
                                    "      \"sample_rate\": 16000,\n" +
                                    "      \"channels\": 1,\n" +
                                    "      \"bit_depth\": 16,\n" +
                                    "      \"seq\": " + seq + ",\n" +
                                    "      \"status\": 1,\n" +
                                    "      \"audio\": \"" + Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)) + "\"\n" +
                                    "    }\n" +
                                    "  }\n" +
                                    "}";
                            webSocket.send(json);
                            // System.err.println(json);
                            // System.out.println("中间帧音频发送中...");
                            break;
                        case StatusLastFrame:    // 最后一帧音频status = 2 ，标志音频发送结束
                            json = "{\n" +
                                    "  \"header\": {\n" +
                                    "    \"app_id\": \"" + appid + "\",\n" +
                                    "    \"status\": 2\n" +
                                    "  },\n" +
                                    "  \"payload\": {\n" +
                                    "    \"audio\": {\n" +
                                    "      \"encoding\": \"raw\",\n" +
                                    "      \"sample_rate\": 16000,\n" +
                                    "      \"channels\": 1,\n" +
                                    "      \"bit_depth\": 16,\n" +
                                    "      \"seq\": " + seq + ",\n" +
                                    "      \"status\": 2,\n" +
                                    "      \"audio\": \"\"\n" +
                                    "    }\n" +
                                    "  }\n" +
                                    "}";
                            webSocket.send(json);
                            // System.err.println(json);
                            System.out.println("最后一帧音频发送完毕...");
                            break end;
                    }
                    Thread.sleep(intervel); //模拟音频采样延时
                }
                System.out.println("所有音频发送完毕...");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        super.onMessage(webSocket, text);
        // System.out.println(text);
        JsonParse jsonParse = gson.fromJson(text, JsonParse.class);
        if (jsonParse != null) {
            if (jsonParse.header.code != 0) {
                System.out.println("code=>" + jsonParse.header.code + " error=>" + jsonParse.header.message + " sid=" + jsonParse.header.sid);
                System.out.println("错误码查询链接：https://www.xfyun.cn/document/error-code");
                return;
            }
            if (jsonParse.payload != null) {
                if (jsonParse.payload.result.text != null) { // 中间结果
                    byte[] decodedBytes = Base64.getDecoder().decode(jsonParse.payload.result.text);
                    String decodeRes = new String(decodedBytes, StandardCharsets.UTF_8);
                    System.out.println("中间识别结果 ==》" + decodeRes);
                    JsonParseText jsonParseText = gson.fromJson(decodeRes, JsonParseText.class);
                    String mark = "";
                    if (jsonParseText.pgs.equals("apd")) {
                        mark = "结果追加到上面结果";
                    }
                    if (jsonParseText.pgs.equals("rpl")) {
                        mark = "结果替换前面" + jsonParseText.rg.get(0) + "到" + jsonParseText.rg.get(1) + "次";
                    }
                    List<Ws> wsList = jsonParseText.ws;
                    System.out.print("中间识别结果 【" + mark + "】==》");
                    for (Ws ws : wsList) {
                        List<Cw> cwList = ws.cw;
                        for (Cw cw : cwList) {
                            System.out.print(cw.w);
                        }
                    }
                    System.out.println();
                    RESULT = decodeRes;
                }
                if (jsonParse.payload.result.status == 2) { // 最终结果  说明数据全部返回完毕，可以关闭连接，释放资源
                    System.out.println("session end ");
                    dateEnd = new Date();
                    System.out.println(sdf.format(dateBegin) + "开始");
                    System.out.println(sdf.format(dateEnd) + "结束");
                    System.out.println("耗时:" + (dateEnd.getTime() - dateBegin.getTime()) + "ms");
//                    System.out.println("最终识别结果 ==》" + decodeRes);  // 按照规则替换与追加出最终识别结果
                    System.out.println();
                    System.out.println("本次识别sid ==》" + jsonParse.header.sid);
                    webSocket.close(1000, "");
                }
            }
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        super.onFailure(webSocket, t, response);
        try {
            if (null != response) {
                int code = response.code();
                System.out.println("onFailure code:" + code);
                System.out.println("onFailure body:" + response.body().string());
                if (101 != code) {
                    System.out.println("connection failed");
                    System.exit(0);
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static String main() throws Exception {
        // 构建鉴权url
        String authUrl = getAuthUrl(hostUrl, apiKey, apiSecret);
        OkHttpClient client = new OkHttpClient.Builder().build();
        //将url中的 schema http://和https://分别替换为ws:// 和 wss://
        String url = authUrl.toString().replace("http://", "ws://").replace("https://", "wss://");
        //System.out.println(url);
        Request request = new Request.Builder().url(url).build();
        // System.out.println(client.newCall(request).execute());
        //System.out.println("url===>" + url);
        WebSocket webSocket = client.newWebSocket(request, new XflatRecognizer());
        System.out.println(RESULT);
        return RESULT;
    }

    public static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        StringBuilder builder = new StringBuilder("host: ").append(url.getHost()).append("\n").//
                append("date: ").append(date).append("\n").//
                append("GET ").append(url.getPath()).append(" HTTP/1.1");
        // System.out.println(builder);
        Charset charset = Charset.forName("UTF-8");
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(builder.toString().getBytes(charset));
        String sha = Base64.getEncoder().encodeToString(hexDigits);

        //System.out.println(sha);
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
        //System.out.println(authorization);
        HttpUrl httpUrl = HttpUrl.parse("https://" + url.getHost() + url.getPath()).newBuilder().//
                addQueryParameter("authorization", Base64.getEncoder().encodeToString(authorization.getBytes(charset))).//
                addQueryParameter("date", date).//
                addQueryParameter("host", url.getHost()).//
                build();
        return httpUrl.toString();
    }

    // 返回结果拆分与展示，仅供参考
    // 返回结果拆分与展示，仅供参考
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
