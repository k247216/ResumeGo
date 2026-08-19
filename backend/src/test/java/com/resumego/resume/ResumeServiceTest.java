package com.resumego.resume;

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
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ResumeService 单元测试")
class ResumeServiceTest {

    private FakeResumeRepository resumeRepository;
    private ResumeService resumeService;

    @BeforeEach
    void setUp() {
        resumeRepository = new FakeResumeRepository();
        resumeService = new ResumeService(resumeRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("createManualVersion: 人工编辑后创建新版本并更新当前版本")
    void shouldCreateManualVersion() {
        Map<String, Object> content = Map.of(
                "basicInfo", Map.of("name", "Demo User"),
                "skills", List.of("Java", "Vue")
        );

        ResumeVersionDTO result = resumeService.createManualVersion(
                1L,
                new CreateResumeVersionRequest(content, "人工编辑：优化项目描述")
        );

        assertThat(result.id()).isEqualTo(12L);
        assertThat(result.resumeId()).isEqualTo(1L);
        assertThat(result.parentVersionId()).isEqualTo(10L);
        assertThat(result.versionNo()).isEqualTo(3);
        assertThat(result.createdByType()).isEqualTo("user");
        assertThat(resumeRepository.updatedResumeId).isEqualTo(1L);
        assertThat(resumeRepository.updatedVersionId).isEqualTo(12L);
        assertThat(resumeRepository.insertedContentJson).contains("\"skills\"");
    }

    @Test
    @DisplayName("createManualVersion: 根据项目 evidenceId 同步简历证据引用表")
    void shouldSyncEvidenceRefsWhenCreatingManualVersion() {
        Map<String, Object> content = Map.of(
                "basicInfo", Map.of("name", "Demo User"),
                "projects", List.of(
                        Map.of("title", "项目A", "evidenceId", 1),
                        Map.of("title", "项目B"),
                        Map.of("title", "项目C", "evidenceId", 5)
                )
        );

        resumeService.createManualVersion(
                1L,
                new CreateResumeVersionRequest(content, "关联项目证据")
        );

        assertThat(resumeRepository.syncedVersionId).isEqualTo(12L);
        assertThat(resumeRepository.syncedUserId).isEqualTo(1L);
        assertThat(resumeRepository.syncedEvidenceRefs)
                .containsExactly(
                        new ResumeRepository.EvidenceRefDraft(1L, "projects[0]"),
                        new ResumeRepository.EvidenceRefDraft(101L, "projects[1]"),
                        new ResumeRepository.EvidenceRefDraft(5L, "projects[2]")
                );
    }

    @Test
    @DisplayName("createManualVersion: 项目经历未手动关联证据时自动生成自述证据并绑定")
    void shouldCreateSelfReportedEvidenceForProjectWithoutEvidenceId() {
        Map<String, Object> content = Map.of(
                "basicInfo", Map.of("name", "Demo User"),
                "projects", List.of(
                        Map.of(
                                "title", "校园二手交易小程序",
                                "description", "负责商品发布、关键词搜索、收藏列表和订单状态管理模块",
                                "techStack", List.of("Spring Boot", "MySQL", "Vue")
                        )
                )
        );

        resumeService.createManualVersion(
                1L,
                new CreateResumeVersionRequest(content, "保存项目经历")
        );

        assertThat(resumeRepository.createdProjectEvidenceDrafts)
                .containsExactly(new ResumeRepository.ProjectEvidenceDraft(
                        "校园二手交易小程序",
                        "来源：简历项目经历「校园二手交易小程序」",
                        "负责商品发布、关键词搜索、收藏列表和订单状态管理模块",
                        null,
                        List.of("Spring Boot", "MySQL", "Vue"),
                        "系统根据简历项目经历自动生成的自述型证据"
                ));
        assertThat(resumeRepository.insertedContentJson).contains("\"evidenceId\":101");
        assertThat(resumeRepository.syncedEvidenceRefs)
                .containsExactly(new ResumeRepository.EvidenceRefDraft(101L, "projects[0]"));
    }

    @Test
    @DisplayName("createManualVersion: 项目经历关联的证据失效时自动替换为新的自述证据")
    void shouldReplaceInvalidProjectEvidenceIdWithSelfReportedEvidence() {
        Map<String, Object> content = Map.of(
                "basicInfo", Map.of("name", "Demo User"),
                "projects", List.of(
                        Map.of(
                                "title", "AI 校园助手原型",
                                "description", "后端使用 Spring Boot 搭建 API 服务，集成通义千问 API",
                                "techStack", List.of("Spring Boot", "通义千问"),
                                "evidenceId", 999
                        )
                )
        );

        resumeService.createManualVersion(
                1L,
                new CreateResumeVersionRequest(content, "替换失效证据")
        );

        assertThat(resumeRepository.createdProjectEvidenceDrafts).hasSize(1);
        assertThat(resumeRepository.insertedContentJson).contains("\"evidenceId\":101");
        assertThat(resumeRepository.syncedEvidenceRefs)
                .containsExactly(new ResumeRepository.EvidenceRefDraft(101L, "projects[0]"));
    }

    @Test
    @DisplayName("createManualVersion: 空内容不允许创建版本")
    void shouldRejectEmptyContent() {
        assertThatThrownBy(() -> resumeService.createManualVersion(
                1L,
                new CreateResumeVersionRequest(Map.of(), "空内容")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("简历内容不能为空");
    }

    @Test
    @DisplayName("createManualVersion: 超长内容（>50K 字符）拒绝创建")
    void shouldRejectOversizedContent() {
        // 构建一个超长字符串（50K+1 字符）
        StringBuilder sb = new StringBuilder(50_001);
        for (int i = 0; i < 50_001; i++) {
            sb.append('x');
        }
        Map<String, Object> content = Map.of("text", sb.toString());

        assertThatThrownBy(() -> resumeService.createManualVersion(
                1L,
                new CreateResumeVersionRequest(content, "超长内容")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("简历内容过长");
    }

    @Test
    @DisplayName("createManualVersion: null 请求拒绝")
    void shouldRejectNullRequest() {
        assertThatThrownBy(() -> resumeService.createManualVersion(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("简历内容不能为空");
    }

    @Test
    @DisplayName("createResume: 创建新简历时保存目标岗位绑定")
    void shouldCreateResumeWithTargetJobBinding() {
        Map<String, Object> content = Map.of(
                "basicInfo", Map.of("name", "Demo User"),
                "projects", List.of(Map.of("name", "ResumeGo"))
        );

        ResumeDTO result = resumeService.createResume(
                new CreateResumeRequest("后端实习简历", content, "创建新简历", 88L)
        );

        assertThat(resumeRepository.insertedResumeTargetJobId).isEqualTo(88L);
        assertThat(result.id()).isEqualTo(3L);
        assertThat(result.targetJobDescriptionId()).isEqualTo(88L);
    }

    @Test
    @DisplayName("createResume: 创建初始版本时同步项目证据引用表")
    void shouldSyncEvidenceRefsWhenCreatingResume() {
        Map<String, Object> content = Map.of(
                "basicInfo", Map.of("name", "Demo User"),
                "projects", List.of(Map.of("name", "ResumeGo", "evidenceId", 3))
        );

        resumeService.createResume(
                new CreateResumeRequest("后端实习简历", content, "创建新简历", 88L)
        );

        assertThat(resumeRepository.syncedVersionId).isEqualTo(12L);
        assertThat(resumeRepository.syncedUserId).isEqualTo(1L);
        assertThat(resumeRepository.syncedEvidenceRefs)
                .containsExactly(new ResumeRepository.EvidenceRefDraft(3L, "projects[0]"));
    }

    @Test
    @DisplayName("createResume: 空白名称拒绝创建且不写入仓储")
    void shouldRejectBlankTitleWithoutWritingRepository() {
        assertThatThrownBy(() -> resumeService.createResume(
                new CreateResumeRequest("  ", Map.of("basicInfo", Map.of("name", "Demo")), null, null)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("简历名称不能为空");

        assertThat(resumeRepository.insertResumeCalls).isZero();
        assertThat(resumeRepository.insertVersionCalls).isZero();
    }

    @Test
    @DisplayName("createManualVersion: 不存在的简历拒绝创建且不写入版本")
    void shouldRejectManualVersionForMissingResume() {
        assertThatThrownBy(() -> resumeService.createManualVersion(
                999L,
                new CreateResumeVersionRequest(Map.of("basicInfo", Map.of("name", "Demo")), "修改")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("简历不存在");

        assertThat(resumeRepository.insertVersionCalls).isZero();
    }

    @Test
    @DisplayName("createManualVersion: 空白变更说明使用默认摘要")
    void shouldUseDefaultSummaryForBlankManualVersionSummary() {
        resumeService.createManualVersion(
                1L,
                new CreateResumeVersionRequest(Map.of("basicInfo", Map.of("name", "Demo")), " ")
        );

        assertThat(resumeRepository.insertedChangeSummary).isEqualTo("人工编辑生成新版本");
    }

    @Test
    @DisplayName("updateTargetJob: 仅存在的简历可以更新目标岗位")
    void shouldUpdateTargetJobForExistingResume() {
        ResumeDTO result = resumeService.updateTargetJob(1L, new UpdateResumeTargetJobRequest(66L));

        assertThat(resumeRepository.updatedTargetJobResumeId).isEqualTo(1L);
        assertThat(resumeRepository.updatedTargetJobId).isEqualTo(66L);
        assertThat(result.targetJobDescriptionId()).isEqualTo(66L);
    }

    @Test
    @DisplayName("updateTargetJob 与 getVersions: 不存在的简历返回稳定异常")
    void shouldRejectMissingResumeForTargetJobAndVersionList() {
        assertThatThrownBy(() -> resumeService.updateTargetJob(999L, new UpdateResumeTargetJobRequest(66L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("简历不存在");
        assertThatThrownBy(() -> resumeService.getVersions(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("简历不存在");
    }

    @Test
    @DisplayName("getVersion: 不存在的版本返回稳定异常")
    void shouldRejectMissingVersion() {
        assertThatThrownBy(() -> resumeService.getVersion(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("简历版本不存在");
    }

    private static class FakeResumeRepository extends ResumeRepository {
        private String insertedContentJson;
        private Long updatedResumeId;
        private Long updatedVersionId;
        private Long insertedResumeTargetJobId;
        private int insertResumeCalls;
        private int insertVersionCalls;
        private String insertedChangeSummary;
        private Long updatedTargetJobResumeId;
        private Long updatedTargetJobId;
        private ResumeVersionDTO insertedVersion;
        private Long syncedVersionId;
        private Long syncedUserId;
        private List<ResumeRepository.EvidenceRefDraft> syncedEvidenceRefs = List.of();
        private List<ResumeRepository.ProjectEvidenceDraft> createdProjectEvidenceDrafts = new ArrayList<>();
        private Set<Long> validEvidenceIds = Set.of(1L, 3L, 5L);

        FakeResumeRepository() {
            super(null, new ObjectMapper());
        }

        @Override
        public String findTitleById(long id) {
            if (id == 1L) return "我的简历";
            if (id == 3L) return "后端实习简历";
            return null;
        }

        @Override
        public Long findTargetJobDescriptionIdById(long id) {
            if (id == 3L) return insertedResumeTargetJobId;
            if (id == 1L) return updatedTargetJobId;
            return null;
        }

        @Override
        public long insertResume(long userId, String title, Long targetJobDescriptionId) {
            this.insertResumeCalls++;
            this.insertedResumeTargetJobId = targetJobDescriptionId;
            return 3L;
        }

        @Override
        public Long findCurrentVersionId(long resumeId) {
            return 10L;
        }

        @Override
        public int findMaxVersionNo(long resumeId) {
            return 2;
        }

        @Override
        public long insertVersion(long resumeId, Long parentVersionId, int versionNo,
                                  String contentJson, String changeSummary, String createdByType) {
            this.insertVersionCalls++;
            this.insertedContentJson = contentJson;
            this.insertedChangeSummary = changeSummary;
            this.insertedVersion = new ResumeVersionDTO(
                    12L,
                    resumeId,
                    parentVersionId,
                    versionNo,
                    Map.of("saved", true),
                    changeSummary,
                    createdByType,
                    LocalDateTime.now()
            );
            return 12L;
        }

        @Override
        public void updateCurrentVersionId(long resumeId, long versionId) {
            this.updatedResumeId = resumeId;
            this.updatedVersionId = versionId;
        }

        @Override
        public void updateTargetJobDescriptionId(long resumeId, Long jobDescriptionId) {
            this.updatedTargetJobResumeId = resumeId;
            this.updatedTargetJobId = jobDescriptionId;
        }

        @Override
        public void replaceEvidenceRefsForVersion(long resumeVersionId, long userId,
                                                  List<EvidenceRefDraft> evidenceRefs) {
            this.syncedVersionId = resumeVersionId;
            this.syncedUserId = userId;
            this.syncedEvidenceRefs = evidenceRefs;
        }

        @Override
        public long createSelfReportedProjectEvidence(long userId, ProjectEvidenceDraft evidence) {
            this.createdProjectEvidenceDrafts.add(evidence);
            return 101L;
        }

        @Override
        public boolean existsActiveEvidenceForUser(long userId, long evidenceId) {
            return validEvidenceIds.contains(evidenceId);
        }

        @Override
        public List<ResumeVersionDTO> findVersionsByResumeId(long resumeId) {
            return insertedVersion == null ? List.of() : List.of(insertedVersion);
        }

        @Override
        public ResumeVersionDTO findVersionById(long versionId) {
            return insertedVersion;
        }
    }
}
