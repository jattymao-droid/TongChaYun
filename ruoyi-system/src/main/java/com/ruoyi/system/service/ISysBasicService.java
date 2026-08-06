package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.SysBasicSettings;

public interface ISysBasicService
{
    SysBasicSettings getSettings(boolean maskSecret);

    SysBasicSettings getSiteInfo();

    void saveSettings(SysBasicSettings settings, String operator);

    void sendTestMail(String to);

    void sendRegisterCode(String email, String captchaCode, String captchaUuid);

    void validateRegisterEmailCode(String email, String code);

    void sendResetPasswordCode(String email, String captchaCode, String captchaUuid);

    void validateResetEmailCode(String email, String code);
}
