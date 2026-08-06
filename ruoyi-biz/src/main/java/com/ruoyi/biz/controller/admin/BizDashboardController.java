package com.ruoyi.biz.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.biz.service.IBizDashboardService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;

@RestController
@RequestMapping("/biz/dashboard")
public class BizDashboardController extends BaseController
{
    @Autowired
    private IBizDashboardService dashboardService;

    @PreAuthorize("@ss.hasPermi('biz:dashboard:list') or @ss.hasPermi('biz:query:list') or @ss.hasPermi('biz:survey:list')")
    @GetMapping("/overview")
    public AjaxResult overview()
    {
        return success(dashboardService.overview());
    }
}
