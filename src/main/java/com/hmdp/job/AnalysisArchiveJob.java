package com.hmdp.job;

import com.hmdp.service.IBehaviorAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 行为分析数据归档定时任务
 */
@Slf4j
@Component
public class AnalysisArchiveJob {

    @Autowired
    private IBehaviorAnalysisService behaviorAnalysisService;

    /** 每日凌晨1点归档最近5天DAU，自动补录遗漏 */
    @Scheduled(cron = "0 0 1 * * ?")
    public void archiveDailyDau() {
        LocalDate today = LocalDate.now();
        int success = 0, fail = 0;
        for (int i = 1; i <= 5; i++) {
            LocalDate date = today.minusDays(i);
            if (behaviorAnalysisService.archiveDauCount(date)) {
                success++;
            } else {
                fail++;
            }
        }
        log.info("DAU归档完成: 成功{}天, 失败{}天", success, fail);
    }

    /** 每月1号凌晨2点归档最近3个月热门商户排行，自动补录遗漏 */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void archiveMonthlyHotShops() {
        LocalDate today = LocalDate.now();
        int success = 0, fail = 0;
        for (int i = 1; i <= 3; i++) {
            String month = today.minusMonths(i).format(DateTimeFormatter.ofPattern("yyyyMM"));
            if (behaviorAnalysisService.archiveHotShopRanking(month)) {
                success++;
            } else {
                fail++;
            }
        }
        log.info("热门排行归档完成: 成功{}个月, 失败{}个月", success, fail);
    }

    /** 每月1号凌晨2点30归档最近3个月MAU，自动补录遗漏 */
    @Scheduled(cron = "0 30 2 1 * ?")
    public void archiveMonthlyMau() {
        LocalDate today = LocalDate.now();
        int success = 0, fail = 0;
        for (int i = 1; i <= 3; i++) {
            String month = today.minusMonths(i).format(DateTimeFormatter.ofPattern("yyyyMM"));
            if (behaviorAnalysisService.archiveMauCount(month)) {
                success++;
            } else {
                fail++;
            }
        }
        log.info("MAU归档完成: 成功{}个月, 失败{}个月", success, fail);
    }
}
