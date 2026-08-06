package com.ruoyi.framework.web.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.enums.UserStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.system.domain.SysUserOauth;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysUserOauthService;
import com.ruoyi.system.service.ISysUserService;

/**
 * Native WeChat / QQ OAuth login
 */
@Component
public class SysOauthLoginService
{
    private static final Logger log = LoggerFactory.getLogger(SysOauthLoginService.class);

    public static final String PROVIDER_WECHAT = "wechat";
    public static final String PROVIDER_QQ = "qq";

    private static final Pattern QQ_OPENID_PATTERN = Pattern.compile("\"openid\"\\s*:\\s*\"([^\"]+)\"");

    @Autowired
    private ISysConfigService configService;
    @Autowired
    private ISysUserService userService;
    @Autowired
    private ISysUserOauthService userOauthService;
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private SysLoginService loginService;
    @Autowired
    private RedisCache redisCache;
    @Autowired
    private PlatformTransactionManager transactionManager;

    public Map<String, Object> publicConfig()
    {
        Map<String, Object> m = new HashMap<>();
        boolean wechat = isProviderReady(PROVIDER_WECHAT);
        boolean qq = isProviderReady(PROVIDER_QQ);
        m.put("wechatEnabled", wechat);
        m.put("qqEnabled", qq);
        m.put("wechatAuthorizeUrl", wechat ? "/login/oauth/wechat/authorize" : "");
        m.put("qqAuthorizeUrl", qq ? "/login/oauth/qq/authorize" : "");
        return m;
    }

    public boolean isProviderReady(String provider)
    {
        if (PROVIDER_WECHAT.equals(provider))
        {
            return Convert.toBool(cfg("sys.oauth.wechat.enabled"), false)
                    && StringUtils.isNotEmpty(cfg("sys.oauth.wechat.appId"))
                    && StringUtils.isNotEmpty(cfg("sys.oauth.wechat.appSecret"));
        }
        if (PROVIDER_QQ.equals(provider))
        {
            return Convert.toBool(cfg("sys.oauth.qq.enabled"), false)
                    && StringUtils.isNotEmpty(cfg("sys.oauth.qq.appId"))
                    && StringUtils.isNotEmpty(cfg("sys.oauth.qq.appKey"));
        }
        return false;
    }

    public String buildAuthorizeUrl(String provider)
    {
        if (!isProviderReady(provider))
        {
            throw new ServiceException("\u8be5\u7b2c\u4e09\u65b9\u767b\u5f55\u672a\u542f\u7528\u6216\u672a\u914d\u7f6e");
        }
        String state = IdUtils.fastSimpleUUID();
        redisCache.setCacheObject(CacheConstants.OAUTH_STATE_KEY + state, provider, 10, TimeUnit.MINUTES);
        String redirectUri = callbackUri(provider);
        if (PROVIDER_WECHAT.equals(provider))
        {
            String appId = cfg("sys.oauth.wechat.appId");
            return "https://open.weixin.qq.com/connect/qrconnect?appid=" + enc(appId)
                    + "&redirect_uri=" + enc(redirectUri)
                    + "&response_type=code&scope=snsapi_login&state=" + enc(state)
                    + "#wechat_redirect";
        }
        if (PROVIDER_QQ.equals(provider))
        {
            String appId = cfg("sys.oauth.qq.appId");
            return "https://graph.qq.com/oauth2.0/authorize?response_type=code&client_id=" + enc(appId)
                    + "&redirect_uri=" + enc(redirectUri)
                    + "&state=" + enc(state)
                    + "&scope=get_user_info";
        }
        throw new ServiceException("\u4e0d\u652f\u6301\u7684\u767b\u5f55\u65b9\u5f0f");
    }

    /**
     * Handle IdP callback, return frontend redirect URL with oauthTicket
     */
    public String handleCallback(String provider, String code, String state)
    {
        if (StringUtils.isEmpty(code) || StringUtils.isEmpty(state))
        {
            throw new ServiceException("\u6388\u6743\u53c2\u6570\u7f3a\u5931");
        }
        String cached = redisCache.getCacheObject(CacheConstants.OAUTH_STATE_KEY + state);
        redisCache.deleteObject(CacheConstants.OAUTH_STATE_KEY + state);
        if (StringUtils.isEmpty(cached) || !provider.equals(cached))
        {
            throw new ServiceException("\u6388\u6743\u72b6\u6001\u65e0\u6548\u6216\u5df2\u8fc7\u671f\uff0c\u8bf7\u91cd\u8bd5");
        }
        if (!isProviderReady(provider))
        {
            throw new ServiceException("\u8be5\u7b2c\u4e09\u65b9\u767b\u5f55\u672a\u542f\u7528");
        }

        OauthProfile profile = PROVIDER_WECHAT.equals(provider)
                ? fetchWechatProfile(code)
                : fetchQqProfile(code);

        if (profile == null || StringUtils.isEmpty(profile.openId))
        {
            throw new ServiceException("\u83b7\u53d6\u7b2c\u4e09\u65b9\u7528\u6237\u4fe1\u606f\u5931\u8d25");
        }

        SysUser user = resolveOrCreateUser(provider, profile);
        LoginUser loginUser = (LoginUser) userDetailsService.createLoginUser(user);
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(user.getUserName(), Constants.LOGIN_SUCCESS,
                provider + " OAuth login success"));
        loginService.recordLoginInfo(loginUser.getUserId());
        String jwt = tokenService.createToken(loginUser);

        String ticket = IdUtils.fastSimpleUUID();
        redisCache.setCacheObject(CacheConstants.OAUTH_TICKET_KEY + ticket, jwt, 120, TimeUnit.SECONDS);

        String redirectBase = frontendRedirectBase();
        return redirectBase + "/login?oauthTicket=" + ticket;
    }

    public String exchangeTicket(String ticket)
    {
        if (StringUtils.isEmpty(ticket))
        {
            throw new ServiceException("\u767b\u5f55\u51ed\u8bc1\u4e0d\u80fd\u4e3a\u7a7a");
        }
        String key = CacheConstants.OAUTH_TICKET_KEY + ticket;
        String jwt = redisCache.getCacheObject(key);
        if (StringUtils.isEmpty(jwt))
        {
            throw new ServiceException("\u767b\u5f55\u51ed\u8bc1\u65e0\u6548\u6216\u5df2\u8fc7\u671f");
        }
        redisCache.deleteObject(key);
        return jwt;
    }

    public String frontendRedirectBase()
    {
        return StringUtils.nvl(cfg("sys.oauth.redirectBase"), "http://127.0.0.1:1024").replaceAll("/+$", "");
    }

    public String callbackUri(String provider)
    {
        String base = StringUtils.nvl(cfg("sys.oauth.callbackBase"), "http://127.0.0.1:1024/dev-api").replaceAll("/+$", "");
        return base + "/login/oauth/callback/" + provider;
    }

    private SysUser resolveOrCreateUser(String provider, OauthProfile profile)
    {
        SysUserOauth bind = userOauthService.selectByProviderOpenId(provider, profile.openId);
        if (bind != null && bind.getUserId() != null)
        {
            SysUser user = userService.selectUserById(bind.getUserId());
            if (user == null)
            {
                throw new ServiceException("\u7ed1\u5b9a\u8d26\u53f7\u4e0d\u5b58\u5728");
            }
            assertActive(user);
            bind.setNickname(clip(profile.nickname, 64));
            bind.setAvatar(clip(profile.avatar, 512));
            bind.setUnionId(profile.unionId);
            userOauthService.updateOauth(bind);
            return user;
        }

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        try
        {
            return tx.execute(status -> createUserAndBind(provider, profile));
        }
        catch (RuntimeException ex)
        {
            // Concurrent first login: unique (provider, open_id) may race — reuse winner
            SysUserOauth raced = userOauthService.selectByProviderOpenId(provider, profile.openId);
            if (raced != null && raced.getUserId() != null)
            {
                SysUser user = userService.selectUserById(raced.getUserId());
                if (user != null)
                {
                    assertActive(user);
                    return user;
                }
            }
            throw ex;
        }
    }

    private SysUser createUserAndBind(String provider, OauthProfile profile)
    {
        SysUserOauth existing = userOauthService.selectByProviderOpenId(provider, profile.openId);
        if (existing != null && existing.getUserId() != null)
        {
            SysUser user = userService.selectUserById(existing.getUserId());
            if (user == null)
            {
                throw new ServiceException("\u7ed1\u5b9a\u8d26\u53f7\u4e0d\u5b58\u5728");
            }
            assertActive(user);
            return user;
        }

        SysUser user = new SysUser();
        String prefix = PROVIDER_WECHAT.equals(provider) ? "wx_" : "qq_";
        user.setUserName(buildUniqueUserName(prefix, profile.openId));
        String fallbackNick = PROVIDER_WECHAT.equals(provider) ? "\u5fae\u4fe1\u7528\u6237" : "QQ\u7528\u6237";
        String nick = StringUtils.isNotEmpty(profile.nickname) ? profile.nickname : fallbackNick;
        user.setNickName(clip(nick, 30));
        // sys_user.avatar is varchar(100); skip long CDN URLs (kept on biz_user_oauth)
        if (StringUtils.isNotEmpty(profile.avatar) && profile.avatar.length() <= 100)
        {
            user.setAvatar(profile.avatar);
        }
        user.setPassword(SecurityUtils.encryptPassword(IdUtils.fastSimpleUUID()));
        user.setStatus("0");
        user.setCreateBy(provider);
        user.setRemark(provider + " OAuth auto register");
        user.setPwdUpdateDate(DateUtils.getNowDate());
        boolean ok = userService.registerUser(user);
        if (!ok || user.getUserId() == null)
        {
            throw new ServiceException("\u81ea\u52a8\u521b\u5efa\u8d26\u53f7\u5931\u8d25");
        }

        SysUserOauth oauth = new SysUserOauth();
        oauth.setUserId(user.getUserId());
        oauth.setProvider(provider);
        oauth.setOpenId(profile.openId);
        oauth.setUnionId(profile.unionId);
        oauth.setNickname(clip(profile.nickname, 64));
        oauth.setAvatar(clip(profile.avatar, 512));
        userOauthService.insertOauth(oauth);

        AsyncManager.me().execute(AsyncFactory.recordLogininfor(user.getUserName(), Constants.REGISTER,
                provider + " OAuth auto register"));

        SysUser created = userService.selectUserById(user.getUserId());
        assertActive(created);
        return created;
    }

    private static String clip(String value, int max)
    {
        if (StringUtils.isEmpty(value))
        {
            return value;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private void assertActive(SysUser user)
    {
        if (UserStatus.DELETED.getCode().equals(user.getDelFlag()))
        {
            throw new ServiceException("\u8d26\u53f7\u5df2\u88ab\u5220\u9664");
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            throw new ServiceException("\u8d26\u53f7\u5df2\u505c\u7528");
        }
    }

    private String buildUniqueUserName(String prefix, String openId)
    {
        String compact = openId.replaceAll("[^a-zA-Z0-9]", "");
        if (compact.length() > 12)
        {
            compact = compact.substring(compact.length() - 12);
        }
        if (StringUtils.isEmpty(compact))
        {
            compact = IdUtils.fastSimpleUUID().substring(0, 8);
        }
        String base = prefix + compact;
        if (base.length() > 30)
        {
            base = base.substring(0, 30);
        }
        String candidate = base;
        int i = 0;
        while (!userService.checkUserNameUnique(nameProbe(candidate)))
        {
            i++;
            String suffix = String.valueOf(i);
            candidate = base.substring(0, Math.min(base.length(), 30 - suffix.length())) + suffix;
            if (i > 50)
            {
                candidate = (prefix + IdUtils.fastSimpleUUID()).substring(0, 30);
                break;
            }
        }
        return candidate;
    }

    private SysUser nameProbe(String userName)
    {
        SysUser u = new SysUser();
        u.setUserName(userName);
        return u;
    }

    private OauthProfile fetchWechatProfile(String code)
    {
        String appId = cfg("sys.oauth.wechat.appId");
        String secret = cfg("sys.oauth.wechat.appSecret");
        String tokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + enc(appId)
                + "&secret=" + enc(secret)
                + "&code=" + enc(code)
                + "&grant_type=authorization_code";
        JSONObject tokenJson = httpGetJson(tokenUrl);
        if (tokenJson == null || StringUtils.isEmpty(tokenJson.getString("openid")))
        {
            log.warn("WeChat token error: errcode={}, errmsg={}",
                    tokenJson == null ? null : tokenJson.get("errcode"),
                    tokenJson == null ? null : tokenJson.getString("errmsg"));
            throw new ServiceException("\u5fae\u4fe1\u6388\u6743\u5931\u8d25");
        }
        String accessToken = tokenJson.getString("access_token");
        String openId = tokenJson.getString("openid");
        String unionId = tokenJson.getString("unionid");

        OauthProfile p = new OauthProfile();
        p.openId = openId;
        p.unionId = unionId;

        String infoUrl = "https://api.weixin.qq.com/sns/userinfo?access_token=" + enc(accessToken)
                + "&openid=" + enc(openId) + "&lang=zh_CN";
        JSONObject info = httpGetJson(infoUrl);
        if (info != null && !isWechatApiError(info))
        {
            p.nickname = info.getString("nickname");
            p.avatar = info.getString("headimgurl");
        }
        return p;
    }

    private static boolean isWechatApiError(JSONObject info)
    {
        if (info == null || !info.containsKey("errcode"))
        {
            return false;
        }
        Integer code = info.getInteger("errcode");
        return code != null && code.intValue() != 0;
    }

    private OauthProfile fetchQqProfile(String code)
    {
        String appId = cfg("sys.oauth.qq.appId");
        String appKey = cfg("sys.oauth.qq.appKey");
        String redirectUri = callbackUri(PROVIDER_QQ);
        String tokenUrl = "https://graph.qq.com/oauth2.0/token?grant_type=authorization_code"
                + "&client_id=" + enc(appId)
                + "&client_secret=" + enc(appKey)
                + "&code=" + enc(code)
                + "&redirect_uri=" + enc(redirectUri)
                + "&fmt=json";
        JSONObject tokenJson = httpGetJson(tokenUrl);
        if (tokenJson == null || StringUtils.isEmpty(tokenJson.getString("access_token")))
        {
            // QQ may return urlencoded string without fmt in some cases
            String raw = httpGetRaw(tokenUrl.replace("&fmt=json", ""));
            String accessToken = parseQueryParam(raw, "access_token");
            if (StringUtils.isEmpty(accessToken))
            {
                log.warn("QQ token error: err={}, msg={}, rawHasError={}",
                        tokenJson == null ? null : tokenJson.get("error"),
                        tokenJson == null ? null : tokenJson.getString("error_description"),
                        raw != null && raw.contains("error"));
                throw new ServiceException("QQ\u6388\u6743\u5931\u8d25");
            }
            tokenJson = new JSONObject();
            tokenJson.put("access_token", accessToken);
        }
        String accessToken = tokenJson.getString("access_token");
        String meRaw = httpGetRaw("https://graph.qq.com/oauth2.0/me?access_token=" + enc(accessToken));
        String openId = extractQqOpenId(meRaw);
        if (StringUtils.isEmpty(openId))
        {
            throw new ServiceException("\u83b7\u53d6QQ OpenID\u5931\u8d25");
        }
        OauthProfile p = new OauthProfile();
        p.openId = openId;
        // optional unionid when QQ Connect enables it
        try
        {
            String json = meRaw;
            int start = meRaw == null ? -1 : meRaw.indexOf('{');
            int end = meRaw == null ? -1 : meRaw.lastIndexOf('}');
            if (start >= 0 && end > start)
            {
                json = meRaw.substring(start, end + 1);
                JSONObject meObj = JSON.parseObject(json);
                if (meObj != null)
                {
                    p.unionId = meObj.getString("unionid");
                }
            }
        }
        catch (Exception ignore)
        {
            // ignore
        }

        String infoUrl = "https://graph.qq.com/user/get_user_info?access_token=" + enc(accessToken)
                + "&oauth_consumer_key=" + enc(appId)
                + "&openid=" + enc(openId);
        JSONObject info = httpGetJson(infoUrl);
        if (info != null && info.getIntValue("ret") == 0)
        {
            p.nickname = info.getString("nickname");
            p.avatar = firstNonEmpty(info.getString("figureurl_qq_2"), info.getString("figureurl_qq_1"),
                    info.getString("figureurl_2"));
        }
        return p;
    }

    private String extractQqOpenId(String raw)
    {
        if (StringUtils.isEmpty(raw))
        {
            return null;
        }
        Matcher m = QQ_OPENID_PATTERN.matcher(raw);
        if (m.find())
        {
            return m.group(1);
        }
        try
        {
            String json = raw;
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            if (start >= 0 && end > start)
            {
                json = raw.substring(start, end + 1);
            }
            JSONObject obj = JSON.parseObject(json);
            return obj == null ? null : obj.getString("openid");
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private static String parseQueryParam(String query, String key)
    {
        if (StringUtils.isEmpty(query))
        {
            return null;
        }
        for (String part : query.split("&"))
        {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && key.equals(kv[0]))
            {
                return kv[1];
            }
        }
        return null;
    }

    private String cfg(String key)
    {
        return configService.selectConfigByKey(key);
    }

    private static String enc(String v)
    {
        try
        {
            return URLEncoder.encode(StringUtils.nvl(v, ""), StandardCharsets.UTF_8.name());
        }
        catch (Exception e)
        {
            return v;
        }
    }

    private static String firstNonEmpty(String... values)
    {
        if (values == null)
        {
            return null;
        }
        for (String v : values)
        {
            if (StringUtils.isNotEmpty(v))
            {
                return v;
            }
        }
        return null;
    }

    /** Strip secrets from URLs before logging. */
    private static String redactUrl(String url)
    {
        if (StringUtils.isEmpty(url))
        {
            return url;
        }
        return url
                .replaceAll("(?i)(secret|client_secret)=([^&]*)", "$1=***")
                .replaceAll("(?i)(access_token)=([^&]*)", "$1=***");
    }

    private JSONObject httpGetJson(String url)
    {
        String raw = httpGetRaw(url);
        if (StringUtils.isEmpty(raw))
        {
            return null;
        }
        try
        {
            // QQ callback( {...} );
            String json = raw.trim();
            if (json.startsWith("callback"))
            {
                int start = json.indexOf('{');
                int end = json.lastIndexOf('}');
                if (start >= 0 && end > start)
                {
                    json = json.substring(start, end + 1);
                }
            }
            return JSON.parseObject(json);
        }
        catch (Exception e)
        {
            log.debug("parse json failed from {}", redactUrl(url));
            return null;
        }
    }

    private String httpGetRaw(String url)
    {
        HttpURLConnection conn = null;
        try
        {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("accept", "*/*");
            int code = conn.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                    StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        }
        catch (Exception e)
        {
            log.warn("HTTP GET failed: {} - {}", redactUrl(url), e.getMessage());
            return null;
        }
        finally
        {
            if (conn != null)
            {
                conn.disconnect();
            }
        }
    }

    private static class OauthProfile
    {
        private String openId;
        private String unionId;
        private String nickname;
        private String avatar;
    }
}
