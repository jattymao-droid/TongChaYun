package com.ruoyi.system.domain.vo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Site basic settings (branding + mail + oauth).
 */
public class SysBasicSettings
{
    private String title;
    private String logo;
    private String copyright;
    private String icp;
    private String footerVisible;

    private String mailEnabled;
    private String mailHost;
    private String mailPort;
    private String mailUsername;
    private String mailPassword;
    private String mailFrom;
    private String mailSsl;
    private String mailVerifyEnabled;
    private String mailResetEnabled;
    private Boolean mailPasswordSet;

    private String oauthRedirectBase;
    private String oauthCallbackBase;
    private String oauthWechatEnabled;
    private String oauthWechatAppId;
    private String oauthWechatAppSecret;
    private Boolean oauthWechatAppSecretSet;
    private String oauthQqEnabled;
    private String oauthQqAppId;
    private String oauthQqAppKey;
    private Boolean oauthQqAppKeySet;

    public Map<String, Object> toSitePublicMap()
    {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("logo", logo);
        m.put("copyright", copyright);
        m.put("icp", icp);
        m.put("footerVisible", footerVisible);
        m.put("mailVerifyEnabled", mailVerifyEnabled);
        m.put("mailResetEnabled", mailResetEnabled);
        m.put("mailEnabled", mailEnabled);
        return m;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public String getCopyright() { return copyright; }
    public void setCopyright(String copyright) { this.copyright = copyright; }
    public String getIcp() { return icp; }
    public void setIcp(String icp) { this.icp = icp; }
    public String getFooterVisible() { return footerVisible; }
    public void setFooterVisible(String footerVisible) { this.footerVisible = footerVisible; }
    public String getMailEnabled() { return mailEnabled; }
    public void setMailEnabled(String mailEnabled) { this.mailEnabled = mailEnabled; }
    public String getMailHost() { return mailHost; }
    public void setMailHost(String mailHost) { this.mailHost = mailHost; }
    public String getMailPort() { return mailPort; }
    public void setMailPort(String mailPort) { this.mailPort = mailPort; }
    public String getMailUsername() { return mailUsername; }
    public void setMailUsername(String mailUsername) { this.mailUsername = mailUsername; }
    public String getMailPassword() { return mailPassword; }
    public void setMailPassword(String mailPassword) { this.mailPassword = mailPassword; }
    public String getMailFrom() { return mailFrom; }
    public void setMailFrom(String mailFrom) { this.mailFrom = mailFrom; }
    public String getMailSsl() { return mailSsl; }
    public void setMailSsl(String mailSsl) { this.mailSsl = mailSsl; }
    public String getMailVerifyEnabled() { return mailVerifyEnabled; }
    public void setMailVerifyEnabled(String mailVerifyEnabled) { this.mailVerifyEnabled = mailVerifyEnabled; }
    public String getMailResetEnabled() { return mailResetEnabled; }
    public void setMailResetEnabled(String mailResetEnabled) { this.mailResetEnabled = mailResetEnabled; }
    public Boolean getMailPasswordSet() { return mailPasswordSet; }
    public void setMailPasswordSet(Boolean mailPasswordSet) { this.mailPasswordSet = mailPasswordSet; }

    public String getOauthRedirectBase() { return oauthRedirectBase; }
    public void setOauthRedirectBase(String oauthRedirectBase) { this.oauthRedirectBase = oauthRedirectBase; }
    public String getOauthCallbackBase() { return oauthCallbackBase; }
    public void setOauthCallbackBase(String oauthCallbackBase) { this.oauthCallbackBase = oauthCallbackBase; }
    public String getOauthWechatEnabled() { return oauthWechatEnabled; }
    public void setOauthWechatEnabled(String oauthWechatEnabled) { this.oauthWechatEnabled = oauthWechatEnabled; }
    public String getOauthWechatAppId() { return oauthWechatAppId; }
    public void setOauthWechatAppId(String oauthWechatAppId) { this.oauthWechatAppId = oauthWechatAppId; }
    public String getOauthWechatAppSecret() { return oauthWechatAppSecret; }
    public void setOauthWechatAppSecret(String oauthWechatAppSecret) { this.oauthWechatAppSecret = oauthWechatAppSecret; }
    public Boolean getOauthWechatAppSecretSet() { return oauthWechatAppSecretSet; }
    public void setOauthWechatAppSecretSet(Boolean oauthWechatAppSecretSet) { this.oauthWechatAppSecretSet = oauthWechatAppSecretSet; }
    public String getOauthQqEnabled() { return oauthQqEnabled; }
    public void setOauthQqEnabled(String oauthQqEnabled) { this.oauthQqEnabled = oauthQqEnabled; }
    public String getOauthQqAppId() { return oauthQqAppId; }
    public void setOauthQqAppId(String oauthQqAppId) { this.oauthQqAppId = oauthQqAppId; }
    public String getOauthQqAppKey() { return oauthQqAppKey; }
    public void setOauthQqAppKey(String oauthQqAppKey) { this.oauthQqAppKey = oauthQqAppKey; }
    public Boolean getOauthQqAppKeySet() { return oauthQqAppKeySet; }
    public void setOauthQqAppKeySet(Boolean oauthQqAppKeySet) { this.oauthQqAppKeySet = oauthQqAppKeySet; }
}
