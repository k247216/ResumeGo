package com.resumego.knowledge;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/** 启动恢复：处理遗留 PENDING 清理任务；测试环境表缺失时静默跳过。 */
@Component
public class KnowledgeCleanupStartupRunner implements ApplicationRunner {

    private final KnowledgeRecoveryService recoveryService;

    public KnowledgeCleanupStartupRunner(KnowledgeRecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            recoveryService.recoverStuckImportJobs();
            recoveryService.recoverPendingCleanupJobs();
        } catch (DataAccessException ignored) {
            // schema.sql 测试环境无 knowledge 表时静默跳过
        }
    }
}
