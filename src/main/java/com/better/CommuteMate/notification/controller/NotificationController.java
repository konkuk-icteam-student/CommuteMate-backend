package com.better.CommuteMate.notification.controller;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.notification.application.NotificationService;
import com.better.CommuteMate.notification.controller.dtos.CheckNotificationResponse;
import com.better.CommuteMate.notification.controller.dtos.NewNotificationResponse;
import com.better.CommuteMate.notification.controller.dtos.NotificationListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "알림", description = "사용자 알림 조회 API")
@SecurityRequirement(name = "JWT")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(
            summary = "알림 목록 조회",
            description = "로그인한 사용자의 알림 목록을 생성 시각 기준 최신순으로 조회합니다. " +
                    "새 알림 여부(isNew)는 마지막 알림함 확인 시각을 기준으로 계산하며, " +
                    "확인 이력이 없는 경우 모든 알림을 새 알림으로 처리합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "알림 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "알림 있음", value = SUCCESS_EXAMPLE),
                                    @ExampleObject(name = "알림 없음", value = EMPTY_EXAMPLE)
                            }
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청", content = @Content)
    })
    public ResponseEntity<Response> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        NotificationListResponse details = notificationService.getNotifications(
                userDetails.getUserId()
        );
        return ResponseEntity.ok(Response.of(
                true,
                "알림 목록을 조회했습니다.",
                details
        ));
    }

    @GetMapping("/new")
    @Operation(
            summary = "새 알림 여부 조회",
            description = "마지막 알림함 확인 시각 이후 생성된 알림의 존재 여부와 개수를 반환합니다. " +
                    "알림 목록 전체를 조회하지 않고 count 쿼리로만 처리하며, " +
                    "확인 이력이 없는 경우 사용자의 모든 알림을 새 알림으로 계산합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "새 알림 여부 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "새 알림 있음", value = NEW_NOTIFICATION_EXAMPLE),
                                    @ExampleObject(name = "새 알림 없음", value = NO_NEW_NOTIFICATION_EXAMPLE)
                            }
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청", content = @Content)
    })
    public ResponseEntity<Response> getNewNotificationStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        NewNotificationResponse details = notificationService.getNewNotificationStatus(
                userDetails.getUserId()
        );
        return ResponseEntity.ok(Response.of(
                true,
                "새 알림 여부를 조회했습니다.",
                details
        ));
    }

    @PatchMapping("/check")
    @Operation(
            summary = "알림 확인 시각 갱신",
            description = "사용자가 알림함에 진입한 시점의 서버 시각을 마지막 알림함 확인 시각으로 저장합니다. " +
                    "기존 확인 상태 데이터가 없으면 새로 생성하며, " +
                    "이후 새 알림 여부(GET /api/v1/notifications/new)는 갱신된 시각을 기준으로 계산됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "알림 확인 시각 갱신 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = CHECK_EXAMPLE)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청", content = @Content)
    })
    public ResponseEntity<Response> checkNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CheckNotificationResponse details = notificationService.checkNotification(
                userDetails.getUserId()
        );
        return ResponseEntity.ok(Response.of(
                true,
                "알림 확인 시간이 갱신되었습니다.",
                details
        ));
    }

    private static final String CHECK_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "알림 확인 시간이 갱신되었습니다.",
              "details": {
                "lastCheckedAt": "2026-04-10T14:00:00"
              }
            }
            """;

    private static final String NEW_NOTIFICATION_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "새 알림 여부를 조회했습니다.",
              "details": {
                "hasNewNotification": true,
                "newNotificationCount": 3
              }
            }
            """;

    private static final String NO_NEW_NOTIFICATION_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "새 알림 여부를 조회했습니다.",
              "details": {
                "hasNewNotification": false,
                "newNotificationCount": 0
              }
            }
            """;

    private static final String SUCCESS_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "알림 목록을 조회했습니다.",
              "details": {
                "notifications": [
                  {
                    "notificationId": "550e8400-e29b-41d4-a716-446655440000",
                    "typeCode": "NT02",
                    "typeName": "근무 변경 요청 거절",
                    "title": "근무 시간 수정이 거절되었습니다.",
                    "content": "4월 6일 13:00-14:30 (1.5h)",
                    "refId": "9a1b2c3d-e29b-41d4-a716-446655440000",
                    "createdAt": "2026-03-20T16:21:00",
                    "isNew": true
                  },
                  {
                    "notificationId": "660e8400-e29b-41d4-a716-446655440001",
                    "typeCode": "NT01",
                    "typeName": "근무 변경 요청 승인",
                    "title": "근무 시간 수정이 승인되었습니다.",
                    "content": "4월 9일 13:00-14:30 (1.5h)",
                    "refId": "8b2c3d4e-e29b-41d4-a716-446655440001",
                    "createdAt": "2026-03-19T10:00:00",
                    "isNew": false
                  }
                ]
              }
            }
            """;

    private static final String EMPTY_EXAMPLE = """
            {
              "isSuccess": true,
              "message": "알림 목록을 조회했습니다.",
              "details": {
                "notifications": []
              }
            }
            """;
}
