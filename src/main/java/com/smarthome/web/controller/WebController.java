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
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

import static java.lang.Thread.sleep;


@Controller
public class WebController {

    private final WebService webService;
    @Autowired
    public WebController(WebService webService) {
        this.webService = webService;
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

        // 1. 定义工程内的文件存储路径（工程根目录/upload/audio）
        // user.dir：获取当前工程根目录（如 D:\pro\java\smarthome）
        String projectRoot = System.getProperty("user.dir");
        // 目标文件夹：工程根目录下的 upload/audio（专门存临时音频）
        String targetDirPath = projectRoot + File.separator + "upload" + File.separator + "audio";
        File targetDir = new File(targetDirPath);

        // 2. 若目标文件夹不存在，自动创建（包括多级目录）
        if (!targetDir.exists()) {
            // mkdirs()：创建多级目录（mkdir() 只能创建单级目录，容易失败）
            boolean isCreated = targetDir.mkdirs();
            if (!isCreated) {
                return ResponseEntity.status(500).body("无法创建音频存储文件夹：" + targetDirPath);
            }
            System.out.println("音频存储文件夹已创建：" + targetDirPath);
        }

        File tempFile = null; // 用 File 直接操作，更直观
        try {
            // 3. 生成唯一的音频文件名（避免覆盖）
            String fileName = "upload-" + UUID.randomUUID().toString() + ".pcm";
            // 最终文件路径：工程根目录/upload/audio/xxx.pcm
            tempFile = new File(targetDir, fileName);
            String tempFilePath = tempFile.getAbsolutePath();
            System.out.println("音频文件将保存到：" + tempFilePath);

            // 4. 保存上传的音频文件到工程目录下
            audioFile.transferTo(tempFile); // 直接传 File 对象，无需转 Path
            System.out.println("音频文件保存成功：" + tempFilePath);

            // 5. 调用讯飞识别（使用修复后的 XflatRecognizer，传工程内的文件路径）
            XflatRecognizer recognizer = new XflatRecognizer();
            XflatRecognizer.file = tempFile.getAbsolutePath();
            String recognizeResult = recognizer.startRecognize();

            // 6. 返回识别结果
            if (recognizeResult == null || recognizeResult.trim().isEmpty()) {
                return ResponseEntity.status(500).body("语音识别失败，未获取到有效结果。");
            }
            return ResponseEntity.ok("识别结果：" + recognizeResult);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("语音识别时发生错误: " + e.getMessage());
        }
    }
}
