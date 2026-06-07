package com.hmdp.dto.neo4j;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnownUserDTO {
    private Long userId;
    private String nickname;
    private Integer commonCount;
    private List<String> commonShops;
}
