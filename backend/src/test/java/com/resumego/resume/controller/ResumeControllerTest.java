package com.resumego.resume.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.resume.dto.CreateResumeRequest;
import com.resumego.resume.dto.CreateResumeVersionRequest;
import com.resumego.resume.dto.ResumeDTO;
import com.resumego.resume.dto.ResumeVersionDTO;
import com.resumego.resume.dto.UpdateResumeTargetJobRequest;
import com.resumego.resume.repository.ResumeRepository;
import com.resumego.resume.service.ResumeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ResumeController 边界测试（通过 Service 层测试入参校验逻辑）。
 */
@DisplayName("ResumeController 边界测试")
class ResumeControllerTest {

    private TestRepo repo;
    private ResumeService resumeService;

    @BeforeEach
    void setUp() {
        repo = new TestRepo();
        resumeService = new ResumeService(repo, new ObjectMapper());
    }

    static class TestRepo extends ResumeRepository {
        String insertedTitle;
        Long insertedTargetJobId;
        String insertedSummary;
        Long updatedTargetJobResumeId;
        Long updatedTargetJobId;
        int insertResumeCount;
        int insertVersionCount;
        long lastVersionId;
        long lastResumeId;
        Long storedTargetJobId;

        TestRepo() { super(null, new ObjectMapper()); }

        @Override public String findTitleById(long id) {
            if (id == 1L) return "简历1";
            if (id == 3L) return "简历3";
            return null;
        }

        @Override public Long findTargetJobDescriptionIdById(long id) {
            if (id == 1L) return 88L;
            if (id == 3L) return storedTargetJobId;
            return null;
        }

        @Override public Long findCurrentVersionId(long resumeId) {
            if (resumeId == 1L) return 10L;
            if (resumeId == 3L) return lastVersionId;
            return null;
        }

        @Override public ResumeVersionDTO findVersionById(long versionId) {
            if (versionId == 10L)
                return new ResumeVersionDTO(10L, 1L, null, 2, Map.of("name", "old"), null, "user", LocalDateTime.now());
            if (versionId == lastVersionId)
                return new ResumeVersionDTO(lastVersionId, lastResumeId, 10L, 3, Map.of("basicInfo", Map.of("name", "Demo")), insertedSummary, "user", LocalDateTime.now());
            return null;
        }

        @Override public List<ResumeVersionDTO> findVersionsByResumeId(long resumeId) {
            return List.of();
        }

        @Override public int findMaxVersionNo(long resumeId) { return resumeId == 1L ? 2 : 0; }

        @Override public long insertResume(long userId, String title, Long targetJobDescriptionId) {
            insertResumeCount++;
            insertedTitle = title;
            insertedTargetJobId = targetJobDescriptionId;
            storedTargetJobId = targetJobDescriptionId;
            lastResumeId = 3L;
            return 3L;
        }

        @Override public long insertVersion(long resumeId, Long parentVersionId, int versionNo,
                                            String contentJson, String changeSummary, String createdByType) {
            insertVersionCount++;
            insertedSummary = changeSummary;
            lastVersionId = 12L;
            lastResumeId = resumeId;
            return 12L;
        }

        @Override public void updateCurrentVersionId(long resumeId, long versionId) {
            lastVersionId = versionId;
            lastResumeId = resumeId;
        }

        @Override public void updateTargetJobDescriptionId(long resumeId, Long targetJobDescriptionId) {
            updatedTargetJobResumeId = resumeId;
            updatedTargetJobId = targetJobDescriptionId;
        }

        @Override public void replaceEvidenceRefsForVersion(long resumeVersionId, long userId,
                                                            List<EvidenceRefDraft> evidenceRefs) {
            // Controller 边界测试只关注入参校验和版本创建结果，证据引用同步由 ResumeServiceTest 覆盖。
        }
    }

    @Nested
    @DisplayName("createResume 边界")
    class CreateResume {

        @Test
        @DisplayName("空白名称 → 拒绝")
        void rejectBlankTitle() {
            assertThatThrownBy(() -> resumeService.createResume(
                    new CreateResumeRequest(" ", Map.of("basicInfo", Map.of("name", "test")), null, null)
            )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("简历名称不能为空");
        }

        @Test
        @DisplayName("null 名称 → 拒绝")
        void rejectNullTitle() {
            assertThatThrownBy(() -> resumeService.createResume(
                    new CreateResumeRequest(null, Map.of("basicInfo", Map.of("name", "test")), null, null)
            )).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空内容 → 拒绝")
        void rejectEmptyContent() {
            assertThatThrownBy(() -> resumeService.createResume(
                    new CreateResumeRequest("test", Map.of(), null, null)
            )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("简历内容不能为空");
        }

        @Test
        @DisplayName("null 请求 → 拒绝")
        void rejectNullRequest() {
            assertThatThrownBy(() -> resumeService.createResume(null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("简历内容不能为空");
        }

        @Test
        @DisplayName("正常创建 → 保存并返回")
        void createSuccess() {
            ResumeDTO r = resumeService.createResume(
                    new CreateResumeRequest("后端实习简历", Map.of("basicInfo", Map.of("name", "Demo")), "v1", 88L));
            assertThat(r.id()).isEqualTo(3L);
            assertThat(r.targetJobDescriptionId()).isEqualTo(88L);
            assertThat(repo.insertResumeCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("createManualVersion 边界")
    class CreateManualVersion {

        @Test
        @DisplayName("简历不存在 → 拒绝")
        void rejectMissingResume() {
            assertThatThrownBy(() -> resumeService.createManualVersion(999L,
                    new CreateResumeVersionRequest(Map.of("basicInfo", Map.of("name", "Demo")), "修改")))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("简历不存在");
        }

        @Test
        @DisplayName("空内容 → 拒绝")
        void rejectEmptyContent() {
            assertThatThrownBy(() -> resumeService.createManualVersion(1L,
                    new CreateResumeVersionRequest(Map.of(), "空")))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("简历内容不能为空");
        }

        @Test
        @DisplayName("超长内容 → 拒绝")
        void rejectOversized() {
            StringBuilder sb = new StringBuilder(50_001);
            for (int i = 0; i < 50_001; i++) sb.append('x');
            assertThatThrownBy(() -> resumeService.createManualVersion(1L,
                    new CreateResumeVersionRequest(Map.of("text", sb.toString()), "超长")))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("简历内容过长");
        }

        @Test
        @DisplayName("正常创建版本")
        void createVersionSuccess() {
            ResumeVersionDTO v = resumeService.createManualVersion(1L,
                    new CreateResumeVersionRequest(Map.of("basicInfo", Map.of("name", "Demo"), "skills", List.of("Java")), "编辑"));
            assertThat(v.id()).isEqualTo(12L);
            assertThat(v.versionNo()).isEqualTo(3);
            assertThat(repo.insertVersionCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getVersion / getVersions 边界")
    class GetVersion {

        @Test
        @DisplayName("版本不存在 → 拒绝")
        void rejectMissingVersion() {
            assertThatThrownBy(() -> resumeService.getVersion(999L))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("简历版本不存在");
        }

        @Test
        @DisplayName("简历不存在时查版本列表 → 拒绝")
        void rejectMissingResumeForVersions() {
            assertThatThrownBy(() -> resumeService.getVersions(999L))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("简历不存在");
        }
    }

    @Nested
    @DisplayName("updateTargetJob 边界")
    class UpdateTargetJob {

        @Test
        @DisplayName("简历不存在 → 拒绝")
        void rejectMissingResume() {
            assertThatThrownBy(() -> resumeService.updateTargetJob(999L, new UpdateResumeTargetJobRequest(66L)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("简历不存在");
        }
    }
}
