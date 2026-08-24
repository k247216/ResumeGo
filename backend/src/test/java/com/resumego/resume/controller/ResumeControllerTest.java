package com.resumego.resume.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.resume.dto.CreateResumeRequest;
import com.resumego.resume.dto.CreateResumeVersionRequest;
import com.resumego.resume.dto.ForkResumeVersionRequest;
import com.resumego.resume.dto.ResumeDTO;
import com.resumego.resume.dto.ResumeVersionDTO;
import com.resumego.resume.dto.UpdateResumeAssetRequest;
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
import java.util.NoSuchElementException;

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
        Long softDeletedResumeId;
        int forkCalls;
        Long forkedSourceVersionId;
        String forkedTitle;
        String forkedContentJson;
        Long renamedResumeId;
        String renamedTitle;
        final java.util.Map<Long, java.time.LocalDateTime> archivedWrites = new java.util.HashMap<>();
        String listQueryKind;
        Boolean listQueryArchived;

        TestRepo() { super(null, new ObjectMapper()); }

        @Override
        public int softDelete(long userId, long resumeId) {
            softDeletedResumeId = resumeId;
            return resumeId == 1L || resumeId == 3L ? 1 : 0;
        }

        @Override public String findTitleById(long userId, long id) {
            if (renamedResumeId != null && id == renamedResumeId && renamedTitle != null) return renamedTitle;
            if (id == 1L) return "简历1";
            if (id == 3L) return "简历3";
            if (id == 30L) return "副本";
            return null;
        }

        @Override public Long findTargetJobDescriptionIdById(long userId, long id) {
            if (id == 1L) return 88L;
            if (id == 3L) return storedTargetJobId;
            return null;
        }

        @Override public String findKindById(long userId, long id) {
            return id == 30L ? "JOB_EXPRESSION" : "GENERAL";
        }

        @Override public Long findForkedFromVersionIdById(long userId, long id) {
            return id == 30L ? 10L : null;
        }

        @Override public java.time.LocalDateTime findArchivedAtById(long userId, long id) {
            return archivedWrites.get(id);
        }

        @Override public List<Long> findIdsByUserId(long userId, String kind, boolean archived) {
            this.listQueryKind = kind;
            this.listQueryArchived = archived;
            return List.of(1L, 30L);
        }

        @Override public long createForkedAsset(long userId, String title, long sourceVersionId,
                                                int sourceVersionNo, String contentJson) {
            this.forkCalls++;
            this.forkedSourceVersionId = sourceVersionId;
            this.forkedTitle = title;
            this.forkedContentJson = contentJson;
            return 30L;
        }

        @Override public ResumeVersionDTO findVersionByIdForUser(long userId, long versionId) {
            if (versionId == 10L) {
                return new ResumeVersionDTO(10L, 1L, null, 2, Map.of("old", "content"), "源", "user", LocalDateTime.now());
            }
            return null;
        }

        @Override public String findContentJsonById(long versionId) {
            return versionId == 10L ? "{\"old\":\"content\"}" : null;
        }

        @Override public void updateTitle(long userId, long resumeId, String title) {
            this.renamedResumeId = resumeId;
            this.renamedTitle = title;
        }

        @Override public void updateArchivedAt(long userId, long resumeId, java.time.LocalDateTime archivedAt) {
            this.archivedWrites.put(resumeId, archivedAt);
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

    @Nested
    @DisplayName("deleteResume 边界")
    class DeleteResume {

        @Test
        @DisplayName("存在的简历 → 删除成功并记录删除 id")
        void deleteExisting() {
            boolean result = resumeService.deleteResume(1L);
            assertThat(result).isTrue();
            assertThat(repo.softDeletedResumeId).isEqualTo(1L);
        }

        @Test
        @DisplayName("不存在的简历 → 返回 false")
        void deleteMissing() {
            boolean result = resumeService.deleteResume(999L);
            assertThat(result).isFalse();
            assertThat(repo.softDeletedResumeId).isEqualTo(999L);
        }
    }

    @Nested
    @DisplayName("forkVersion 边界")
    class ForkVersion {

        @Test
        @DisplayName("fork 成功 → 返回 JOB_EXPRESSION 资产，服务端复制正文")
        void forkSuccess() {
            ResumeDTO forked = resumeService.forkVersion(10L, new ForkResumeVersionRequest("岗位表达副本"));

            assertThat(forked.id()).isEqualTo(30L);
            assertThat(forked.kind()).isEqualTo("JOB_EXPRESSION");
            assertThat(forked.forkedFromVersionId()).isEqualTo(10L);
            assertThat(repo.forkedSourceVersionId).isEqualTo(10L);
            assertThat(repo.forkedTitle).isEqualTo("岗位表达副本");
            assertThat(repo.forkedContentJson).contains("old");
        }

        @Test
        @DisplayName("空白标题 → 拒绝")
        void rejectBlankTitle() {
            assertThatThrownBy(() -> resumeService.forkVersion(10L, new ForkResumeVersionRequest(" ")))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("简历名称");
            assertThat(repo.forkCalls).isZero();
        }

        @Test
        @DisplayName("跨用户/不存在版本 → 按不存在处理")
        void rejectMissingVersion() {
            assertThatThrownBy(() -> resumeService.forkVersion(999L, new ForkResumeVersionRequest("副本")))
                    .isInstanceOf(NoSuchElementException.class).hasMessageContaining("简历版本不存在");
        }
    }

    @Nested
    @DisplayName("rename / archive / restore 边界")
    class AssetLifecycle {

        @Test
        @DisplayName("改名成功")
        void renameSuccess() {
            ResumeDTO renamed = resumeService.renameResume(1L, new UpdateResumeAssetRequest("改名后"));
            assertThat(repo.renamedResumeId).isEqualTo(1L);
            assertThat(renamed.title()).isEqualTo("改名后");
        }

        @Test
        @DisplayName("归档与恢复幂等")
        void archiveAndRestore() {
            resumeService.archiveResume(1L);
            assertThat(repo.archivedWrites.get(1L)).isNotNull();
            resumeService.restoreResume(1L);
            assertThat(repo.archivedWrites.containsKey(1L)).isTrue();
        }

        @Test
        @DisplayName("跨用户 → 按不存在处理")
        void rejectMissing() {
            assertThatThrownBy(() -> resumeService.renameResume(999L, new UpdateResumeAssetRequest("x")))
                    .isInstanceOf(NoSuchElementException.class);
            assertThatThrownBy(() -> resumeService.archiveResume(999L))
                    .isInstanceOf(NoSuchElementException.class);
            assertThatThrownBy(() -> resumeService.restoreResume(999L))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("列表过滤参数透传，未知 kind 拒绝")
        void listFilters() {
            resumeService.listAssets("JOB_EXPRESSION", true);
            assertThat(repo.listQueryKind).isEqualTo("JOB_EXPRESSION");
            assertThat(repo.listQueryArchived).isTrue();
            assertThatThrownBy(() -> resumeService.listAssets("WRONG", null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("简历种类");
        }
    }
}
