package com.resumego.knowledge;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.resumego.knowledge.dto.KnowledgeSearchItemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.sql.Timestamp;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@Import({KnowledgeRepository.class, KnowledgeService.class, KnowledgeClassificationService.class})
@Sql(scripts = "/sql/knowledge_schema.sql")
class KnowledgeNoteContentIntegrationTest {

    @Autowired
    KnowledgeRepository repository;

    @Autowired
    KnowledgeService service;

    @Autowired
    KnowledgeClassificationService classification;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private long noteId;
    private long fileId;

    @BeforeEach
    void setUp() {
        noteId = repository.insertDocument(1L, "我的笔记", "NOTE", "NOT_STARTED");
        KnowledgeImportIds ids = repository.insertImportRecords(1L, "文件", new KnowledgeSourceFileDraft(
                "a.md", "knowledge/sources/1/sha-n.md", "md", 10, "sha-n", "STAGED", null));
        fileId = ids.documentId();
        repository.failImport(fileId, ids.sourceFileId(), ids.importJobId(), "EXTRACTION_FAILED", true);
    }

    @Test
    void firstSaveMarksCompletedAndOverwriteReplaces() {
        service.saveNoteContent(noteId, "第一版正文");
        assertThat(repository.findById(1L, noteId).orElseThrow().processingStatus()).isEqualTo("COMPLETED");
        assertThat(service.getContent(noteId).content()).isEqualTo("第一版正文");

        service.saveNoteContent(noteId, "第二版正文");
        assertThat(service.getContent(noteId).content()).isEqualTo("第二版正文");
    }

    @Test
    void emptyAndMarkdownChineseAreStoredVerbatim() {
        String markdown = "# 标题\n\n## 小节\n- 项目经历 TensorFlow 部署\n中文字符 123";
        service.saveNoteContent(noteId, markdown);
        assertThat(service.getContent(noteId).content()).isEqualTo(markdown);

        service.saveNoteContent(noteId, "");
        assertThat(service.getContent(noteId).content()).isEqualTo("");
        assertThat(repository.findById(1L, noteId).orElseThrow().processingStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void oversizedSaveRollsBackAndKeepsOldContentAndTimestamp() {
        service.saveNoteContent(noteId, "旧正文");
        Timestamp before = jdbcTemplate.queryForObject(
                "SELECT updated_at FROM knowledge_documents WHERE id = ?", Timestamp.class, noteId);

        assertThatThrownBy(() -> service.saveNoteContent(noteId, "a".repeat(1024 * 1024 + 1)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(service.getContent(noteId).content()).isEqualTo("旧正文");
        Timestamp after = jdbcTemplate.queryForObject(
                "SELECT updated_at FROM knowledge_documents WHERE id = ?", Timestamp.class, noteId);
        assertThat(after).isEqualTo(before);
    }

    @Test
    void fileAndCrossUserAreRejected() {
        assertThatThrownBy(() -> service.saveNoteContent(fileId, "x"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("NOTE");
        // user2 不可见 user1 的文档
        assertThat(repository.findById(2L, noteId)).isEmpty();
    }

    @Test
    void searchPicksUpNewNoteContentImmediately() {
        service.saveNoteContent(noteId, "搜索关键词-深度强化学习");
        List<KnowledgeSearchItemResponse> results = classification.search("深度强化学习", null, null, false);
        assertThat(results).anyMatch(r -> r.document().id() == noteId && "CONTENT".equals(r.matchedField()));
    }

    @Test
    void ordinaryLogsDoNotContainNoteContent() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        LoggerContext context = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = context.getLogger("com.resumego.knowledge");
        logger.addAppender(appender);
        try {
            service.saveNoteContent(noteId, "机密正文-切勿进入日志-abcdef12345");
        } finally {
            logger.detachAppender(appender);
        }
        String joined = appender.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", (a, b) -> a + "|" + b);
        assertThat(joined).doesNotContain("机密正文");
    }
}
