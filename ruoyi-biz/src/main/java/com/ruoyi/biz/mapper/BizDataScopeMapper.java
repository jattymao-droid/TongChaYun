package com.ruoyi.biz.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * Lightweight dept-scope checks against sys_dept / sys_role_dept.
 */
public interface BizDataScopeMapper
{
    int countDeptSelfOrChild(@Param("projectDeptId") Long projectDeptId, @Param("userDeptId") Long userDeptId);

    int countDeptInRole(@Param("projectDeptId") Long projectDeptId, @Param("roleId") Long roleId);
}
