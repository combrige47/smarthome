package com.smarthome.web.controller;

import com.smarthome.tools.lat.XflatRecognizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.smarthome.web.service.WebService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;


@Controller
public class WebController {

    private final WebService webService;
    @Autowired
    public WebController(WebService webService) {
        this.webService = webService;
    }

    @PostMapping("/send/bedroom")
    public ResponseEntity<String> sendBedroom(
            @RequestParam int l0,
            @RequestParam int l1,
            @RequestParam int l2,
            @RequestParam int fan,
            @RequestParam int tv
    ) {
        webService.sendBedroomData(l0, l1, l2, fan, tv);
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
}
