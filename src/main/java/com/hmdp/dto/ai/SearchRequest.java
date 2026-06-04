package com.hmdp.dto.ai;

import lombok.Data;

@Data
public class SearchRequest {
    private String query;
    private Double x;
    private Double y;
}
