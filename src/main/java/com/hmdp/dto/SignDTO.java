package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.BitSet;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignDTO {
    private int continuousDays;    // 连续签到天数
    private int totalSignedDays;   // 总签到天数
    private BitSet signBitSet;     // 签到位图
}

