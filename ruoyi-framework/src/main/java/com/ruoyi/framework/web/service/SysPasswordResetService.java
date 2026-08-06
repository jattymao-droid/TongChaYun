package com.ruoyi.framework.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.ForgotPasswordBody;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.exception.user.CaptchaException;
import com.ruoyi.common.exception.user.CaptchaExpireException;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.system.service.ISysBasicService;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysUserService;

/**
 * Forgot password via email code
 */
@Component
public class SysPasswordResetService
{
    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private ISysBasicService basicService;

    @Autowired
    private RedisCache redisCache;

    public String resetByEmail(ForgotPasswordBody body)
    {
        if (body == null)
        {
            return "\u53c2\u6570\u4e0d\u80fd\u4e3a\u7a7a";
        }
        if (!Convert.toBool(configService.selectConfigByKey("sys.mail.resetEnabled"), false))
        {
            return "\u672a\u5f00\u542f\u5fd8\u8bb0\u5bc6\u7801\u90ae\u7bb1\u91cd\u7f6e";
        }
        String email = body.getEmail();
        String password = body.getPassword();
        if (StringUtils.isEmpty(email))
        {
            return "\u90ae\u7bb1\u4e0d\u80fd\u4e3a\u7a7a";
        }
        if (StringUtils.isEmpty(password))
        {
            return "\u5bc6\u7801\u4e0d\u80fd\u4e3a\u7a7a";
        }
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
            || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            return "\u5bc6\u7801\u957f\u5ea6\u5fc5\u987b\u5728" + UserConstants.PASSWORD_MIN_LENGTH
                + "\u5230" + UserConstants.PASSWORD_MAX_LENGTH + "\u4e2a\u5b57\u7b26\u4e4b\u95f4";
        }

        boolean captchaEnabled = configService.selectCaptchaEnabled();
        if (captchaEnabled)
        {
            validateCaptcha(email, body.getCode(), body.getUuid());
        }

        try
        {
            basicService.validateResetEmailCode(email, body.getEmailCode());
        }
        catch (RuntimeException e)
        {
            return e.getMessage();
        }

        SysUser user = userService.selectUserByEmail(email.trim());
        if (user == null)
        {
            return "\u90ae\u7bb1\u672a\u7ed1\u5b9a\u8d26\u53f7\u6216\u9a8c\u8bc1\u7801\u65e0\u6548";
        }
        if ("1".equals(user.getStatus()))
        {
            return "\u8d26\u53f7\u5df2\u505c\u7528\uff0c\u65e0\u6cd5\u91cd\u7f6e\u5bc6\u7801";
        }

        String encrypted = SecurityUtils.encryptPassword(password);
        userService.resetUserPwd(user.getUserId(), encrypted);
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(user.getUserName(), "Reset",
            "\u90ae\u7bb1\u9a8c\u8bc1\u7801\u91cd\u7f6e\u5bc6\u7801\u6210\u529f"));
        return "";
    }

    private void validateCaptcha(String username, String code, String uuid)
    {
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
        String captcha = redisCache.getCacheObject(verifyKey);
        if (captcha == null)
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire")));
            throw new CaptchaExpireException();
        }
        redisCache.deleteObject(verifyKey);
        if (!code.equalsIgnoreCase(captcha))
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error")));
            throw new CaptchaException();
        }
    }
}
