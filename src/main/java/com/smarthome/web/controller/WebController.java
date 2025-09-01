package com.smarthome.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.smarthome.web.service.WebService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;


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
    public void recognizeAudio(@RequestParam("file") MultipartFile audioFile) throws IOException, InterruptedException {
        webService.IatToLLM(audioFile);
    }

    @PostMapping("/iat")
    @ResponseBody
    public String iat(@RequestParam("file") MultipartFile audioFile) throws IOException, InterruptedException {
        return webService.Iat(audioFile);
    }

    @PostMapping("/testai")
    @ResponseBody
    public String testAi(@RequestParam String voiceText) {
        return webService.testAi(voiceText);
    }
}
