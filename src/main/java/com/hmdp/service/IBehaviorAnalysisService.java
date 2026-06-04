package com.hmdp.service;

import com.hmdp.dto.analytics.HotShopDTO;
import com.hmdp.dto.analytics.UserPreferenceDTO;

import java.time.LocalDate;
import java.util.List;

public interface IBehaviorAnalysisService {

    void recordActive(Long userId);

    long getDau(LocalDate date);

    long getWau(LocalDate monday);

    long getMau(String yearMonth);

    /** 记录商户热度，写入当月ZSet */
    void recordHotShop(Long shopId, int weight);

    /** 查询热门商户排行，yearMonth格式yyyyMM，默认当月 */
    List<HotShopDTO> getHotShops(int topN, String yearMonth);

    /** 记录用户偏好，同时写入当年key和all-time key */
    void recordUserPreference(Long userId, Long shopTypeId, String actionType);

    /** 查询用户偏好画像，year为"all"查全量，否则查指定年份(yyyy) */
    UserPreferenceDTO getUserPreference(Long userId, String year);

    /** 归档指定日期的DAU数据到MySQL */
    boolean archiveDauCount(LocalDate date);

    /** 归档指定月份的热门商户排行到MySQL */
    boolean archiveHotShopRanking(String yearMonth);

    /** 归档指定月份的MAU数据到MySQL，yearMonth格式yyyyMM */
    boolean archiveMauCount(String yearMonth);
}
