package com.better.CommuteMate.domain.faq.controller;

import com.better.CommuteMate.domain.faq.entity.Faq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "FAQ", description = "FAQ 관련 API")
@RestController
@RequestMapping("/faq")
public class FaqController {

    @Operation(
            summary = "FAQ 작성",
            description = "새로운 FAQ를 작성하는 API입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FAQ 작성 성공"),
            @ApiResponse(responseCode = "400", description = "요청 데이터 오류", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
    })
    @PostMapping
    public ResponseEntity<?> saveFAQ(
            @RequestBody
            @Parameter(description = "작성할 FAQ 객체", required = true)
            Faq faq
    ) {
        // TODO: FAQ 작성 로직 구현
        return null;
    }

    @Operation(
            summary = "FAQ 수정",
            description = "특정 FAQ를 수정하는 API입니다.\n" +
                    "수정 시 기존 내용은 faq_history 테이블에 기록되고, faq 테이블은 최신 상태로 업데이트됩니다.\n" +
                    "삭제된 FAQ는 수정이 불가합니다.."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FAQ 수정 성공"),
            @ApiResponse(responseCode = "404", description = "FAQ를 찾을 수 없음", content = @Content),
            @ApiResponse(responseCode = "400", description = "요청 데이터 오류", content = @Content)
    })
    @PutMapping("/{faqId}")
    public ResponseEntity<?> updateFAQ(
            @Parameter(description = "수정할 FAQ ID", required = true)
            @PathVariable Long faqId,
            @RequestBody Faq faq
    ) {
        // TODO: FAQ 수정 로직 구현
        return null;
    }

    @Operation(
            summary = "FAQ 삭제",
            description = "특정 FAQ를 삭제 처리하는 API입니다.\n" +
                    "실제 데이터는 삭제하지 않고, deleted_flag를 true로 설정하고 deleted_at에 삭제 시간을 기록합니다.\n" +
                    "프론트에서는 해당 FAQ에 “삭제됨” 배지를 표시합니다"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FAQ 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "FAQ를 찾을 수 없음", content = @Content)
    })
    @DeleteMapping("/{faqId}")
    public ResponseEntity<?> deleteFAQ(
            @Parameter(description = "삭제할 FAQ ID", required = true)
            @PathVariable Long faqId
    ) {
        // TODO: FAQ 삭제 로직 구현
        return null;
    }

    @Operation(
            summary = "FAQ 상세 조회",
            description = "특정 FAQ의 상세 내용을 조회하는 API입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FAQ 조회 성공"),
            @ApiResponse(responseCode = "404", description = "FAQ를 찾을 수 없음", content = @Content)
    })
    @GetMapping("/{faqId}")
    public ResponseEntity<?> getFAQ(
            @Parameter(description = "조회할 FAQ ID", required = true)
            @PathVariable Long faqId
    ) {
        // TODO: FAQ 단건 조회 로직 구현
        return null;
    }

    @Operation(
            summary = "FAQ 목록 조회",
            description = "필터 조건에 따라 FAQ 목록을 조회하는 API입니다. 필터 옵션에는 최신순, 오래된순 등이 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FAQ 목록 조회 성공")
    })
    @GetMapping("/list")
    public ResponseEntity<List<Faq>> getFAQs(
            @Parameter(description = "정렬 또는 필터 조건 (예: 최신순, 오래된순)", example = "latest")
            @RequestParam(required = false) String filter
    ) {
        // TODO: FAQ 목록 조회 로직 구현
        return null;
    }

    @Operation(
            summary = "FAQ 검색 (검색어 기반)",
            description = "검색어와 날짜 범위를 이용하여 FAQ를 검색합니다. "
                    + "검색어는 제목, 내용, 작성자 이름 등을 기준으로 조회됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FAQ 검색 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터", content = @Content)
    })
    @GetMapping
    public ResponseEntity<?> searchFaqByKeyword(
            @Parameter(description = "검색 키워드 (예: '회원가입')", example = "로그인")
            @RequestParam(required = false) String searchkey,
            @Parameter(description = "검색 시작일 (yyyy-MM-dd)", example = "2025-01-01")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "검색 종료일 (yyyy-MM-dd)", example = "2025-12-31")
            @RequestParam(required = false) String endDate
    ) {
        // TODO: FAQ 검색 로직 구현 (검색어 + 날짜 범위)
        return null;
    }

    // Todo 이거 대분류만 들어왔을 경우, 대분류+날짜만 들어왔을 경우, 소분류만 선택했을 경우도 추가해야 할 듯.
    //  파라미터만 다르게 해서 오버로딩 쓰면 되지 않을까 라는 생각
    @Operation(
            summary = "FAQ 검색 (필터 기반)",
            description = "대분류(category), 소분류(subcategory), 날짜 범위를 조건으로 FAQ를 검색합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FAQ 필터 검색 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터", content = @Content)
    })
    @GetMapping("/filter")
    public ResponseEntity<?> searchFaqByFilter(
            @Parameter(description = "대분류명", example = "서비스이용")
            @RequestParam(required = false) String category,
            @Parameter(description = "소분류명", example = "회원정보")
            @RequestParam(required = false) String subcategory,
            @Parameter(description = "검색 시작일 (yyyy-MM-dd)", example = "2025-01-01")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "검색 종료일 (yyyy-MM-dd)", example = "2025-12-31")
            @RequestParam(required = false) String endDate
    ) {
        // TODO: FAQ 필터 검색 로직 구현 (카테고리 + 날짜 범위)
        return null;
    }
}