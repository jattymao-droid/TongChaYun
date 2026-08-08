package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.SysBasicSettings;

public interface ISysBasicService
{
    SysBasicSettings getSettings(boolean maskSecret);

    SysBasicSettings getSiteInfo();

    void saveSettings(SysBasicSettings settings, String operator);

    void sendTestMail(String to);

    /**
     * 业务通知邮件（答卷/截止/发布）。邮件未开启时静默跳过并返回 false。
     */
    boolean sendBizMail(String to, String subject, String html);

    /** 站点标题（邮件署名） */
    String getSiteTitle();

    void sendRegisterCode(String email, String captchaCode, String captchaUuid);

    void validateRegisterEmailCode(String email, String code);

    void sendResetPasswordCode(String email, String captchaCode, String captchaUuid);

    void validateResetEmailCode(String email, String code);
}
