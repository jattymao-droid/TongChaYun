package com.ruoyi.system.mapper;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.SysUserOauth;

/**
 * OAuth binding mapper
 */
public interface SysUserOauthMapper
{
    SysUserOauth selectByProviderOpenId(@Param("provider") String provider, @Param("openId") String openId);

    int insertOauth(SysUserOauth oauth);

    int updateOauth(SysUserOauth oauth);
}
