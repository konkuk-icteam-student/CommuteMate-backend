package com.better.CommuteMate.domain.code.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CodeSubId implements Serializable {

    private String majorCode;
    private String subCode;
}