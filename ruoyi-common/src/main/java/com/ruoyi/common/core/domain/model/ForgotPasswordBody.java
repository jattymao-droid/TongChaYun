package com.ruoyi.common.core.domain.model;

/**
 * Forgot password / email reset body
 */
public class ForgotPasswordBody extends LoginBody
{
    private String email;

    private String emailCode;

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getEmailCode()
    {
        return emailCode;
    }

    public void setEmailCode(String emailCode)
    {
        this.emailCode = emailCode;
    }
}
