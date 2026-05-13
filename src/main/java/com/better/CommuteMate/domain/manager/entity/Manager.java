package com.better.CommuteMate.domain.manager.entity;

import com.better.CommuteMate.domain.organization.entity.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "manager")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Manager {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(length = 30)
    private String phonenum;

    public Manager(String name, Organization organization, String phonenum) {
        this.name = name;
        this.organization = organization;
        this.phonenum = phonenum;
    }
}

