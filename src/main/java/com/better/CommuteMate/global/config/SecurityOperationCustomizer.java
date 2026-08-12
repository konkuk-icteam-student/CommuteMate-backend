package com.better.CommuteMate.global.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

/**
 * @SecurityRequirement(name = "JWT")가 선언된 모든 오퍼레이션에 401 응답을 자동으로 추가합니다.
 */
@Component
public class SecurityOperationCustomizer implements OperationCustomizer {

    private static final String UNAUTHORIZED_EXAMPLE =
            "{\"isSuccess\":false,\"message\":\"인증이 필요합니다.\",\"details\":null}";

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        if (!hasJwtSecurityRequirement(operation)) {
            return operation;
        }

        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }

        if (!operation.getResponses().containsKey("401")) {
            operation.getResponses().addApiResponse("401", buildUnauthorizedResponse());
        }

        return operation;
    }

    private boolean hasJwtSecurityRequirement(Operation operation) {
        if (operation.getSecurity() == null) {
            return false;
        }
        return operation.getSecurity().stream()
                .anyMatch(req -> req.containsKey("JWT"));
    }

    private ApiResponse buildUnauthorizedResponse() {
        Schema<?> schema = new Schema<>();
        schema.setExample(UNAUTHORIZED_EXAMPLE);

        MediaType mediaType = new MediaType().schema(schema);
        Content content = new Content().addMediaType("application/json", mediaType);

        return new ApiResponse()
                .description("인증되지 않은 사용자")
                .content(content);
    }
}
