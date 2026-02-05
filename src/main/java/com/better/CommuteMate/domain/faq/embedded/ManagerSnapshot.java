package com.better.CommuteMate.domain.faq.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ManagerSnapshot {

    @Column(name = "manager_name", length = 30, nullable = false)
    private String managerName;

    @Column(name = "team_name", length = 30, nullable = false)
    private String teamName;

    @Column(name = "category_name", length = 100, nullable = false)
    private String categoryName;
}