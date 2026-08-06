package com.ruoyi.system.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.SysUserOauth;
import com.ruoyi.system.mapper.SysUserOauthMapper;
import com.ruoyi.system.service.ISysUserOauthService;

@Service
public class SysUserOauthServiceImpl implements ISysUserOauthService
{
    @Autowired
    private SysUserOauthMapper userOauthMapper;

    @Override
    public SysUserOauth selectByProviderOpenId(String provider, String openId)
    {
        return userOauthMapper.selectByProviderOpenId(provider, openId);
    }

    @Override
    public int insertOauth(SysUserOauth oauth)
    {
        return userOauthMapper.insertOauth(oauth);
    }

    @Override
    public int updateOauth(SysUserOauth oauth)
    {
        return userOauthMapper.updateOauth(oauth);
    }
}
