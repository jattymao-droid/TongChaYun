package com.ruoyi.web.controller.system;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.ForgotPasswordBody;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.SysPasswordResetService;
import com.ruoyi.system.service.ISysBasicService;
import com.ruoyi.system.service.ISysConfigService;

/** Forgot password (email reset) */
@RestController
public class SysPasswordResetController extends BaseController
{
    @Autowired
    private SysPasswordResetService passwordResetService;

    @Autowired
    private ISysBasicService basicService;

    @Autowired
    private ISysConfigService configService;

    @Anonymous
    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    @PostMapping("/forgotPassword/sendCode")
    public AjaxResult sendCode(@RequestBody Map<String, String> body)
    {
        if (!Convert.toBool(configService.selectConfigByKey("sys.mail.resetEnabled"), false))
        {
            return error("\u672a\u5f00\u542f\u5fd8\u8bb0\u5bc6\u7801\u90ae\u7bb1\u91cd\u7f6e");
        }
        String email = body == null ? null : body.get("email");
        String code = body == null ? null : body.get("code");
        String uuid = body == null ? null : body.get("uuid");
        basicService.sendResetPasswordCode(email, code, uuid);
        return success("\u82e5\u8be5\u90ae\u7bb1\u5df2\u7ed1\u5b9a\u8d26\u53f7\uff0c\u9a8c\u8bc1\u7801\u5c06\u53d1\u9001\u81f3\u90ae\u7bb1");
    }

    @Anonymous
    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    @PostMapping("/forgotPassword/reset")
    public AjaxResult reset(@RequestBody ForgotPasswordBody body)
    {
        String msg = passwordResetService.resetByEmail(body);
        return StringUtils.isEmpty(msg)
            ? success("\u5bc6\u7801\u5df2\u91cd\u7f6e\uff0c\u8bf7\u4f7f\u7528\u65b0\u5bc6\u7801\u767b\u5f55")
            : error(msg);
    }
}
