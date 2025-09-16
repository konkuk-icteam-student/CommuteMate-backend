package com.better.CommuteMate.global.controller.dtos;

public abstract class ErrorResponseDetail extends ResponseDetail {

    public ErrorResponseDetail(String codeId, String codeName, String codeValue) {
        super(codeId, codeName, codeValue);
    }

}
