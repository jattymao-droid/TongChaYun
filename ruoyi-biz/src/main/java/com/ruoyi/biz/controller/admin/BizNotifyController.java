package com.ruoyi.biz.controller.admin;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.biz.domain.BizSurveyNotify;
import com.ruoyi.biz.service.IBizNotifyService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/biz/notify")
public class BizNotifyController extends BaseController
{
    @Autowired
    private IBizNotifyService notifyService;

    @PreAuthorize("@ss.hasPermi('biz:dashboard:list') or @ss.hasPermi('biz:survey:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizSurveyNotify query)
    {
        startPage();
        List<BizSurveyNotify> list = notifyService.selectList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('biz:dashboard:list') or @ss.hasPermi('biz:survey:list')")
    @GetMapping("/listTop")
    public AjaxResult listTop()
    {
        AjaxResult ajax = success(notifyService.selectTop(10));
        ajax.put("unreadCount", notifyService.countUnread());
        return ajax;
    }

    @PreAuthorize("@ss.hasPermi('biz:dashboard:list') or @ss.hasPermi('biz:survey:list')")
    @GetMapping("/unreadCount")
    public AjaxResult unreadCount()
    {
        return success(notifyService.countUnread());
    }

    @PreAuthorize("@ss.hasPermi('biz:dashboard:list') or @ss.hasPermi('biz:survey:list')")
    @PutMapping("/read/{notifyId}")
    public AjaxResult read(@PathVariable Long notifyId)
    {
        return toAjax(notifyService.markRead(notifyId));
    }

    @PreAuthorize("@ss.hasPermi('biz:dashboard:list') or @ss.hasPermi('biz:survey:list')")
    @PutMapping("/readAll")
    public AjaxResult readAll()
    {
        return toAjax(notifyService.markAllRead());
    }
}
