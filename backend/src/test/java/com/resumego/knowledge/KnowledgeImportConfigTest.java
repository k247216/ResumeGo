package com.resumego.knowledge;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeImportConfigTest {

    @Test
    void prefersDesktopEnvDataDirOverPropertyAndFallback() {
        // Electron 只传 RESUMEGO_DATA_DIR：桌面环境必须命中
        assertThat(KnowledgeImportConfig.resolveDataDir(
                "/Users/me/Library/Application Support/ResumeGo", "./data/resumego"))
                .isEqualTo("/Users/me/Library/Application Support/ResumeGo");
        // 无 env 时回退到属性
        assertThat(KnowledgeImportConfig.resolveDataDir("", "/custom/property")).isEqualTo("/custom/property");
        // 均缺失时使用默认
        assertThat(KnowledgeImportConfig.resolveDataDir(null, "   ")).isEqualTo("./data/resumego");
    }

    @Test
    void requestLimitLeavesOverheadOverFileLimit() {
        assertThat(KnowledgeImportConfig.MAX_FILE_BYTES).isEqualTo(10L * 1024 * 1024);
        assertThat(KnowledgeImportConfig.MAX_REQUEST_BYTES).isGreaterThan(KnowledgeImportConfig.MAX_FILE_BYTES);
    }
}
