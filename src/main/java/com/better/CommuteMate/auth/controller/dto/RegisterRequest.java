package com.better.CommuteMate.auth.controller.dto;

import com.better.CommuteMate.global.code.CodeType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 4, max = 16, message = "비밀번호는 4자 이상 16자 이하로 입력해주세요")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "비밀번호는 영어와 숫자만 사용할 수 있습니다")
    private String password;

    @NotBlank
    private String name;

    @NotNull
    private CodeType roleCode;

    private Integer organizationId;

}
