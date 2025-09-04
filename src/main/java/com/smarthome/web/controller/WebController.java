package com.smarthome.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.smarthome.web.service.WebService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;


@Controller
@Api(tags = "服务接口")
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
    @ApiOperation("向对应主题发送消息")
    public ResponseEntity<String> send(
            @RequestParam String topic,
            @RequestParam String payload
    ) {
        String sendmsg = webService.sendData(topic, payload);
        return ResponseEntity.ok(sendmsg);
    }

    /**
     * 返回当前所有主题和对应消息
     * @return
     */
    @GetMapping("/getdata")
    @ApiOperation("获取当前所有主题和对应消息")
    @ResponseBody
    public String getAll() {return webService.getAll();}

    /**
     * 返回当前对应主题的消息
     * @param deciveid 主题
     * @return
     */
    @GetMapping("/getdata/{deciveid}")
    @ApiOperation("获取当前对应主题的消息")
    @ResponseBody
    public String getById(@PathVariable String deciveid) {return webService.getById(deciveid);}


    /**
     * 返回语音转文字的内容
     * @param audioFile 语音文件
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    @PostMapping("/iat")
    @ApiOperation("将语音转换为文字")
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
    @ApiOperation("ai处理语音识别后的文本")
    @ResponseBody
    public String testAi(@RequestParam String voiceText) throws UnsupportedEncodingException {
        return webService.testAi(voiceText);
    }
}
