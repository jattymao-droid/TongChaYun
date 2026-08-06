package com.ruoyi.web.controller.system;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.SysOauthLoginService;

/**
 * Native WeChat / QQ OAuth
 */
@RestController
@RequestMapping("/login/oauth")
public class SysOauthController
{
    @Autowired
    private SysOauthLoginService oauthLoginService;

    @GetMapping("/config")
    public AjaxResult config()
    {
        return AjaxResult.success(oauthLoginService.publicConfig());
    }

    @GetMapping("/{provider}/authorize")
    public void authorize(@PathVariable("provider") String provider, HttpServletResponse response) throws Exception
    {
        try
        {
            String p = normalizeProvider(provider);
            String url = oauthLoginService.buildAuthorizeUrl(p);
            response.sendRedirect(url);
        }
        catch (ServiceException e)
        {
            response.sendRedirect(failRedirect(e.getMessage()));
        }
        catch (Exception e)
        {
            response.sendRedirect(failRedirect("\u7b2c\u4e09\u65b9\u767b\u5f55\u5931\u8d25"));
        }
    }

    @GetMapping("/callback/{provider}")
    public void callback(@PathVariable("provider") String provider,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            HttpServletResponse response) throws Exception
    {
        try
        {
            String p = normalizeProvider(provider);
            if (StringUtils.isNotEmpty(error))
            {
                response.sendRedirect(failRedirect("\u6388\u6743\u5df2\u53d6\u6d88"));
                return;
            }
            String redirect = oauthLoginService.handleCallback(p, code, state);
            response.sendRedirect(redirect);
        }
        catch (ServiceException e)
        {
            response.sendRedirect(failRedirect(e.getMessage()));
        }
        catch (Exception e)
        {
            response.sendRedirect(failRedirect("\u7b2c\u4e09\u65b9\u767b\u5f55\u5931\u8d25"));
        }
    }

    @PostMapping("/exchange")
    public AjaxResult exchange(@RequestBody Map<String, String> body)
    {
        String ticket = body == null ? null : body.get("ticket");
        String token = oauthLoginService.exchangeTicket(ticket);
        AjaxResult ajax = AjaxResult.success();
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }

    private String failRedirect(String msg)
    {
        String base = oauthLoginService.frontendRedirectBase();
        String q = URLEncoder.encode(StringUtils.nvl(msg, "\u767b\u5f55\u5931\u8d25"), StandardCharsets.UTF_8);
        return base + "/login?oauthError=" + q;
    }

    private String normalizeProvider(String provider)
    {
        if (SysOauthLoginService.PROVIDER_WECHAT.equalsIgnoreCase(provider))
        {
            return SysOauthLoginService.PROVIDER_WECHAT;
        }
        if (SysOauthLoginService.PROVIDER_QQ.equalsIgnoreCase(provider))
        {
            return SysOauthLoginService.PROVIDER_QQ;
        }
        throw new ServiceException("\u4e0d\u652f\u6301\u7684\u767b\u5f55\u65b9\u5f0f");
    }
}
