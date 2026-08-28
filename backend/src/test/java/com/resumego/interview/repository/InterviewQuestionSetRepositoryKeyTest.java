package com.resumego.interview.repository;

import com.resumego.interview.QuestionSourceType;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** JDBC 驱动兼容性回归：题集插入必须明确要求返回自增 id。 */
class InterviewQuestionSetRepositoryKeyTest {

    @Test
    void requestsIdColumnWhenCreatingQuestionSet() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        whenPreparedStatementCreated(jdbcTemplate, connection, statement);

        new InterviewQuestionSetRepository(jdbcTemplate).createSet(
                1L, "腾讯面经", QuestionSourceType.USER_MANUAL, "知识库资料",
                "腾讯", "Java 后端", "tencent", 9L, java.util.List.of("题目一"));

        verify(connection).prepareStatement(anyString(), aryEq(new String[]{"id"}));
    }

    private void whenPreparedStatementCreated(JdbcTemplate jdbcTemplate, Connection connection,
                                              PreparedStatement statement) throws Exception {
        org.mockito.Mockito.when(connection.prepareStatement(anyString(), aryEq(new String[]{"id"})))
                .thenReturn(statement);
        doAnswer(invocation -> {
            var creator = invocation.getArgument(0, org.springframework.jdbc.core.PreparedStatementCreator.class);
            KeyHolder keyHolder = invocation.getArgument(1, KeyHolder.class);
            creator.createPreparedStatement(connection);
            // MySQL 驱动可能使用 GENERATED_KEY 作为返回列名；仓储不应依赖列名文本。
            keyHolder.getKeyList().add(Map.of("GENERATED_KEY", 41L));
            return 1;
        }).when(jdbcTemplate).update(any(org.springframework.jdbc.core.PreparedStatementCreator.class), any(KeyHolder.class));
    }
}
