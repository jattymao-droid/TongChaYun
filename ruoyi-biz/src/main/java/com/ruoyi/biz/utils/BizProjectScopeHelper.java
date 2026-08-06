package com.ruoyi.biz.utils;

import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.biz.mapper.BizDataScopeMapper;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * Align detail/update access with RuoYi role dataScope (same rules as list @DataScope).
 */
@Component
public class BizProjectScopeHelper
{
    @Autowired
    private BizDataScopeMapper dataScopeMapper;

    public void assertAccess(Long createUserId, Long deptId, String permission, String denyMsg)
    {
        if (SecurityUtils.isAdmin())
        {
            return;
        }
        SysUser user = SecurityUtils.getLoginUser().getUser();
        if (user == null)
        {
            throw new ServiceException(denyMsg);
        }
        if (createUserId != null && createUserId.equals(user.getUserId()))
        {
            return;
        }
        if (user.getRoles() == null || user.getRoles().isEmpty())
        {
            throw new ServiceException(denyMsg);
        }

        Set<String> seen = new HashSet<>();
        boolean anyRoleMatchedPerm = false;
        for (SysRole role : user.getRoles())
        {
            if (role == null || StringUtils.equals(role.getStatus(), UserConstants.ROLE_DISABLE))
            {
                continue;
            }
            if (StringUtils.isNotEmpty(permission)
                && (role.getPermissions() == null
                    || !StringUtils.containsAny(role.getPermissions(), Convert.toStrArray(permission))))
            {
                continue;
            }
            anyRoleMatchedPerm = true;
            String dataScope = role.getDataScope();
            if (seen.contains(dataScope))
            {
                continue;
            }
            seen.add(dataScope);

            if (Constants.Dept.DATA_SCOPE_ALL.equals(dataScope))
            {
                return;
            }
            if (Constants.Dept.DATA_SCOPE_SELF.equals(dataScope))
            {
                if (createUserId != null && createUserId.equals(user.getUserId()))
                {
                    return;
                }
                continue;
            }
            if (deptId == null)
            {
                continue;
            }
            if (Constants.Dept.DATA_SCOPE_DEPT.equals(dataScope))
            {
                if (user.getDeptId() != null && user.getDeptId().equals(deptId))
                {
                    return;
                }
            }
            else if (Constants.Dept.DATA_SCOPE_DEPT_AND_CHILD.equals(dataScope))
            {
                if (user.getDeptId() != null
                    && dataScopeMapper.countDeptSelfOrChild(deptId, user.getDeptId()) > 0)
                {
                    return;
                }
            }
            else if (Constants.Dept.DATA_SCOPE_CUSTOM.equals(dataScope))
            {
                if (role.getRoleId() != null
                    && dataScopeMapper.countDeptInRole(deptId, role.getRoleId()) > 0)
                {
                    return;
                }
            }
        }
        if (!anyRoleMatchedPerm)
        {
            throw new ServiceException(denyMsg);
        }
        throw new ServiceException(denyMsg);
    }
}
