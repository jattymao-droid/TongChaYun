package com.ruoyi.biz.controller.admin;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.biz.domain.vo.BizUserProjectVo;
import com.ruoyi.biz.service.IBizUserProjectService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * Admin hub: users and their query/survey project counts.
 */
@RestController
@RequestMapping("/biz/user-projects")
public class BizUserProjectController extends BaseController
{
    @Autowired
    private IBizUserProjectService userProjectService;

    @PreAuthorize("@ss.hasPermi('biz:user:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizUserProjectVo query)
    {
        startPage();
        List<BizUserProjectVo> list = userProjectService.selectUserProjectList(query);
        return getDataTable(list);
    }
}
