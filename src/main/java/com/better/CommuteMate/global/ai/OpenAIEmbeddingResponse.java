package com.better.CommuteMate.global.ai;

import java.util.List;

public record OpenAIEmbeddingResponse(
        List<EmbeddingData> data
) {
    public record EmbeddingData(
            List<Float> embedding
    ) {}
}