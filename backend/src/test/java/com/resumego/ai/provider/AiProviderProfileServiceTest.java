package com.resumego.ai.provider;

import com.resumego.ai.runtime.AiRuntimeRegistry;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class AiProviderProfileServiceTest {

    private final AiProviderProfileRepository repository = mock(AiProviderProfileRepository.class);
    private final AiRuntimeRegistry registry = mock(AiRuntimeRegistry.class);
    private final AiProviderProbeService probeService = mock(AiProviderProbeService.class);
    private final AiProviderProfileService service = new AiProviderProfileService(repository, registry, probeService);

    @Test
    void normalizesAndCreatesHttpsProviderProfile() {
        when(repository.create(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(7L);
        when(repository.findById(1L, 7L)).thenReturn(Optional.of(profile(7L)));

        service.create(new AiProviderProfileRequest(
                "  DeepSeek  ", "openai-compatible", "https://api.deepseek.com/ ", " deepseek-chat "
        ));

        verify(repository).create(1L, new AiProviderProfileRequest(
                "DeepSeek", "openai-compatible", "https://api.deepseek.com", "deepseek-chat"
        ));
    }

    @Test
    void rejectsRemotePlainHttpEndpoint() {
        assertThatThrownBy(() -> service.create(new AiProviderProfileRequest(
                "Unsafe", "openai-compatible", "http://api.example.com/v1", "model"
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTPS");
    }

    @Test
    void testsAnUnsavedNormalizedProfileWithoutPersistingIt() {
        AiProviderProbeResponse expected = new AiProviderProbeResponse(true, "连接成功", java.util.List.of());
        when(probeService.test(any(), eq("temporary-key"))).thenReturn(expected);

        AiProviderProbeResponse actual = service.test(new AiProviderProbeRequest(
                "  Local  ", "openai-compatible", "https://provider.example/v1/", " model-a ", "temporary-key"
        ));

        org.assertj.core.api.Assertions.assertThat(actual).isEqualTo(expected);
        verify(probeService).test(org.mockito.ArgumentMatchers.argThat(profile ->
                profile.displayName().equals("Local") && profile.baseUrl().equals("https://provider.example/v1")
                        && profile.defaultModel().equals("model-a")), eq("temporary-key"));
    }

    @Test
    void probesAnUnsavedProfileBeforeAModelHasBeenChosen() {
        AiProviderProbeResponse expected = new AiProviderProbeResponse(true, "已获取 2 个模型", java.util.List.of("a", "b"));
        when(probeService.models(any(), eq("temporary-key"))).thenReturn(expected);

        AiProviderProbeResponse actual = service.models(new AiProviderProbeRequest(
                "DeepSeek", "openai-compatible", "https://api.deepseek.com/v1", "", "temporary-key"
        ));

        org.assertj.core.api.Assertions.assertThat(actual).isEqualTo(expected);
        verify(probeService).models(org.mockito.ArgumentMatchers.argThat(profile ->
                profile.displayName().equals("DeepSeek") && profile.defaultModel().isEmpty()), eq("temporary-key"));
    }

    @Test
    void settingDefaultFirstClearsTheExistingDefault() {
        when(repository.findById(1L, 4L)).thenReturn(Optional.of(profile(4L)));

        service.setDefault(4L);

        verify(repository).clearDefault(1L);
        verify(repository).setDefault(1L, 4L);
        verify(registry).clearActive();
    }

    private AiProviderProfile profile(long id) {
        return new AiProviderProfile(id, 1L, "DeepSeek", "openai-compatible",
                "https://api.deepseek.com", "deepseek-chat", false,
                null, null, null, java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
    }
}
