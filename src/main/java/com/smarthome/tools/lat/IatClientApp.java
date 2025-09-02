package com.smarthome.tools.lat;

import cn.xfyun.api.IatClient;
import com.smarthome.config.IatConfig;
import cn.xfyun.model.response.iat.IatResponse;
import cn.xfyun.model.response.iat.IatResult;
import cn.xfyun.model.response.iat.Text;
import cn.xfyun.service.iat.AbstractIatWebSocketListener;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.LineUnavailableException;

@Component
public class IatClientApp {
    private static final Logger logger = LoggerFactory.getLogger(IatClientApp.class);
    private static final String APP_ID = IatConfig.getAppId();
    private static final String API_KEY = IatConfig.getApiKey();
    private static final String API_SECRET = IatConfig.getApiSecret();
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final IatClient IAT_CLIENT;

    // 初始化客户端（保持不变）
    static {
        IAT_CLIENT = new IatClient.Builder()
                .signature(APP_ID, API_KEY, API_SECRET)
                .dwa("wpgs")
                .vad_eos(6000)
                .build();
    }

    /**
     * 处理音频文件并返回识别结果（同步等待）
     * @param audioFilePath 音频文件路径
     * @return 识别结果
     * @throws InterruptedException 等待被中断
     */
    public String processAudioSync(String audioFilePath) throws InterruptedException {
        // 1. 用 CountDownLatch 实现同步等待（1 表示等待 1 次信号）
        CountDownLatch latch = new CountDownLatch(1);
        // 2. 用线程安全的局部变量存储结果（避免多线程冲突）
        StringBuilder finalResult = new StringBuilder();
        List<Text> resultSegments = new ArrayList<>();
        Date dateBegin = new Date();

        // 3. 自定义监听器，识别完成后释放等待
        AbstractIatWebSocketListener listener = new AbstractIatWebSocketListener() {
            @Override
            public void onSuccess(WebSocket webSocket, IatResponse iatResponse) {
                if (iatResponse.getCode() != 0) {
                    logger.warn("识别错误：code={}, error={}, sid={}",
                            iatResponse.getCode(), iatResponse.getMessage(), iatResponse.getSid());
                    latch.countDown(); // 错误时也释放等待
                    return;
                }

                if (iatResponse.getData() != null && iatResponse.getData().getResult() != null) {
                    IatResult result = iatResponse.getData().getResult();
                    Text textObject = result.getText();
                    handleResultText(textObject, resultSegments); // 处理结果到局部列表
                    logger.info("中间识别结果：{}", getCurrentResult(resultSegments));
                }

                // 4. 识别完成（status=2）时，拼接最终结果并释放等待
                if (iatResponse.getData() != null && iatResponse.getData().getStatus() == 2) {
                    Date dateEnd = new Date();
                    finalResult.append(getCurrentResult(resultSegments)); // 保存最终结果
                    logger.info("识别完成，总耗时：{}ms，结果：【{}】",
                            dateEnd.getTime() - dateBegin.getTime(), finalResult);
                    IAT_CLIENT.closeWebsocket();
                    latch.countDown(); // 释放等待
                }
            }

            @Override
            public void onFail(WebSocket webSocket, Throwable t, Response response) {
                logger.error("识别失败", t);
                latch.countDown(); // 失败时释放等待
            }
        };

        // 5. 发送音频并等待结果（最多等 60 秒，避免无限阻塞）
        try {
            File file = new File(audioFilePath);
            IAT_CLIENT.send(file, listener);
            latch.await(60, TimeUnit.SECONDS); // 等待识别完成或超时
        } catch (FileNotFoundException e) {
            logger.error("文件未找到：{}", audioFilePath, e);
            return "音频文件不存在";
        } catch (Exception e) {
            logger.error("处理音频失败", e);
            return "识别失败：" + e.getMessage();
        }

        return finalResult.toString();
    }

    // 处理结果片段（改为局部列表参数，避免静态成员冲突）
    private void handleResultText(Text textObject, List<Text> resultSegments) {
        if (StringUtils.equals(textObject.getPgs(), "rpl")
                && textObject.getRg() != null && textObject.getRg().length == 2) {
            int start = textObject.getRg()[0] - 1;
            int end = textObject.getRg()[1] - 1;
            for (int i = start; i <= end && i < resultSegments.size(); i++) {
                resultSegments.get(i).setDeleted(true);
            }
        }
        resultSegments.add(textObject);
    }

    // 获取当前结果（基于局部列表）
    private String getCurrentResult(List<Text> resultSegments) {
        StringBuilder sb = new StringBuilder();
        for (Text text : resultSegments) {
            if (text != null && !text.isDeleted()) {
                sb.append(text.getText());
            }
        }
        return sb.toString();
    }

}