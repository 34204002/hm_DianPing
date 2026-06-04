package com.hmdp.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DauStatsDTO {
    private long dau;
    private long wau;
    private long mau;
    private String date;
}
