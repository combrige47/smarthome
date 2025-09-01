package com.smarthome.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.smarthome.web.service.WebService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;


@Controller
public class WebController {

    private final WebService webService;
    @Autowired
    public WebController(WebService webService) {
        this.webService = webService;
    }

    @PostMapping("/send/bedroom")
    public ResponseEntity<String> sendBedroom(
            @RequestParam String topic,
            @RequestParam int value
    ) {
        webService.sendBedroomData(topic, value);
        return ResponseEntity.ok("Bedroom sent");
    }


    @GetMapping("/getdata")
    @ResponseBody
    public String getAll() {return webService.getAll();}

    @GetMapping("/getdata/{deciveid}")
    @ResponseBody
    public String getById(@PathVariable String deciveid) {return webService.getById(deciveid);}

    @PostMapping("/recognize")
    public ResponseEntity<String> recognizeAudio(@RequestParam("file") MultipartFile audioFile) {
        if (audioFile.isEmpty()) {
            return ResponseEntity.badRequest().body("请上传一个音频文件。");
        }

        return webService.IatRecognizer(audioFile);
    }

    @PostMapping("/testai")
    @ResponseBody
    public String testAi(@RequestParam String voiceText) {
        return webService.testAi(voiceText);
    }
}
