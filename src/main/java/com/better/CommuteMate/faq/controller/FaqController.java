package com.better.CommuteMate.faq.controller;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.category.application.dto.response.GetCategoryListWrapper;
import com.better.CommuteMate.faq.application.dto.request.FaqSearchScope;
import com.better.CommuteMate.faq.application.dto.request.PostFaqRequest;
import com.better.CommuteMate.faq.application.dto.request.PutFaqUpdateRequest;
import com.better.CommuteMate.faq.application.FaqService;
import com.better.CommuteMate.faq.application.dto.response.GetFaqDetailResponse;
import com.better.CommuteMate.faq.application.dto.response.GetFaqListWrapper;
import com.better.CommuteMate.global.controller.dtos.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import org.springframework.web.multipart.MultipartFile;


@Tag(name = "FAQ", description = "FAQ 관련 API")
@RestController
@RequestMapping("/api/faq")
@RequiredArgsConstructor
public class FaqController {

    private final FaqService faqService;

    @Operation(
            summary = "FAQ 작성",
            description = "FAQ 작성을 위한 API입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FAQ 작성 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> createFaq(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid PostFaqRequest request
    ) {
        return ResponseEntity.ok(new Response(true, "FAQ 작성 성공", faqService.createFaq(userDetails.getUserId(), request)));
    }

    @Operation(
            summary = "FAQ 수정",
            description = """
                    특정 FAQ를 수정하는 API입니다.
                    수정 시 기존 내용은 faq_history 테이블에 기록되고,
                    faq 테이블은 최신 상태로 업데이트됩니다.
                    삭제된 FAQ(deleted_flag = true)는 수정할 수 없습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FAQ 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "FAQ를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "삭제된 FAQ는 수정 불가")
    })
    @PutMapping("/{faqId}")
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> updateFaq(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long faqId,
            @RequestBody PutFaqUpdateRequest request
    ) {
        return ResponseEntity.ok(new Response(true, "FAQ 수정 성공", faqService.updateFaq(userDetails.getUserId(), faqId, request)));
    }

    @Operation(
            summary = "FAQ 삭제",
            description = """
                    특정 FAQ를 삭제 처리하는 API입니다.
                    실제 데이터는 삭제하지 않고, deleted_flag를 true로 설정하고 deleted_at에 삭제 시간을 기록합니다.
                    프론트에서는 해당 FAQ에 “삭제됨” 배지를 표시합니다
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FAQ 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "FAQ를 찾을 수 없음", content = @Content),
            @ApiResponse(responseCode = "400", description = "이미 삭제된 FAQ", content = @Content)
    })
    @DeleteMapping("/{faqId}")
    public ResponseEntity<Response> deleteFAQ(
            @Parameter(description = "삭제할 FAQ ID", required = true)
            @PathVariable Long faqId
    ) {
        faqService.deleteFaq(faqId);
        return ResponseEntity.ok(new Response(true, "FAQ 삭제 성공", null));
    }

    @Operation(
            summary = "FAQ 상세 조회",
            description = """
                    FAQ ID와 날짜를 기준으로
                    해당 날짜에 해당하는 FAQ 수정 이력을 조회합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FAQ 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = GetFaqDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "FAQ를 찾을 수 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "해당 날짜의 FAQ 기록을 찾을 수 없음", content = @Content),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content)
    })
    @GetMapping("/{faqId}")
    public ResponseEntity<Response> getFaqDetail(
            @Parameter(description = "조회할 FAQ ID", required = true)
            @PathVariable Long faqId,

            @Parameter(description = "조회할 날짜 (yyyy-MM-dd)", required = true)
            @RequestParam LocalDate date
    ) {
        return ResponseEntity.ok(new Response(true, "FAQ 상세 조회 성공", faqService.getFaqDetailByDate(faqId, date)));
    }

    @Operation(
            summary = "FAQ 목록 조회",
            description = """
                    필터 조건에 따라 FAQ 목록을 조회하는 API입니다.
                    필터 옵션에는 소속, 분류, 검색 범위(제목+내용, 제목, 내용, 작성자), 날짜가 있습니다.
                    기본적으로 최신순 정렬입니다.
                    페이지 단위로 조회하고 페이지당 10개씩 조회됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FAQ 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = GetFaqListWrapper.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @GetMapping
    public ResponseEntity<Response> getFaqList(
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) FaqSearchScope searchScope,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok(new Response(true, "FAQ 목록 조회 성공", faqService.getFaqList(teamId, categoryId, keyword, searchScope, startDate, endDate, page)));
    }

    @Operation(
            summary = "FAQ 이미지 업로드",
            description = """
                FAQ 작성 중 본문에 삽입할 이미지를 업로드하는 API입니다.
                업로드된 이미지는 즉시 서버 로컬 스토리지에 저장되며 faq_id가 없는 상태(null)로 DB에 저장됩니다.
                응답으로 받은 URL을 <img src="..."> 형태로 사용하면 됩니다.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이미지 업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping(value = "/images", consumes = "multipart/form-data")
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> uploadFaqImage(
            @Parameter(description = "업로드할 이미지 파일")
            @RequestPart MultipartFile imageFile
    ) {
        return ResponseEntity.ok(
                new Response(true, "이미지 업로드 성공", faqService.uploadFaqImage(imageFile))
        );
    }


    @Operation(
            summary = "FAQ 파일 업로드",
            description = """
              FAQ에 첨부할 파일을 업로드하는 API입니다.
              업로드된 파일은 서버 로컬 스토리지에 저장되며 게시글과 연결되지 않은 상태(faq_id = null)로 DB에 저장됩니다.
              응답으로 받은 URL은 FAQ 생성 시 함께 전달하여 첨부 파일로 등록됩니다.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "파일 업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping(value = "/files", consumes = "multipart/form-data")
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Response> uploadFaqFile(
            @Parameter(description = "업로드할 파일")
            @RequestPart MultipartFile file
    ) {
        return ResponseEntity.ok(
                new Response(true, "파일 업로드 성공", faqService.uploadFaqFile(file))
        );
    }
}