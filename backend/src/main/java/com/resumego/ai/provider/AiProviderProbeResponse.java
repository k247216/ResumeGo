package com.resumego.ai.provider;

import java.util.List;

public record AiProviderProbeResponse(boolean success, String message, List<String> models) {
}
