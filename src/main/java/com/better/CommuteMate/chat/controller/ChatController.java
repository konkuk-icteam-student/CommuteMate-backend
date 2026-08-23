package com.better.CommuteMate.chat.controller;

import com.better.CommuteMate.chat.application.dto.request.PostChatQueryRequest;
import com.better.CommuteMate.chat.application.service.ChatService;
import com.better.CommuteMate.global.controller.dtos.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chat", description = "챗봇(RAG) 관련 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(
            summary = "챗봇 질의",
            description = "규정과 FAQ에 대해 자유 형식으로 질문하면 RAG 기반 챗봇이 답변합니다. "
                    + "LLM 호출이 포함되어 응답까지 최대 약 120초가 걸릴 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "챗봇 질의 성공",
                    content = @Content(schema = @Schema(implementation = com.better.CommuteMate.chat.application.dto.response.PostChatQueryResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @PostMapping("/query")
    public Response query(@RequestBody @Valid PostChatQueryRequest request) {
        return new Response(
                true,
                "챗봇 질의 성공",
                chatService.query(request.query())
        );
    }
}
