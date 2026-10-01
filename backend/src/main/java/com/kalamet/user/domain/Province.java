package com.kalamet.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Reference data (Iran's 31 provinces), loaded by V1. Read-only. */
@Entity
@Table(name = "provinces")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Province {

    @Id
    private Short id;

    @Column(nullable = false, length = 50)
    private String name;
}
