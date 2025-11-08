package com.tengYii.jobspark.domain.cv.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 证书/获奖
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certificate {
    private String name;       // 证书/奖项名称
    private String issuer;     // 颁发机构
    private LocalDate date;    // 获得日期
}