package com.resumego.knowledge;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class KnowledgeImportConfig {

    public static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    /** request 上限给 multipart 协议开销留余量，避免接近 10 MiB 的合法文件被提前拒绝。 */
    public static final long MAX_REQUEST_BYTES = MAX_FILE_BYTES + 1024L * 1024;
    private static final String FALLBACK_DATA_DIR = "./data/resumego";

    /** multipart 上限：file 保持 10 MiB，request 多 1 MiB 余量。 */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        return new MultipartConfigElement("", MAX_FILE_BYTES, MAX_REQUEST_BYTES, 0);
    }

    /**
     * V2 data dir 解析：优先 Electron 传入的 RESUMEGO_DATA_DIR，
     * 其次 resumego.knowledge.data-dir 属性，最后回退 ./data/resumego。
     */
    @Bean
    public KnowledgeFileStore knowledgeFileStore(
            @Value("${RESUMEGO_DATA_DIR:}") String envDataDir,
            @Value("${resumego.knowledge.data-dir:}") String propertyDataDir) {
        return new KnowledgeFileStore(Path.of(resolveDataDir(envDataDir, propertyDataDir)));
    }

    static String resolveDataDir(String envDataDir, String propertyDataDir) {
        if (envDataDir != null && !envDataDir.isBlank()) {
            return envDataDir.trim();
        }
        if (propertyDataDir != null && !propertyDataDir.isBlank()) {
            return propertyDataDir.trim();
        }
        return FALLBACK_DATA_DIR;
    }
}
