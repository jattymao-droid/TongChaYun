package com.ruoyi.biz.controller.open;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.biz.domain.BizQueryRow;
import com.ruoyi.biz.service.IBizQueryService;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.LimitType;

@RestController
@RequestMapping("/open/query")
public class OpenQueryController extends BaseController
{
    @Autowired
    private IBizQueryService queryService;

    @RateLimiter(time = 60, count = 60, limitType = LimitType.IP)
    @GetMapping("/{code}/meta")
    public AjaxResult meta(@PathVariable("code") String code,
        @RequestParam(value = "accessPwd", required = false) String accessPwd)
    {
        return success(queryService.openMeta(code, accessPwd));
    }

    @RateLimiter(time = 60, count = 60, limitType = LimitType.IP)
    @PostMapping("/{code}/search")
    public TableDataInfo search(@PathVariable("code") String code, @RequestBody(required = false) Map<String, Object> body)
    {
        Map<String, Object> parsed = parseBody(body);
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) parsed.get("params");
        int pageNum = (Integer) parsed.get("pageNum");
        int pageSize = (Integer) parsed.get("pageSize");
        String accessPwd = (String) parsed.get("accessPwd");
        String captchaCode = (String) parsed.get("captchaCode");
        String captchaUuid = (String) parsed.get("captchaUuid");
        List<BizQueryRow> list = queryService.openSearch(code, params, pageNum, pageSize, accessPwd, captchaCode, captchaUuid);
        return getDataTable(list);
    }

    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    @PostMapping("/{code}/export")
    public void export(@PathVariable("code") String code, @RequestBody(required = false) Map<String, Object> body,
        HttpServletResponse response) throws Exception
    {
        Map<String, Object> parsed = parseBody(body);
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) parsed.get("params");
        String accessPwd = (String) parsed.get("accessPwd");
        String captchaCode = (String) parsed.get("captchaCode");
        String captchaUuid = (String) parsed.get("captchaUuid");
        queryService.openExport(code, params, accessPwd, response, captchaCode, captchaUuid);
    }

    @RateLimiter(time = 60, count = 6, limitType = LimitType.IP)
    @PostMapping("/{code}/exportPdf")
    public void exportPdf(@PathVariable("code") String code, @RequestBody(required = false) Map<String, Object> body,
        HttpServletResponse response) throws Exception
    {
        Map<String, Object> parsed = parseBody(body);
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) parsed.get("params");
        String accessPwd = (String) parsed.get("accessPwd");
        String captchaCode = (String) parsed.get("captchaCode");
        String captchaUuid = (String) parsed.get("captchaUuid");
        queryService.openExportPdf(code, params, accessPwd, response, captchaCode, captchaUuid);
    }

    @RateLimiter(time = 60, count = 60, limitType = LimitType.IP)
    @PostMapping("/{code}/dist")
    public AjaxResult dist(@PathVariable("code") String code, @RequestBody(required = false) Map<String, Object> body)
    {
        Map<String, Object> parsed = parseBody(body);
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) parsed.get("params");
        String accessPwd = (String) parsed.get("accessPwd");
        String captchaCode = (String) parsed.get("captchaCode");
        String captchaUuid = (String) parsed.get("captchaUuid");
        String fieldKey = body == null || body.get("fieldKey") == null ? null : String.valueOf(body.get("fieldKey"));
        return success(queryService.openFieldDist(code, fieldKey, params, accessPwd, captchaCode, captchaUuid));
    }

    private Map<String, Object> parseBody(Map<String, Object> body)
    {
        Map<String, Object> params = new HashMap<>();
        int pageNum = 1;
        int pageSize = 10;
        String accessPwd = null;
        String captchaCode = null;
        String captchaUuid = null;
        if (body != null)
        {
            if (body.get("pageNum") != null)
            {
                pageNum = Integer.parseInt(String.valueOf(body.get("pageNum")));
            }
            if (body.get("pageSize") != null)
            {
                pageSize = Integer.parseInt(String.valueOf(body.get("pageSize")));
            }
            if (body.get("accessPwd") != null)
            {
                accessPwd = String.valueOf(body.get("accessPwd"));
            }
            if (body.get("code") != null)
            {
                captchaCode = String.valueOf(body.get("code"));
            }
            if (body.get("uuid") != null)
            {
                captchaUuid = String.valueOf(body.get("uuid"));
            }
            Object p = body.get("params");
            if (p instanceof Map)
            {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) p;
                params.putAll(m);
            }
            for (Map.Entry<String, Object> e : body.entrySet())
            {
                if (e.getKey().matches("^c\\d+$"))
                {
                    params.put(e.getKey(), e.getValue());
                }
            }
        }
        pageSize = Math.min(Math.max(pageSize, 1), 100);
        Map<String, Object> out = new HashMap<>();
        out.put("params", params);
        out.put("pageNum", pageNum);
        out.put("pageSize", pageSize);
        out.put("accessPwd", accessPwd);
        out.put("captchaCode", captchaCode);
        out.put("captchaUuid", captchaUuid);
        return out;
    }
}
