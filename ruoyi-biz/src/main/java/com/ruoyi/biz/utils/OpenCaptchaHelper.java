package com.ruoyi.biz.utils;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.spring.SpringUtils;

/**
 * One-shot captcha check for open endpoints (reuse login captcha Redis keys).
 */
public final class OpenCaptchaHelper
{
    private OpenCaptchaHelper() {}

    public static void validate(String code, String uuid)
    {
        if (StringUtils.isEmpty(code) || StringUtils.isEmpty(uuid))
        {
            throw new ServiceException("请输入验证码");
        }
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + uuid;
        RedisCache redisCache = SpringUtils.getBean(RedisCache.class);
        String captcha = redisCache.getCacheObject(verifyKey);
        if (captcha == null)
        {
            throw new ServiceException("验证码已失效");
        }
        redisCache.deleteObject(verifyKey);
        if (!code.equalsIgnoreCase(captcha))
        {
            throw new ServiceException("验证码错误");
        }
    }
}
