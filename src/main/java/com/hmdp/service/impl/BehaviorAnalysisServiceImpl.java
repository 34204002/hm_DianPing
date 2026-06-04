package com.hmdp.service.impl;

import com.hmdp.dto.analytics.HotShopDTO;
import com.hmdp.dto.analytics.UserPreferenceDTO;
import com.hmdp.service.IBehaviorAnalysisService;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BehaviorAnalysisServiceImpl implements IBehaviorAnalysisService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void recordActive(Long userId) {
        try {
            LocalDate today = LocalDate.now();
            String dauKey = RedisConstants.DAU_KEY + today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            stringRedisTemplate.opsForValue().setBit(dauKey, userId, true);
            stringRedisTemplate.expire(dauKey, RedisConstants.DAU_KEY_TTL_DAYS, TimeUnit.DAYS);

            String wauKey = RedisConstants.WAU_KEY + today.with(DayOfWeek.MONDAY).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            stringRedisTemplate.opsForValue().setBit(wauKey, userId, true);
            stringRedisTemplate.expire(wauKey, RedisConstants.WAU_KEY_TTL_DAYS, TimeUnit.DAYS);

            String mauKey = RedisConstants.MAU_KEY + today.format(DateTimeFormatter.ofPattern("yyyyMM"));
            stringRedisTemplate.opsForValue().setBit(mauKey, userId, true);
            stringRedisTemplate.expire(mauKey, RedisConstants.MAU_KEY_TTL_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            log.debug("记录活跃用户失败: userId={}", userId, e);
        }
    }

    public long getDau(LocalDate date) {
        String key = RedisConstants.DAU_KEY + date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return bitCount(key);
    }

    public long getWau(LocalDate monday) {
        String key = RedisConstants.WAU_KEY + monday.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return bitCount(key);
    }

    public long getMau(String yearMonth) {
        String key = RedisConstants.MAU_KEY + yearMonth;
        return bitCount(key);
    }

    private long bitCount(String key) {
        Long count = stringRedisTemplate.execute(
                (RedisConnection connection) -> connection.bitCount(key.getBytes(StandardCharsets.UTF_8)));
        return count != null ? count : 0;
    }

    public void recordHotShop(Long shopId, int weight) {
        try {
            String monthKey = RedisConstants.HOT_SHOP_KEY + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            stringRedisTemplate.opsForZSet().incrementScore(monthKey, String.valueOf(shopId), weight);
            stringRedisTemplate.expire(monthKey, RedisConstants.HOT_SHOP_KEY_TTL_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            log.debug("记录热门商铺失败: shopId={}", shopId, e);
        }
    }

    public List<HotShopDTO> getHotShops(int topN, String yearMonth) {
        String key = RedisConstants.HOT_SHOP_KEY + resolveYearMonth(yearMonth);
        Set<ZSetOperations.TypedTuple<String>> top = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(key, 0, topN - 1);
        if (top == null || top.isEmpty()) {
            return Collections.emptyList();
        }
        return top.stream()
                .map(t -> HotShopDTO.builder()
                        .shopId(Long.valueOf(Objects.requireNonNull(t.getValue())))
                        .hotScore(t.getScore())
                        .build())
                .collect(Collectors.toList());
    }

    private String resolveYearMonth(String yearMonth) {
        if (yearMonth == null || yearMonth.isEmpty()) {
            return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        }
        return yearMonth;
    }

    public void recordUserPreference(Long userId, Long shopTypeId, String actionType) {
        try {
            String field = actionType + ":" + shopTypeId;
            String yearKey = RedisConstants.USER_BEHAVIOR_KEY + LocalDate.now().getYear() + ":" + userId;
            stringRedisTemplate.opsForHash().increment(yearKey, field, 1);
            stringRedisTemplate.expire(yearKey, RedisConstants.USER_BEHAVIOR_KEY_TTL_DAYS, TimeUnit.DAYS);
            String allKey = RedisConstants.USER_BEHAVIOR_ALL_KEY + userId;
            stringRedisTemplate.opsForHash().increment(allKey, field, 1);
        } catch (Exception e) {
            log.debug("记录用户偏好失败: userId={}", userId, e);
        }
    }

    public UserPreferenceDTO getUserPreference(Long userId, String year) {
        String key;
        if ("all".equals(year)) {
            key = RedisConstants.USER_BEHAVIOR_ALL_KEY + userId;
        } else if (year != null && !year.isEmpty()) {
            key = RedisConstants.USER_BEHAVIOR_KEY + year + ":" + userId;
        } else {
            key = RedisConstants.USER_BEHAVIOR_KEY + LocalDate.now().getYear() + ":" + userId;
        }
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        Map<String, Long> counts = new LinkedHashMap<>();
        entries.forEach((k, v) -> counts.put(k.toString(), Long.valueOf(v.toString())));
        return UserPreferenceDTO.builder().userId(userId).preferenceCounts(counts).build();
    }

    public boolean archiveDauCount(LocalDate date) {
        String key = RedisConstants.DAU_KEY + date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = bitCount(key);
        try {
            jdbcTemplate.update(
                    "INSERT INTO tb_dau_stats (date, dau_count) VALUES (?, ?) ON DUPLICATE KEY UPDATE dau_count = VALUES(dau_count)",
                    date.toString(), count);
            log.info("DAU归档完成: date={}, count={}", date, count);
            return true;
        } catch (Exception e) {
            log.warn("DAU归档失败: date={}", date, e);
            return false;
        }
    }

    public boolean archiveHotShopRanking(String yearMonth) {
        String key = RedisConstants.HOT_SHOP_KEY + yearMonth;
        Set<ZSetOperations.TypedTuple<String>> all = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(key, 0, -1);
        if (all == null || all.isEmpty()) {
            return false;
        }
        try {
            int rank = 0;
            List<Object[]> batchArgs = new ArrayList<>();
            for (ZSetOperations.TypedTuple<String> t : all) {
                rank++;
                Long shopId = Long.valueOf(Objects.requireNonNull(t.getValue()));
                String shopName = queryShopName(shopId);
                batchArgs.add(new Object[]{shopId, shopName, yearMonth, t.getScore().longValue(), rank});
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO tb_hot_shop_ranking (shop_id, shop_name, year_month, hot_score, ranking) VALUES (?, ?, ?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE hot_score = VALUES(hot_score), ranking = VALUES(ranking)",
                    batchArgs);
            log.info("热门商户排行归档完成: yearMonth={}, total={}", yearMonth, rank);
            return true;
        } catch (Exception e) {
            log.warn("热门商户排行归档失败: yearMonth={}", yearMonth, e);
            return false;
        }
    }

    public boolean archiveMauCount(String yearMonth) {
        String key = RedisConstants.MAU_KEY + yearMonth;
        long count = bitCount(key);
        try {
            jdbcTemplate.update(
                    "INSERT INTO tb_mau_stats (year_month, mau_count) VALUES (?, ?) ON DUPLICATE KEY UPDATE mau_count = VALUES(mau_count)",
                    yearMonth, count);
            log.info("MAU归档完成: yearMonth={}, count={}", yearMonth, count);
            return true;
        } catch (Exception e) {
            log.warn("MAU归档失败: yearMonth={}", yearMonth, e);
            return false;
        }
    }

    private String queryShopName(Long shopId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT name FROM tb_shop WHERE id = ?", String.class, shopId);
        } catch (Exception e) {
            return "未知商户";
        }
    }
}
