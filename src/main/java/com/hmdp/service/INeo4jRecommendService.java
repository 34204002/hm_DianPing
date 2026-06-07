package com.hmdp.service;

import com.hmdp.dto.neo4j.KnownUserDTO;
import com.hmdp.dto.neo4j.ShopRecommendDTO;

import java.util.List;

public interface INeo4jRecommendService {

    List<ShopRecommendDTO> getShopsFriendsVisited(Long userId);

    List<ShopRecommendDTO> getShopsFriendsLiked(Long userId);

    List<KnownUserDTO> getPeopleYouMightKnow(Long userId, int limit);
}
