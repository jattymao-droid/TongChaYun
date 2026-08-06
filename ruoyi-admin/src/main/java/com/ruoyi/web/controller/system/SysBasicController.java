package com.ruoyi.web.controller.system;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.vo.SysBasicSettings;
import com.ruoyi.system.service.ISysBasicService;

@RestController
@RequestMapping("/system/basic")
public class SysBasicController extends BaseController
{
    @Autowired
    private ISysBasicService basicService;

    @Anonymous
    @GetMapping("/site")
    public AjaxResult site()
    {
        return success(basicService.getSiteInfo().toSitePublicMap());
    }

    @PreAuthorize("@ss.hasPermi('system:basic:query') or @ss.hasPermi('system:basic:list')")
    @GetMapping
    public AjaxResult getInfo()
    {
        return success(basicService.getSettings(true));
    }

    @PreAuthorize("@ss.hasPermi('system:basic:edit')")
    @Log(title = "BasicSettings", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult save(@RequestBody SysBasicSettings settings)
    {
        basicService.saveSettings(settings, getUsername());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('system:basic:edit')")
    @Log(title = "MailTest", businessType = BusinessType.OTHER)
    @PostMapping("/testMail")
    public AjaxResult testMail(@RequestBody Map<String, String> body)
    {
        String to = body == null ? null : body.get("to");
        if (StringUtils.isEmpty(to))
        {
            return error("\u8bf7\u586b\u5199\u6536\u4ef6\u90ae\u7bb1");
        }
        basicService.sendTestMail(to);
        return success("\u6d4b\u8bd5\u90ae\u4ef6\u5df2\u53d1\u9001");
    }

    @Anonymous
    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    @PostMapping("/sendRegisterCode")
    public AjaxResult sendRegisterCode(@RequestBody Map<String, String> body)
    {
        String email = body == null ? null : body.get("email");
        String code = body == null ? null : body.get("code");
        String uuid = body == null ? null : body.get("uuid");
        basicService.sendRegisterCode(email, code, uuid);
        return success("\u9a8c\u8bc1\u7801\u5df2\u53d1\u9001");
    }
}
