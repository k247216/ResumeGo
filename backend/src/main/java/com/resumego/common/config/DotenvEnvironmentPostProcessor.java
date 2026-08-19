package com.resumego.common.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 在 Spring Boot 启动早期加载项目根目录下的 .env 文件，
 * 将其中的键值对注入到 Environment 中，供 application.yml 的 ${...} 占位符解析。
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(DotenvEnvironmentPostProcessor.class);

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Dotenv dotenv = Dotenv.configure()
                .directory("../")
                .ignoreIfMissing()
                .load();

        Map<String, Object> dotenvProperties = new HashMap<>();
        dotenv.entries().forEach(e -> dotenvProperties.put(e.getKey(), e.getValue()));

        if (!dotenvProperties.isEmpty()) {
            environment.getPropertySources()
                    .addFirst(new MapPropertySource("dotenv", dotenvProperties));
            log.info(".env 文件已加载，共 {} 个变量", dotenvProperties.size());
        } else {
            log.info("未找到 .env 文件，跳过加载");
        }
    }
}