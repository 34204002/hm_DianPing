package com.hmdp.controller;

import com.hmdp.service.IBehaviorAnalysisService;
import com.hmdp.dto.Result;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 用户行为分析接口，提供DAU/WAU/MAU、热门商户排行和用户偏好画像
 */
@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    @Autowired
    private IBehaviorAnalysisService behaviorAnalysisService;

    /** 查询指定日期的日活用户数 */
    @GetMapping("/dau")
    public Result dau(@RequestParam(defaultValue = "") String date) {
        LocalDate targetDate = parseDate(date);
        return Result.ok(behaviorAnalysisService.getDau(targetDate));
    }

    /** 查询DAU + WAU + MAU综合统计 */
    @GetMapping("/stats")
    public Result stats(@RequestParam(defaultValue = "") String date) {
        LocalDate targetDate = parseDate(date);
        long dau = behaviorAnalysisService.getDau(targetDate);
        long wau = behaviorAnalysisService.getWau(targetDate.with(DayOfWeek.MONDAY));
        long mau = behaviorAnalysisService.getMau(targetDate.format(DateTimeFormatter.ofPattern("yyyyMM")));
        return Result.ok(new com.hmdp.dto.analytics.DauStatsDTO(dau, wau, mau,
                targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
    }

    /** 查询热门商户排行 Top N，month格式yyyyMM，默认当月 */
    @GetMapping("/hot-shops")
    public Result hotShops(@RequestParam(defaultValue = "10") int topN,
                           @RequestParam(defaultValue = "") String month) {
        return Result.ok(behaviorAnalysisService.getHotShops(topN, month));
    }

    /** 查询当前用户的偏好画像，year为"all"查全量，否则查指定年份(yyyy)，默认今年 */
    @GetMapping("/user-preference")
    public Result userPreference(@RequestParam(defaultValue = "") String year) {
        Long userId = UserHolder.getUser().getId();
        return Result.ok(behaviorAnalysisService.getUserPreference(userId, year));
    }

    /** 归档指定日期的DAU数据到MySQL */
    @PostMapping("/dau/archive")
    public Result archiveDau(@RequestParam(defaultValue = "") String date) {
        LocalDate targetDate = parseDate(date);
        boolean ok = behaviorAnalysisService.archiveDauCount(targetDate);
        return ok ? Result.ok("DAU归档完成") : Result.fail("DAU归档失败");
    }

    /** 归档指定月份的热门商户排行到MySQL，month格式yyyyMM */
    @PostMapping("/hot-shops/archive")
    public Result archiveHotShops(@RequestParam String month) {
        boolean ok = behaviorAnalysisService.archiveHotShopRanking(month);
        return ok ? Result.ok("热门排行归档完成") : Result.fail("热门排行归档失败");
    }

    /** 归档指定月份的MAU数据到MySQL，month格式yyyyMM */
    @PostMapping("/mau/archive")
    public Result archiveMau(@RequestParam String month) {
        boolean ok = behaviorAnalysisService.archiveMauCount(month);
        return ok ? Result.ok("MAU归档完成") : Result.fail("MAU归档失败");
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isEmpty()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
}
