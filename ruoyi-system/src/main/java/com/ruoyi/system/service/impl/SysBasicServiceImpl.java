package com.ruoyi.system.service.impl;

import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.exception.user.CaptchaException;
import com.ruoyi.common.exception.user.CaptchaExpireException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.domain.vo.SysBasicSettings;
import com.ruoyi.system.mapper.SysConfigMapper;
import com.ruoyi.system.service.ISysBasicService;
import com.ruoyi.system.service.ISysConfigService;

@Service
public class SysBasicServiceImpl implements ISysBasicService
{
    public static final String PASSWORD_MASK = "******";

    private static final String K_TITLE = "sys.info.title";
    private static final String K_LOGO = "sys.info.logo";
    private static final String K_COPYRIGHT = "sys.info.copyright";
    private static final String K_ICP = "sys.info.icp";
    private static final String K_FOOTER = "sys.info.footerVisible";
    private static final String K_MAIL_ENABLED = "sys.mail.enabled";
    private static final String K_MAIL_HOST = "sys.mail.host";
    private static final String K_MAIL_PORT = "sys.mail.port";
    private static final String K_MAIL_USER = "sys.mail.username";
    private static final String K_MAIL_PASS = "sys.mail.password";
    private static final String K_MAIL_FROM = "sys.mail.from";
    private static final String K_MAIL_SSL = "sys.mail.ssl";
    private static final String K_MAIL_VERIFY = "sys.mail.verifyEnabled";
    private static final String K_MAIL_RESET = "sys.mail.resetEnabled";
    private static final String K_OAUTH_REDIRECT = "sys.oauth.redirectBase";
    private static final String K_OAUTH_CALLBACK = "sys.oauth.callbackBase";
    private static final String K_OAUTH_WX_ENABLED = "sys.oauth.wechat.enabled";
    private static final String K_OAUTH_WX_APPID = "sys.oauth.wechat.appId";
    private static final String K_OAUTH_WX_SECRET = "sys.oauth.wechat.appSecret";
    private static final String K_OAUTH_QQ_ENABLED = "sys.oauth.qq.enabled";
    private static final String K_OAUTH_QQ_APPID = "sys.oauth.qq.appId";
    private static final String K_OAUTH_QQ_KEY = "sys.oauth.qq.appKey";

    @Autowired
    private ISysConfigService configService;
    @Autowired
    private SysConfigMapper configMapper;
    @Autowired
    private RedisCache redisCache;
    @Autowired
    private com.ruoyi.system.service.ISysUserService userService;

    @Override
    public SysBasicSettings getSettings(boolean maskSecret)
    {
        SysBasicSettings s = loadRaw();
        boolean hasPass = StringUtils.isNotEmpty(s.getMailPassword());
        s.setMailPasswordSet(hasPass);
        boolean hasWxSecret = StringUtils.isNotEmpty(s.getOauthWechatAppSecret());
        s.setOauthWechatAppSecretSet(hasWxSecret);
        boolean hasQqKey = StringUtils.isNotEmpty(s.getOauthQqAppKey());
        s.setOauthQqAppKeySet(hasQqKey);
        if (maskSecret)
        {
            s.setMailPassword(hasPass ? PASSWORD_MASK : "");
            s.setOauthWechatAppSecret(hasWxSecret ? PASSWORD_MASK : "");
            s.setOauthQqAppKey(hasQqKey ? PASSWORD_MASK : "");
        }
        return s;
    }

    @Override
    public SysBasicSettings getSiteInfo()
    {
        return getSettings(true);
    }

    @Override
    public void saveSettings(SysBasicSettings settings, String operator)
    {
        if (settings == null)
        {
            throw new ServiceException("参数不能为空");
        }
        upsert(K_TITLE, nz(settings.getTitle()), "系统名称", operator);
        upsert(K_LOGO, nz(settings.getLogo()), "系统Logo", operator);
        upsert(K_COPYRIGHT, nz(settings.getCopyright()), "页脚版权", operator);
        upsert(K_ICP, nz(settings.getIcp()), "备案号", operator);
        upsert(K_FOOTER, boolStr(settings.getFooterVisible()), "显示页脚", operator);

        upsert(K_MAIL_ENABLED, boolStr(settings.getMailEnabled()), "邮件服务开关", operator);
        upsert(K_MAIL_HOST, nz(settings.getMailHost()), "SMTP主机", operator);
        upsert(K_MAIL_PORT, StringUtils.isEmpty(settings.getMailPort()) ? "465" : settings.getMailPort(), "SMTP端口", operator);
        upsert(K_MAIL_USER, nz(settings.getMailUsername()), "邮箱账号", operator);
        upsert(K_MAIL_FROM, nz(settings.getMailFrom()), "发件人地址", operator);
        upsert(K_MAIL_SSL, boolStr(settings.getMailSsl()), "SMTP SSL", operator);
        upsert(K_MAIL_VERIFY, boolStr(settings.getMailVerifyEnabled()), "注册邮箱验证", operator);
        upsert(K_MAIL_RESET, boolStr(settings.getMailResetEnabled()), "忘记密码邮箱重置", operator);

        String pwd = settings.getMailPassword();
        if (StringUtils.isNotEmpty(pwd) && !PASSWORD_MASK.equals(pwd))
        {
            upsert(K_MAIL_PASS, pwd, "邮箱密码", operator);
        }

        upsert(K_OAUTH_REDIRECT, StringUtils.isEmpty(settings.getOauthRedirectBase())
                ? "http://127.0.0.1:1024" : nz(settings.getOauthRedirectBase()), "OAuth前端回调根地址", operator);
        upsert(K_OAUTH_CALLBACK, StringUtils.isEmpty(settings.getOauthCallbackBase())
                ? "http://127.0.0.1:1024/dev-api" : nz(settings.getOauthCallbackBase()), "OAuth接口回调根地址", operator);
        upsert(K_OAUTH_WX_ENABLED, boolStr(settings.getOauthWechatEnabled()), "微信OAuth开关", operator);
        upsert(K_OAUTH_WX_APPID, nz(settings.getOauthWechatAppId()), "微信AppID", operator);
        upsert(K_OAUTH_QQ_ENABLED, boolStr(settings.getOauthQqEnabled()), "QQ OAuth开关", operator);
        upsert(K_OAUTH_QQ_APPID, nz(settings.getOauthQqAppId()), "QQ AppID", operator);
        String wxSecret = settings.getOauthWechatAppSecret();
        if (StringUtils.isNotEmpty(wxSecret) && !PASSWORD_MASK.equals(wxSecret))
        {
            upsert(K_OAUTH_WX_SECRET, wxSecret, "微信AppSecret", operator);
        }
        String qqKey = settings.getOauthQqAppKey();
        if (StringUtils.isNotEmpty(qqKey) && !PASSWORD_MASK.equals(qqKey))
        {
            upsert(K_OAUTH_QQ_KEY, qqKey, "QQ AppKey", operator);
        }

        configService.resetConfigCache();
    }

    @Override
    public void sendTestMail(String to)
    {
        if (StringUtils.isEmpty(to))
        {
            throw new ServiceException("请填写收件邮箱");
        }
        SysBasicSettings s = loadRaw();
        assertMailReady(s);
        String title = StringUtils.nvl(s.getTitle(), "通查云");
        sendMail(s, to, "【" + title + "】邮件发送测试",
            "<p>这是一封来自 <b>" + title + "</b> 的测试邮件。</p><p>若您收到此邮件，说明 SMTP 配置正确。</p>");
    }

    @Override
    public void sendRegisterCode(String email, String captchaCode, String captchaUuid)
    {
        if (StringUtils.isEmpty(email) || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
        {
            throw new ServiceException("邮箱格式不正确");
        }
        if (!Convert.toBool(configService.selectConfigByKey("sys.account.registerUser"), false))
        {
            throw new ServiceException("当前系统没有开启注册功能");
        }
        SysBasicSettings s = loadRaw();
        if (!Convert.toBool(s.getMailVerifyEnabled(), false))
        {
            throw new ServiceException("未开启注册邮箱验证");
        }
        assertMailReady(s);
        validateImageCaptcha(captchaCode, captchaUuid);
        assertMailSendRate("register");
        String normalized = email.trim().toLowerCase();
        String rateKey = CacheConstants.CAPTCHA_CODE_KEY + "mail-reg-rate:" + normalized;
        if (Boolean.TRUE.equals(redisCache.hasKey(rateKey)))
        {
            throw new ServiceException("发送过于频繁，请稍后再试");
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));
        String key = CacheConstants.CAPTCHA_CODE_KEY + "mail:" + normalized;
        redisCache.setCacheObject(key, code, 10, TimeUnit.MINUTES);
        redisCache.setCacheObject(rateKey, "1", 60, TimeUnit.SECONDS);
        String title = StringUtils.nvl(s.getTitle(), "通查云");
        sendMail(s, email.trim(), "【" + title + "】注册验证码",
            "<p>您的注册验证码为：<b style=\"font-size:18px\">" + code + "</b></p>"
                + "<p>10 分钟内有效，请勿泄露给他人。</p>");
    }

    @Override
    public void validateRegisterEmailCode(String email, String code)
    {
        if (StringUtils.isEmpty(email) || StringUtils.isEmpty(code))
        {
            throw new ServiceException("请填写邮箱与邮箱验证码");
        }
        String key = CacheConstants.CAPTCHA_CODE_KEY + "mail:" + email.trim().toLowerCase();
        String cached = redisCache.getCacheObject(key);
        if (cached == null)
        {
            throw new ServiceException("邮箱验证码已过期，请重新获取");
        }
        if (!cached.equalsIgnoreCase(code.trim()))
        {
            throw new ServiceException("邮箱验证码错误");
        }
        redisCache.deleteObject(key);
    }

    @Override
    public void sendResetPasswordCode(String email, String captchaCode, String captchaUuid)
    {
        if (StringUtils.isEmpty(email) || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
        {
            throw new ServiceException("邮箱格式不正确");
        }
        SysBasicSettings s = loadRaw();
        if (!Convert.toBool(s.getMailResetEnabled(), false))
        {
            throw new ServiceException("未开启忘记密码邮箱重置");
        }
        assertMailReady(s);
        validateImageCaptcha(captchaCode, captchaUuid);
        assertMailSendRate("reset");
        String normalized = email.trim().toLowerCase();
        String rateKey = CacheConstants.CAPTCHA_CODE_KEY + "mail-reset-rate:" + normalized;
        if (Boolean.TRUE.equals(redisCache.hasKey(rateKey)))
        {
            throw new ServiceException("发送过于频繁，请稍后再试");
        }
        redisCache.setCacheObject(rateKey, "1", 60, TimeUnit.SECONDS);
        // 无论邮箱是否存在都返回成功文案，避免枚举账号；仅真实用户发信
        com.ruoyi.common.core.domain.entity.SysUser user = userService.selectUserByEmail(email.trim());
        if (user != null && !"1".equals(user.getStatus()))
        {
            String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));
            String key = CacheConstants.CAPTCHA_CODE_KEY + "mail-reset:" + normalized;
            redisCache.setCacheObject(key, code, 10, TimeUnit.MINUTES);
            String title = StringUtils.nvl(s.getTitle(), "通查云");
            String userName = StringUtils.nvl(user.getUserName(), "");
            sendMail(s, email.trim(), "【" + title + "】密码重置验证码",
                "<p>您好" + (StringUtils.isNotEmpty(userName) ? "（账号 <b>" + userName + "</b>）" : "") + "，</p>"
                    + "<p>您正在重置登录密码，验证码为：<b style=\"font-size:18px\">" + code + "</b></p>"
                    + "<p>10 分钟内有效。如非本人操作，请忽略本邮件。</p>");
        }
        else
        {
            // 缩短与真实发信的时间差，降低邮箱枚举侧信道
            try { Thread.sleep(120L + ThreadLocalRandom.current().nextInt(80)); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
    }

    @Override
    public void validateResetEmailCode(String email, String code)
    {
        if (StringUtils.isEmpty(email) || StringUtils.isEmpty(code))
        {
            throw new ServiceException("请填写邮箱与邮箱验证码");
        }
        String key = CacheConstants.CAPTCHA_CODE_KEY + "mail-reset:" + email.trim().toLowerCase();
        String cached = redisCache.getCacheObject(key);
        if (cached == null)
        {
            throw new ServiceException("邮箱验证码已过期，请重新获取");
        }
        if (!cached.equalsIgnoreCase(code.trim()))
        {
            throw new ServiceException("邮箱验证码错误");
        }
        redisCache.deleteObject(key);
    }

    private SysBasicSettings loadRaw()
    {
        SysBasicSettings s = new SysBasicSettings();
        s.setTitle(configService.selectConfigByKey(K_TITLE));
        s.setLogo(configService.selectConfigByKey(K_LOGO));
        s.setCopyright(configService.selectConfigByKey(K_COPYRIGHT));
        s.setIcp(configService.selectConfigByKey(K_ICP));
        s.setFooterVisible(configService.selectConfigByKey(K_FOOTER));
        s.setMailEnabled(configService.selectConfigByKey(K_MAIL_ENABLED));
        s.setMailHost(configService.selectConfigByKey(K_MAIL_HOST));
        s.setMailPort(configService.selectConfigByKey(K_MAIL_PORT));
        s.setMailUsername(configService.selectConfigByKey(K_MAIL_USER));
        s.setMailPassword(configService.selectConfigByKey(K_MAIL_PASS));
        s.setMailFrom(configService.selectConfigByKey(K_MAIL_FROM));
        s.setMailSsl(configService.selectConfigByKey(K_MAIL_SSL));
        s.setMailVerifyEnabled(configService.selectConfigByKey(K_MAIL_VERIFY));
        s.setMailResetEnabled(configService.selectConfigByKey(K_MAIL_RESET));
        s.setOauthRedirectBase(configService.selectConfigByKey(K_OAUTH_REDIRECT));
        s.setOauthCallbackBase(configService.selectConfigByKey(K_OAUTH_CALLBACK));
        s.setOauthWechatEnabled(configService.selectConfigByKey(K_OAUTH_WX_ENABLED));
        s.setOauthWechatAppId(configService.selectConfigByKey(K_OAUTH_WX_APPID));
        s.setOauthWechatAppSecret(configService.selectConfigByKey(K_OAUTH_WX_SECRET));
        s.setOauthQqEnabled(configService.selectConfigByKey(K_OAUTH_QQ_ENABLED));
        s.setOauthQqAppId(configService.selectConfigByKey(K_OAUTH_QQ_APPID));
        s.setOauthQqAppKey(configService.selectConfigByKey(K_OAUTH_QQ_KEY));
        if (StringUtils.isEmpty(s.getTitle())) { s.setTitle("通查云"); }
        if (StringUtils.isEmpty(s.getLogo())) { s.setLogo("/logo.svg"); }
        if (StringUtils.isEmpty(s.getCopyright())) { s.setCopyright("© 通查云 · 查询与问卷平台"); }
        if (StringUtils.isEmpty(s.getFooterVisible())) { s.setFooterVisible("true"); }
        if (StringUtils.isEmpty(s.getMailEnabled())) { s.setMailEnabled("false"); }
        if (StringUtils.isEmpty(s.getMailSsl())) { s.setMailSsl("true"); }
        if (StringUtils.isEmpty(s.getMailVerifyEnabled())) { s.setMailVerifyEnabled("false"); }
        if (StringUtils.isEmpty(s.getMailResetEnabled())) { s.setMailResetEnabled("false"); }
        if (StringUtils.isEmpty(s.getMailPort())) { s.setMailPort("465"); }
        if (StringUtils.isEmpty(s.getOauthRedirectBase())) { s.setOauthRedirectBase("http://127.0.0.1:1024"); }
        if (StringUtils.isEmpty(s.getOauthCallbackBase())) { s.setOauthCallbackBase("http://127.0.0.1:1024/dev-api"); }
        if (StringUtils.isEmpty(s.getOauthWechatEnabled())) { s.setOauthWechatEnabled("false"); }
        if (StringUtils.isEmpty(s.getOauthQqEnabled())) { s.setOauthQqEnabled("false"); }
        return s;
    }

    private void validateImageCaptcha(String code, String uuid)
    {
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        if (!captchaEnabled)
        {
            return;
        }
        if (StringUtils.isEmpty(code) || StringUtils.isEmpty(uuid))
        {
            throw new ServiceException("请填写图形验证码");
        }
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + uuid;
        String captcha = redisCache.getCacheObject(verifyKey);
        if (captcha == null)
        {
            throw new CaptchaExpireException();
        }
        redisCache.deleteObject(verifyKey);
        if (!code.equalsIgnoreCase(captcha))
        {
            throw new CaptchaException();
        }
    }

    private void assertMailSendRate(String scene)
    {
        String ip = IpUtils.getIpAddr();
        if (StringUtils.isEmpty(ip))
        {
            ip = "unknown";
        }
        String key = CacheConstants.CAPTCHA_CODE_KEY + "mail-ip:" + scene + ":" + ip;
        Object cached = redisCache.getCacheObject(key);
        int count = cached == null ? 0 : Convert.toInt(cached, 0);
        if (count >= 20)
        {
            throw new ServiceException("发送次数过多，请稍后再试");
        }
        redisCache.setCacheObject(key, count + 1, 1, TimeUnit.HOURS);
    }

    private void assertMailReady(SysBasicSettings s)
    {
        if (!Convert.toBool(s.getMailEnabled(), false))
        {
            throw new ServiceException("请先开启邮件服务");
        }
        if (StringUtils.isEmpty(s.getMailHost()) || StringUtils.isEmpty(s.getMailUsername())
            || StringUtils.isEmpty(s.getMailPassword()))
        {
            throw new ServiceException("请完善 SMTP 主机、账号与密码");
        }
    }

    private void sendMail(SysBasicSettings s, String to, String subject, String html)
    {
        try
        {
            JavaMailSenderImpl sender = buildSender(s);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            String from = StringUtils.isNotEmpty(s.getMailFrom()) ? s.getMailFrom() : s.getMailUsername();
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(message);
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("邮件发送失败：" + e.getMessage());
        }
    }

    private JavaMailSenderImpl buildSender(SysBasicSettings s)
    {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(s.getMailHost());
        int port = 465;
        try { port = Integer.parseInt(StringUtils.nvl(s.getMailPort(), "465")); } catch (Exception ignored) {}
        sender.setPort(port);
        sender.setUsername(s.getMailUsername());
        sender.setPassword(s.getMailPassword());
        sender.setDefaultEncoding("UTF-8");
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.connectiontimeout", "10000");
        boolean ssl = Convert.toBool(s.getMailSsl(), true);
        if (ssl)
        {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", String.valueOf(port));
        }
        else
        {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "false");
        }
        return sender;
    }

    private void upsert(String key, String value, String name, String operator)
    {
        SysConfig query = new SysConfig();
        query.setConfigKey(key);
        SysConfig exist = configMapper.selectConfig(query);
        if (exist == null)
        {
            SysConfig c = new SysConfig();
            c.setConfigName(name);
            c.setConfigKey(key);
            c.setConfigValue(value);
            c.setConfigType("Y");
            c.setCreateBy(operator);
            configService.insertConfig(c);
        }
        else
        {
            exist.setConfigValue(value);
            exist.setUpdateBy(operator);
            configService.updateConfig(exist);
        }
    }

    private static String nz(String v) { return v == null ? "" : v.trim(); }

    private static String boolStr(String v)
    {
        if (StringUtils.isEmpty(v)) { return "false"; }
        String t = v.trim().toLowerCase();
        if ("1".equals(t) || "true".equals(t) || "y".equals(t) || "yes".equals(t) || "on".equals(t))
        {
            return "true";
        }
        return "false";
    }
}
