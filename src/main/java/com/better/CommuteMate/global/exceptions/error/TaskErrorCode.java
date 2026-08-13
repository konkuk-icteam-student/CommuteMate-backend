package com.better.CommuteMate.global.exceptions.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TaskErrorCode implements CustomErrorCode {

    // Task 관련 에러
    TASK_NOT_FOUND(
            "해당 업무를 찾을 수 없습니다.",
            "[Error] : Task를 찾을 수 없습니다.",
            HttpStatus.NOT_FOUND),
    INVALID_TASK_TIME(
            "업무 시간이 유효하지 않습니다.",
            "[Error] : 업무 시간 유효성 검증 실패",
            HttpStatus.BAD_REQUEST),
    INVALID_TASK_DATE(
            "업무 날짜가 유효하지 않습니다.",
            "[Error] : 업무 날짜 유효성 검증 실패",
            HttpStatus.BAD_REQUEST),
    INVALID_TASK_TYPE(
            "업무 유형이 유효하지 않습니다. TT01(정기) 또는 TT02(비정기)를 사용해주세요.",
            "[Error] : 업무 유형 유효성 검증 실패",
            HttpStatus.BAD_REQUEST),
    ASSIGNEE_NOT_FOUND(
            "담당자를 찾을 수 없습니다.",
            "[Error] : 담당자(User)를 찾을 수 없습니다.",
            HttpStatus.NOT_FOUND),

    // Template 관련 에러
    TEMPLATE_NOT_FOUND(
            "해당 템플릿을 찾을 수 없습니다.",
            "[Error] : TaskTemplate을 찾을 수 없습니다.",
            HttpStatus.NOT_FOUND),
    TEMPLATE_NAME_ALREADY_EXISTS(
            "이미 존재하는 템플릿 이름입니다.",
            "[Error] : 템플릿 이름 중복 발생",
            HttpStatus.CONFLICT),
    TEMPLATE_ITEM_NOT_FOUND(
            "해당 템플릿 항목을 찾을 수 없습니다.",
            "[Error] : TaskTemplateItem을 찾을 수 없습니다.",
            HttpStatus.NOT_FOUND),
    TEMPLATE_HAS_NO_ITEMS(
            "템플릿에 업무 항목이 없습니다.",
            "[Error] : 템플릿에 항목이 없어 적용할 수 없습니다.",
            HttpStatus.BAD_REQUEST),

    // Batch 관련 에러
    BATCH_UPDATE_PARTIAL_FAILURE(
            "일부 업무 저장에 실패했습니다.",
            "[Error] : 배치 업데이트 중 일부 실패 발생",
            HttpStatus.MULTI_STATUS),
    BATCH_UPDATE_ALL_FAILURE(
            "모든 업무 저장에 실패했습니다.",
            "[Error] : 배치 업데이트 전체 실패",
            HttpStatus.UNPROCESSABLE_ENTITY);

    private final String message;
    private final String logMessage;
    private final HttpStatus status;

    @Override
    public String getName() {
        return this.name();
    }
}
