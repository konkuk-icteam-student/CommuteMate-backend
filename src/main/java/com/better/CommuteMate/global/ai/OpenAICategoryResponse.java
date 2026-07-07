package com.better.CommuteMate.global.ai;

import java.util.List;

public record OpenAICategoryResponse(
        List<Choice> choices
) {
    public record Choice(
            Message message
    ) {}

    public record Message(
            String content
    ) {}
}
