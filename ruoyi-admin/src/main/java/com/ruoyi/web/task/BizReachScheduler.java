package com.ruoyi.web.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.ruoyi.biz.service.IBizReachService;

/**
 * P22: scan schedule publish / auto expire / deadline remind every minute.
 */
@Component
public class BizReachScheduler
{
    private static final Logger log = LoggerFactory.getLogger(BizReachScheduler.class);

    @Autowired
    private IBizReachService reachService;

    @Scheduled(fixedDelay = 60000, initialDelay = 15000)
    public void tick()
    {
        try
        {
            var result = reachService.runDueTasks();
            int total = result.values().stream().mapToInt(Integer::intValue).sum();
            if (total > 0)
            {
                log.info("biz reach tasks done: {}", result);
            }
        }
        catch (Exception e)
        {
            log.warn("biz reach scheduler failed: {}", e.getMessage());
        }
    }
}
