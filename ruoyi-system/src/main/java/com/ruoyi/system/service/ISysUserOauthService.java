package com.ruoyi.system.service;

import com.ruoyi.system.domain.SysUserOauth;

public interface ISysUserOauthService
{
    SysUserOauth selectByProviderOpenId(String provider, String openId);

    int insertOauth(SysUserOauth oauth);

    int updateOauth(SysUserOauth oauth);
}
