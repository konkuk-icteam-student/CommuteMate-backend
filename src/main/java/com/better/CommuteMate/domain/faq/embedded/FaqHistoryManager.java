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
public class FaqHistoryManager {

    @Column(name = "manager_name", length = 30, nullable = false)
    private String managerName;

    @Column(name = "organization_name", length = 30, nullable = false)
    private String organizationName;

    @Column(name = "category_name", length = 100, nullable = false)
    private String categoryName;
}