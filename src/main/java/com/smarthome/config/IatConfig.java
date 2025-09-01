package com.smarthome.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration // 仍需 Spring 加载配置，但成员改为静态
public class IatConfig {
    // 1. 静态成员（供静态 getter 访问）
    @Getter
    private static String apiSecret;
    @Getter
    private static String appId;
    @Getter
    private static String apiKey;

    // 2. 非静态成员（用于接收 @Value 注入的值）
    @Value("${Xliat.apiSecret}")
    private String injectApiSecret;
    @Value("${Xliat.appid}")
    private String injectAppId;
    @Value("${Xliat.apiKey}")
    private String injectApiKey;

    // 3. @PostConstruct：Spring 实例化该类后，自动执行此方法（初始化静态成员）
    @PostConstruct
    public void initStaticConfig() {
        IatConfig.apiSecret = this.injectApiSecret;
        IatConfig.appId = this.injectAppId;
        IatConfig.apiKey = this.injectApiKey;
    }
}