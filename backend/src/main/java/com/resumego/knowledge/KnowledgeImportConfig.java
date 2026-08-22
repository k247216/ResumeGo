package com.resumego.knowledge;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class KnowledgeImportConfig {

    public static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    /** multipart 上限提到 10 MiB（Spring Boot 默认 1 MiB 过小）。 */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        return new MultipartConfigElement("", MAX_FILE_BYTES, MAX_FILE_BYTES, 0);
    }

    /** V2 data dir（默认与本地 H2 同级），knowledge 文件存于其下。 */
    @Bean
    public KnowledgeFileStore knowledgeFileStore(
            @Value("${resumego.knowledge.data-dir:./data/resumego}") String dataDir) {
        return new KnowledgeFileStore(Path.of(dataDir));
    }
}
