package com.ruoyi.biz.controller.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.biz.domain.BizQuery;
import com.ruoyi.biz.domain.BizQueryDataset;
import com.ruoyi.biz.domain.BizQueryField;
import com.ruoyi.biz.domain.BizQueryPage;
import com.ruoyi.biz.domain.BizQueryRelation;
import com.ruoyi.biz.domain.BizQueryRow;
import com.ruoyi.biz.domain.vo.BizQueryDetailVo;
import com.ruoyi.biz.service.IBizQueryService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;

@RestController
@RequestMapping("/biz/query")
public class BizQueryController extends BaseController
{
    @Autowired
    private IBizQueryService queryService;

    @PreAuthorize("@ss.hasPermi('biz:query:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizQuery query)
    {
        startPage();
        List<BizQuery> list = queryService.selectBizQueryList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('biz:query:query')")
    @GetMapping("/{queryId}")
    public AjaxResult getInfo(@PathVariable Long queryId)
    {
        return success(queryService.selectDetail(queryId));
    }

    @PreAuthorize("@ss.hasPermi('biz:query:add')")
    @Log(title = "查询项目", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizQuery query)
    {
        return success(queryService.insertBizQuery(query));
    }

    @PreAuthorize("@ss.hasPermi('biz:query:edit')")
    @Log(title = "查询项目", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizQuery query)
    {
        return toAjax(queryService.updateBizQuery(query));
    }

    @PreAuthorize("@ss.hasPermi('biz:query:remove')")
    @Log(title = "查询项目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{queryIds}")
    public AjaxResult remove(@PathVariable Long[] queryIds)
    {
        return toAjax(queryService.deleteBizQueryByIds(queryIds));
    }

    @PreAuthorize("@ss.hasPermi('biz:query:edit')")
    @Log(title = "查询上传Excel", businessType = BusinessType.UPDATE)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("queryId") Long queryId,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "mode", required = false, defaultValue = "replace") String mode) throws Exception
    {
        return success(queryService.uploadExcel(queryId, file, mode));
    }

    @PreAuthorize("@ss.hasPermi('biz:query:export') or @ss.hasPermi('biz:query:query')")
    @Log(title = "查询导出", businessType = BusinessType.EXPORT)
    @PostMapping("/export/{queryId}")
    public void export(@PathVariable Long queryId, jakarta.servlet.http.HttpServletResponse response) throws Exception
    {
        queryService.exportRows(queryId, response);
    }

    @PreAuthorize("@ss.hasPermi('biz:query:edit')")
    @Log(title = "查询字段配置", businessType = BusinessType.UPDATE)
    @PutMapping("/fields/{queryId}")
    public AjaxResult saveFields(@PathVariable Long queryId, @RequestBody List<BizQueryField> fields)
    {
        return toAjax(queryService.saveFields(queryId, fields));
    }

    @PreAuthorize("@ss.hasPermi('biz:query:edit')")
    @Log(title = "查询页面设计", businessType = BusinessType.UPDATE)
    @PutMapping("/page")
    public AjaxResult savePage(@RequestBody BizQueryPage page)
    {
        return toAjax(queryService.savePage(page));
    }

    @PreAuthorize("@ss.hasPermi('biz:query:publish')")
    @Log(title = "查询发布", businessType = BusinessType.UPDATE)
    @PostMapping("/publish/{queryId}")
    public AjaxResult publish(@PathVariable Long queryId)
    {
        String code = queryService.publish(queryId);
        Map<String, Object> data = new HashMap<>();
        data.put("publicCode", code);
        data.put("path", "/q/" + code);
        return success(data);
    }

    @PreAuthorize("@ss.hasPermi('biz:query:publish')")
    @Log(title = "查询停用", businessType = BusinessType.UPDATE)
    @PostMapping("/offline/{queryId}")
    public AjaxResult offline(@PathVariable Long queryId)
    {
        return toAjax(queryService.offline(queryId));
    }

    @PreAuthorize("@ss.hasPermi('biz:query:query')")
    @GetMapping("/link/{queryId}")
    public AjaxResult link(@PathVariable Long queryId)
    {
        BizQueryDetailVo detail = queryService.selectDetail(queryId);
        BizQuery q = detail.getQuery();
        Map<String, Object> data = new HashMap<>();
        data.put("publicCode", q.getPublicCode());
        data.put("status", q.getStatus());
        data.put("path", q.getPublicCode() == null ? null : "/q/" + q.getPublicCode());
        return success(data);
    }

    @PreAuthorize("@ss.hasPermi('biz:query:query')")
    @GetMapping("/preview/{queryId}/meta")
    public AjaxResult previewMeta(@PathVariable Long queryId)
    {
        return success(queryService.previewMeta(queryId));
    }

    @PreAuthorize("@ss.hasPermi('biz:query:query')")
    @PostMapping("/preview/{queryId}/search")
    public TableDataInfo previewSearch(@PathVariable Long queryId, @RequestBody(required = false) Map<String, Object> body)
    {
        Map<String, Object> params = new HashMap<>();
        int pageNum = 1;
        int pageSize = 10;
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
            Object p = body.get("params");
            if (p instanceof Map)
            {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) p;
                params.putAll(m);
            }
        }
        pageSize = Math.min(Math.max(pageSize, 1), 100);
        List<BizQueryRow> list = queryService.previewSearch(queryId, params, pageNum, pageSize);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('biz:query:add')")
    @Log(title = "查询复制", businessType = BusinessType.INSERT)
    @PostMapping("/copy/{queryId}")
    public AjaxResult copy(@PathVariable Long queryId)
    {
        return success(queryService.copyQuery(queryId));
    }

    @PreAuthorize("@ss.hasPermi('biz:query:query')")
    @GetMapping("/dist/{queryId}")
    public AjaxResult fieldDist(@PathVariable Long queryId, @RequestParam("fieldKey") String fieldKey)
    {
        return success(queryService.fieldDist(queryId, fieldKey));
    }

    @PreAuthorize("@ss.hasPermi('biz:query:query')")
    @GetMapping("/sample/{queryId}")
    public AjaxResult sampleRows(@PathVariable Long queryId,
        @RequestParam(value = "limit", defaultValue = "5") Integer limit)
    {
        return success(queryService.sampleRows(queryId, limit == null ? 5 : limit));
    }

    @PreAuthorize("@ss.hasPermi('biz:query:add')")
    @GetMapping("/templates")
    public AjaxResult templates()
    {
        return success(queryService.listTemplates());
    }

    @PreAuthorize("@ss.hasPermi('biz:query:add')")
    @Log(title = "查询模板创建", businessType = BusinessType.INSERT)
    @PostMapping("/fromTemplate/{key}")
    public AjaxResult fromTemplate(@PathVariable("key") String key)
    {
        return success(queryService.createFromTemplate(key));
    }

    @PreAuthorize("@ss.hasPermi('biz:query:query')")
    @GetMapping("/{queryId}/datasets")
    public AjaxResult listDatasets(@PathVariable Long queryId)
    {
        return success(queryService.listDatasets(queryId));
    }

    @PreAuthorize("@ss.hasPermi('biz:query:edit')")
    @Log(title = "查询数据表上传", businessType = BusinessType.UPDATE)
    @PostMapping("/{queryId}/datasets/upload")
    public AjaxResult uploadDataset(@PathVariable Long queryId,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "datasetName", required = false) String datasetName,
        @RequestParam(value = "isPrimary", required = false) String isPrimary,
        @RequestParam(value = "mode", required = false, defaultValue = "replace") String mode) throws Exception
    {
        return success(queryService.uploadDataset(queryId, file, datasetName, isPrimary, mode));
    }

    @PreAuthorize("@ss.hasPermi('biz:query:edit')")
    @Log(title = "查询数据表更新", businessType = BusinessType.UPDATE)
    @PutMapping("/datasets")
    public AjaxResult updateDataset(@RequestBody BizQueryDataset dataset)
    {
        return toAjax(queryService.updateDataset(dataset));
    }

    @PreAuthorize("@ss.hasPermi('biz:query:edit')")
    @Log(title = "查询数据表删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/{queryId}/datasets/{datasetId}")
    public AjaxResult deleteDataset(@PathVariable Long queryId, @PathVariable Long datasetId)
    {
        return toAjax(queryService.deleteDataset(queryId, datasetId));
    }

    @PreAuthorize("@ss.hasPermi('biz:query:query')")
    @GetMapping("/{queryId}/relations")
    public AjaxResult listRelations(@PathVariable Long queryId)
    {
        return success(queryService.listRelations(queryId));
    }

    @PreAuthorize("@ss.hasPermi('biz:query:edit')")
    @Log(title = "查询关联配置", businessType = BusinessType.UPDATE)
    @PutMapping("/{queryId}/relations")
    public AjaxResult saveRelations(@PathVariable Long queryId, @RequestBody List<BizQueryRelation> relations)
    {
        return toAjax(queryService.saveRelations(queryId, relations));
    }

    @PreAuthorize("@ss.hasPermi('biz:query:edit')")
    @Log(title = "查询关联物化", businessType = BusinessType.UPDATE)
    @PostMapping("/{queryId}/materialize")
    public AjaxResult materialize(@PathVariable Long queryId)
    {
        return success(queryService.materializeJoin(queryId));
    }

    @PreAuthorize("@ss.hasPermi('biz:query:query')")
    @GetMapping("/{queryId}/access-logs")
    public AjaxResult accessLogs(@PathVariable Long queryId,
        @RequestParam(value = "action", required = false) String action,
        @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit)
    {
        return success(queryService.listAccessLogs(queryId, action, limit == null ? 50 : limit));
    }

    @PreAuthorize("@ss.hasPermi('biz:user:transfer')")
    @Log(title = "查询转让归属", businessType = BusinessType.UPDATE)
    @PutMapping("/{queryId}/transfer/{targetUserId}")
    public AjaxResult transfer(@PathVariable Long queryId, @PathVariable Long targetUserId)
    {
        return toAjax(queryService.transferOwnership(queryId, targetUserId));
    }
}
