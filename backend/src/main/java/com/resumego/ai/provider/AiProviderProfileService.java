package com.resumego.ai.provider;

import com.resumego.ai.AiRequest;
import com.resumego.ai.AiResult;
import com.resumego.ai.runtime.AiRuntimeRegistry;
import com.resumego.common.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

@Service
public class AiProviderProfileService {

    private static final Set<String> PROTOCOLS = Set.of("openai-compatible", "anthropic", "gemini");
    private final AiProviderProfileRepository repository;
    private final AiRuntimeRegistry registry;
    private final AiProviderProbeService probeService;

    public AiProviderProfileService(AiProviderProfileRepository repository, AiRuntimeRegistry registry,
                                    AiProviderProbeService probeService) {
        this.repository = repository;
        this.registry = registry;
        this.probeService = probeService;
    }

    public List<AiProviderProfileResponse> list() {
        return repository.findAll(CurrentUser.DEMO_USER_ID).stream().map(this::response).toList();
    }

    public AiProviderProfile getEntity(long id) {
        return repository.findById(CurrentUser.DEMO_USER_ID, id)
                .orElseThrow(() -> new NoSuchElementException("模型配置不存在"));
    }

    public AiProviderProfileResponse create(AiProviderProfileRequest input) {
        AiProviderProfileRequest request = normalize(input);
        long id = repository.create(CurrentUser.DEMO_USER_ID, request);
        return response(getEntity(id));
    }

    public AiProviderProfileResponse update(long id, AiProviderProfileRequest input) {
        AiProviderProfile existing = getEntity(id);
        AiProviderProfileRequest request = normalize(input);
        repository.update(CurrentUser.DEMO_USER_ID, id, request);
        // Keep the in-memory key when only presentation data changes. If the
        // endpoint/protocol/model changes, the old client must not be reused.
        if (registry.hasKey(id) && connectionFieldsChanged(existing, request)) {
            registry.clear(id);
        }
        return response(getEntity(id));
    }

    @Transactional
    public AiProviderProfileResponse setDefault(long id) {
        getEntity(id);
        // A default switch must never leave the previous provider active. The caller may
        // explicitly load the new profile's key after this transaction completes.
        AiRuntimeRegistry.ActiveRuntime active = registry.activeRuntime();
        if (active == null || active.profileId() != id) {
            registry.clearActive();
        }
        repository.clearDefault(CurrentUser.DEMO_USER_ID);
        repository.setDefault(CurrentUser.DEMO_USER_ID, id);
        return response(getEntity(id));
    }

    private boolean connectionFieldsChanged(AiProviderProfile existing, AiProviderProfileRequest request) {
        return !Objects.equals(existing.protocolType(), request.protocolType())
                || !Objects.equals(existing.baseUrl(), request.baseUrl())
                || !Objects.equals(existing.defaultModel(), request.defaultModel());
    }

    public void delete(long id) {
        getEntity(id);
        registry.clear(id);
        repository.delete(CurrentUser.DEMO_USER_ID, id);
    }

    public AiProviderProfileResponse apply(long id, String apiKey) {
        AiProviderProfile profile = getEntity(id);
        registry.apply(profile, apiKey);
        return response(profile);
    }

    public AiProviderProfileResponse clearRuntime(long id) {
        AiProviderProfile profile = getEntity(id);
        registry.clear(id);
        return response(profile);
    }

    public AiProviderProfileResponse test(long id) {
        AiProviderProfile profile = getEntity(id);
        if (!registry.hasKey(id)) throw new IllegalStateException("请先为此配置装载 API Key");
        AiResult result = registry.client().invoke(AiRequest.builder()
                .featureType("provider_connection_test")
                .userId(CurrentUser.DEMO_USER_ID)
                .promptVersion("provider-test-v1")
                .systemPrompt("Return exactly OK. Do not include any other text.")
                .userMessage("Connection test")
                .build());
        String message = result.success() ? "连接成功" : safeTestMessage(result);
        repository.recordTest(CurrentUser.DEMO_USER_ID, id, result.success(), message);
        AiProviderProfile tested = new AiProviderProfile(profile.id(), profile.userId(), profile.displayName(),
                profile.protocolType(), profile.baseUrl(), profile.defaultModel(), profile.defaultProfile(),
                LocalDateTime.now(), result.success() ? "success" : "failed", message,
                profile.createdAt(), LocalDateTime.now());
        return response(tested);
    }

    public AiProviderProbeResponse test(AiProviderProbeRequest input) {
        AiProviderProfile profile = transientProfile(input);
        return probeService.test(profile, input.apiKey());
    }

    public AiProviderProbeResponse models(AiProviderProbeRequest input) {
        AiProviderProfile profile = transientProfile(input);
        return probeService.models(profile, input.apiKey());
    }

    private AiProviderProfile transientProfile(AiProviderProbeRequest input) {
        if (input == null) throw new IllegalArgumentException("模型配置不能为空");
        // 探测（验证并继续 / 获取模型）发生在用户选择模型之前，因此这里不要求模型名称；
        // 已保存的配置在 create/update 中仍强制模型名称非空。
        AiProviderProfileRequest request = input.profileRequest();
        String name = trimRequired(request.displayName(), "配置名称", 80);
        String protocol = trimRequired(request.protocolType(), "协议", 32);
        String baseUrl = trimRequired(request.baseUrl(), "Base URL", 500).replaceAll("/+$", "");
        if (!PROTOCOLS.contains(protocol)) throw new IllegalArgumentException("不支持的模型协议");
        validateUrl(baseUrl);
        String model = request.defaultModel() == null ? "" : request.defaultModel().trim();
        LocalDateTime now = LocalDateTime.now();
        return new AiProviderProfile(-1L, CurrentUser.DEMO_USER_ID, name, protocol, baseUrl, model, false, null,
                null, null, now, now);
    }

    private String safeTestMessage(AiResult result) {
        return result.errorMessage() == null ? "连接失败" : result.errorMessage();
    }

    private AiProviderProfileRequest normalize(AiProviderProfileRequest input) {
        if (input == null) throw new IllegalArgumentException("模型配置不能为空");
        String name = trimRequired(input.displayName(), "配置名称", 80);
        String protocol = trimRequired(input.protocolType(), "协议", 32);
        String baseUrl = trimRequired(input.baseUrl(), "Base URL", 500).replaceAll("/+$", "");
        String model = trimRequired(input.defaultModel(), "模型名称", 120);
        if (!PROTOCOLS.contains(protocol)) throw new IllegalArgumentException("不支持的模型协议");
        validateUrl(baseUrl);
        return new AiProviderProfileRequest(name, protocol, baseUrl, model);
    }

    private String trimRequired(String value, String label, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + "不能为空");
        if (normalized.length() > maxLength) throw new IllegalArgumentException(label + "过长");
        return normalized;
    }

    private void validateUrl(String value) {
        try {
            URI uri = URI.create(value);
            boolean localhost = "localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost());
            if (!("https".equalsIgnoreCase(uri.getScheme())
                    || ("http".equalsIgnoreCase(uri.getScheme()) && localhost))) {
                throw new IllegalArgumentException("Base URL 必须使用 HTTPS；仅本机服务允许 HTTP");
            }
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("HTTPS")) throw exception;
            throw new IllegalArgumentException("Base URL 格式无效");
        }
    }

    private AiProviderProfileResponse response(AiProviderProfile profile) {
        return new AiProviderProfileResponse(profile.id(), profile.displayName(), profile.protocolType(),
                profile.baseUrl(), profile.defaultModel(), profile.defaultProfile(), registry.hasKey(profile.id()),
                profile.lastTestedAt(), profile.lastTestStatus(), profile.lastTestMessage());
    }
}
