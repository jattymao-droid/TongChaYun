package com.ruoyi.biz.utils;

import java.util.concurrent.TimeUnit;
import com.ruoyi.biz.domain.BizQuery;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.common.utils.spring.SpringUtils;

/**
 * Open query captcha unlock + per-IP daily search limit.
 * Daily quota is consumed only by real searches, not by export / field-dist.
 */
public final class OpenQueryGuard
{
    private static final String UNLOCK_PREFIX = "biz_q_unlock:";
    private static final String DAILY_PREFIX = "biz_q_daily:";

    private OpenQueryGuard() {}

    /** Captcha unlock + consume one daily search quota. */
    public static void assertSearchAllowed(BizQuery query, String captchaCode, String captchaUuid)
    {
        assertAccess(query, captchaCode, captchaUuid, true);
    }

    /** Captcha unlock only (export / distribution). Does not consume daily quota. */
    public static void assertAccessAllowed(BizQuery query, String captchaCode, String captchaUuid)
    {
        assertAccess(query, captchaCode, captchaUuid, false);
    }

    private static void assertAccess(BizQuery query, String captchaCode, String captchaUuid, boolean consumeDaily)
    {
        if (query == null)
        {
            return;
        }
        String code = query.getPublicCode();
        String ip = StringUtils.nvl(IpUtils.getIpAddr(), "unknown");
        RedisCache redis = SpringUtils.getBean(RedisCache.class);

        if ("1".equals(query.getNeedCaptcha()))
        {
            String unlockKey = UNLOCK_PREFIX + code + ":" + ip;
            Object unlocked = redis.getCacheObject(unlockKey);
            if (unlocked == null)
            {
                OpenCaptchaHelper.validate(captchaCode, captchaUuid);
                redis.setCacheObject(unlockKey, "1", 15, TimeUnit.MINUTES);
            }
        }

        Integer dailyLimit = query.getDailyLimit();
        if (dailyLimit == null || dailyLimit <= 0)
        {
            return;
        }
        String dayKey = DAILY_PREFIX + code + ":" + ip;
        if (consumeDaily)
        {
            Long count = redis.redisTemplate.opsForValue().increment(dayKey);
            if (count != null && count == 1L)
            {
                redis.expire(dayKey, 1, TimeUnit.DAYS);
            }
            if (count != null && count > dailyLimit.longValue())
            {
                throw new ServiceException("\u4eca\u65e5\u67e5\u8be2\u6b21\u6570\u5df2\u8fbe\u4e0a\u9650\uff0c\u8bf7\u660e\u65e5\u518d\u8bd5");
            }
        }
        else
        {
            // Soft check: block export/dist only when already over limit from prior searches
            Object raw = redis.redisTemplate.opsForValue().get(dayKey);
            long count = 0L;
            if (raw instanceof Number)
            {
                count = ((Number) raw).longValue();
            }
            else if (raw != null)
            {
                try { count = Long.parseLong(String.valueOf(raw)); } catch (Exception ignored) {}
            }
            if (count >= dailyLimit.longValue())
            {
                throw new ServiceException("\u4eca\u65e5\u67e5\u8be2\u6b21\u6570\u5df2\u8fbe\u4e0a\u9650\uff0c\u8bf7\u660e\u65e5\u518d\u8bd5");
            }
        }
    }
}
