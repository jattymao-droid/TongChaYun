package com.ruoyi.biz.controller.admin;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.biz.service.IBizRiskService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;

@RestController
@RequestMapping("/biz/risk")
public class BizRiskController extends BaseController
{
    @Autowired
    private IBizRiskService riskService;

    @PreAuthorize("@ss.hasPermi('biz:survey:query')")
    @GetMapping("/survey/{surveyId}/board")
    public AjaxResult board(@PathVariable Long surveyId)
    {
        return success(riskService.surveyBoard(surveyId));
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:query')")
    @GetMapping("/survey/{surveyId}/blacklist")
    public AjaxResult list(@PathVariable Long surveyId)
    {
        return success(riskService.listBlacklist("survey", surveyId));
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:edit')")
    @Log(title = "Survey blacklist", businessType = BusinessType.INSERT)
    @PostMapping("/survey/{surveyId}/blacklist")
    public AjaxResult add(@PathVariable Long surveyId, @RequestBody Map<String, Object> body)
    {
        String kind = body == null || body.get("kind") == null ? null : String.valueOf(body.get("kind"));
        String value = body == null || body.get("value") == null ? null : String.valueOf(body.get("value"));
        String reason = body == null || body.get("reason") == null ? "" : String.valueOf(body.get("reason"));
        boolean markInvalid = false;
        if (body != null && body.get("markInvalid") != null)
        {
            Object raw = body.get("markInvalid");
            markInvalid = Boolean.TRUE.equals(raw) || "1".equals(String.valueOf(raw))
                || "true".equalsIgnoreCase(String.valueOf(raw));
        }
        if (StringUtils.isEmpty(kind) || StringUtils.isEmpty(value))
        {
            return error("kind/value required");
        }
        riskService.addBlacklist("survey", surveyId, kind, value, reason, markInvalid);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('biz:survey:edit')")
    @Log(title = "Survey blacklist", businessType = BusinessType.DELETE)
    @DeleteMapping("/survey/{surveyId}/blacklist/{id}")
    public AjaxResult remove(@PathVariable Long surveyId, @PathVariable Long id)
    {
        return toAjax(riskService.removeBlacklist("survey", surveyId, id));
    }
}
