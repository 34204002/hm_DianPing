package com.hmdp.service;

import com.hmdp.dto.ai.SearchResponse;

public interface IAISearchService {

    SearchResponse search(String query, Double userX, Double userY);
}
