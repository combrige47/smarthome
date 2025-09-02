package com.smarthome.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.smarthome.web.service.WebService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@Controller
public class WebController {

    private final WebService webService;
    @Autowired
    public WebController(WebService webService) {
        this.webService = webService;
    }

    /**
     * 发送对应主题和消息
     * @param topic 主题
     * @param payload 消息
     * @return
     */
    @PostMapping("/send")
    public ResponseEntity<String> sendBedroom(
            @RequestParam String topic,
            @RequestParam String payload
    ) {
        webService.sendData(topic, payload);
        return ResponseEntity.ok("Bedroom sent");
    }

    /**
     * 返回当前所有主题和对应消息
     * @return
     */
    @GetMapping("/getdata")
    @ResponseBody
    public String getAll() {return webService.getAll();}

    /**
     * 返回当前对应主题的消息
     * @param deciveid 主题
     * @return
     */
    @GetMapping("/getdata/{deciveid}")
    @ResponseBody
    public String getById(@PathVariable String deciveid) {return webService.getById(deciveid);}

    /**
     *
     * @param audioFile
     * @throws IOException
     * @throws InterruptedException
     */
    @PostMapping("/recognize")
    public void recognizeAudio(@RequestParam("file") MultipartFile audioFile) throws IOException, InterruptedException {
        webService.IatToLLM(audioFile);
    }

    /**
     * 返回语音转文字的内容
     * @param audioFile 语音文件
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    @PostMapping("/iat")
    @ResponseBody
    public String iat(@RequestParam("file") MultipartFile audioFile) throws IOException, InterruptedException {
        return webService.Iat(audioFile);
    }

    /**
     * 返回ai对语音文本处理后结果
     * @param voiceText 语音文本
     * @return
     */
    @PostMapping("/testai")
    @ResponseBody
    public String testAi(@RequestParam String voiceText) {
        return webService.testAi(voiceText);
    }
}
