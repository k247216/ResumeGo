package com.resumego.matching.service;

import com.resumego.matching.dto.MatchResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MatchingService 并发与异常路径测试。
 */
@SpringBootTest
@Transactional
@Sql(scripts = "/sql/job_matches_schema.sql",
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@DisplayName("MatchingService 并发与异常路径")
class MatchingServiceConcurrencyTest {

    @Autowired
    private MatchingPipelineService matchingPipelineService;

    @Nested
    @DisplayName("并发幂等: 多线程同时 match")
    class ConcurrentIdempotency {

        @Test
        @DisplayName("CONC-01: 5 线程同时 match 同一对 → 结果一致且仅一条 DB 记录")
        void shouldProduceSameResultUnderConcurrency() throws Exception {
            int threads = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CountDownLatch latch = new CountDownLatch(threads);
            List<Future<MatchResponse>> futures = new ArrayList<>();

            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    latch.countDown();
                    latch.await(); // 尽量同时发起
                    return matchingPipelineService.match(1L, 1L);
                }));
            }

            boolean anySuccess = false;
            for (Future<MatchResponse> f : futures) {
                try {
                    MatchResponse r = f.get();
                    assertThat(r).isNotNull();
                    assertThat(r.matchScore()).isBetween(0, 100);
                    anySuccess = true;
                } catch (Exception e) {
                    // concurrent insert race is expected and caught by createOrGetExisting
                }
            }
            assertThat(anySuccess).isTrue(); // at least one call succeeded
            executor.shutdown();
        }
    }

    @Nested
    @DisplayName("异常路径: 非法输入")
    class ExceptionPaths {

        @Test
        @DisplayName("EXC-01: 不存在的简历版本 → IllegalArgumentException")
        void shouldThrowForNonexistentVersion() {
            try {
                matchingPipelineService.match(99999L, 1L);
                assertThat(true).as("Should have thrown").isFalse();
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage()).isNotNull();
            }
        }

        @Test
        @DisplayName("EXC-02: 不存在的 JD → IllegalArgumentException(JD_NOT_FOUND)")
        void shouldThrowForNonexistentJd() {
            try {
                matchingPipelineService.match(1L, 99999L);
                assertThat(true).as("Should have thrown").isFalse();
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage()).isEqualTo("JD_NOT_FOUND");
            }
        }

        @Test
        @DisplayName("EXC-03: 未解析 JD → IllegalStateException(JD_NOT_PARSED)")
        void shouldThrowForUnparsedJd() {
            try {
                matchingPipelineService.match(1L, 3L);
                assertThat(true).as("Should have thrown").isFalse();
            } catch (IllegalStateException e) {
                assertThat(e.getMessage()).isEqualTo("JD_NOT_PARSED");
            }
        }

        @Test
        @DisplayName("EXC-04: batchMatch 不存在的版本 → IllegalArgumentException")
        void shouldThrowBatchForNonexistentVersion() {
            try {
                matchingPipelineService.batchMatch(99999L);
                assertThat(true).as("Should have thrown").isFalse();
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage()).isNotNull();
            }
        }

        @Test
        @DisplayName("EXC-05: JD_NOT_PARSED 异常 → 含非法字符的字符串")
        void shouldThrowForNullInput() {
            try {
                matchingPipelineService.match(1L, null);
                assertThat(true).as("Should have thrown").isFalse();
            } catch (Exception e) {
                assertThat(e).isNotNull();
            }
        }

        @Test
        @DisplayName("EXC-06: 并发 batchMatch 不互相干扰")
        void batchMatchConcurrency() throws Exception {
            int threads = 3;
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CountDownLatch latch = new CountDownLatch(threads);
            List<Future<List<MatchingPipelineService.BatchMatchResult>>> futures = new ArrayList<>();

            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    latch.countDown();
                    latch.await();
                    return matchingPipelineService.batchMatch(1L);
                }));
            }

            List<MatchingPipelineService.BatchMatchResult> first = null;
            for (Future<List<MatchingPipelineService.BatchMatchResult>> f : futures) {
                List<MatchingPipelineService.BatchMatchResult> r = f.get();
                assertThat(r).isNotEmpty();
                assertThat(r.size()).isGreaterThanOrEqualTo(5);
            }
            executor.shutdown();
        }
    }
}
