package com.smarthome.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import com.google.common.base.Predicates; // 引入Guava的Predicates工具类

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                // 关键：用Predicates.or()组合多个包路径
                .apis(Predicates.or(
                        RequestHandlerSelectors.basePackage("com.smarthome.web.controller"),
                        RequestHandlerSelectors.basePackage("com.smarthome.user.controller")
                ))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("智能家居项目接口文档")
                .description("覆盖设备控制、用户管理等模块的API接口")
                .version("1.0")
                .build();
    }
}
