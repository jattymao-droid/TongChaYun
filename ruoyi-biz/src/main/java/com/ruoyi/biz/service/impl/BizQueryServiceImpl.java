package com.ruoyi.biz.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.github.pagehelper.PageHelper;
import com.ruoyi.biz.domain.BizQuery;
import com.ruoyi.biz.domain.BizQueryDataset;
import com.ruoyi.biz.domain.BizQueryDatasetRow;
import com.ruoyi.biz.domain.BizQueryField;
import com.ruoyi.biz.domain.BizQueryPage;
import com.ruoyi.biz.domain.BizQueryRelation;
import com.ruoyi.biz.domain.BizQueryRow;
import com.ruoyi.biz.domain.vo.BizQueryDetailVo;
import com.ruoyi.biz.mapper.BizQueryDatasetMapper;
import com.ruoyi.biz.mapper.BizQueryDatasetRowMapper;
import com.ruoyi.biz.mapper.BizQueryFieldMapper;
import com.ruoyi.biz.mapper.BizAccessLogMapper;
import com.ruoyi.biz.mapper.BizQueryAdminMapper;
import com.ruoyi.biz.mapper.BizQueryMapper;
import com.ruoyi.biz.mapper.BizQueryPageMapper;
import com.ruoyi.biz.mapper.BizQueryRelationMapper;
import com.ruoyi.biz.mapper.BizQueryRowMapper;
import com.ruoyi.biz.service.IBizQueryService;
import com.ruoyi.biz.service.IBizUserProjectService;
import com.ruoyi.biz.service.IBizVersionService;
import com.ruoyi.biz.utils.BizAccessLogHelper;
import com.ruoyi.biz.utils.BizAccessPwdHelper;
import com.ruoyi.biz.utils.BizMaskHelper;
import com.ruoyi.biz.utils.BizProjectScopeHelper;
import com.ruoyi.biz.utils.BizQueryFieldInferHelper;
import com.ruoyi.biz.utils.BizQueryIndexHelper;
import com.ruoyi.biz.utils.BizQueryJoinHelper;
import com.ruoyi.biz.utils.BizQueryPdfHelper;
import com.ruoyi.biz.utils.BizQueryTemplates;
import com.ruoyi.biz.utils.OpenQueryGuard;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.common.constant.Constants;

@Service
public class BizQueryServiceImpl implements IBizQueryService
{
    private static final int MAX_ROWS = 20000;
    private static final int MAX_COLS = 50;
    /** files larger than this parse async */
    private static final long ASYNC_THRESHOLD = 200 * 1024L;
    private static final String CODE_CHARS = "abcdefghjkmnpqrstuvwxyz23456789";
    private static final Set<String> ALLOWED_QUERY_OPS = new HashSet<>(
        Arrays.asList("EQ", "LIKE", "BETWEEN", "GT", "GTE", "LT", "LTE", "IN"));

    @Autowired
    private BizQueryMapper queryMapper;
    @Autowired
    private BizProjectScopeHelper projectScopeHelper;
    @Autowired
    private BizQueryFieldMapper fieldMapper;
    @Autowired
    private BizQueryRowMapper rowMapper;
    @Autowired
    private BizQueryPageMapper pageMapper;
    @Autowired
    private BizQueryDatasetMapper datasetMapper;
    @Autowired
    private BizQueryDatasetRowMapper datasetRowMapper;
    @Autowired
    private BizQueryRelationMapper relationMapper;
    @Autowired
    private BizAccessLogMapper accessLogMapper;
    @Autowired
    private IBizUserProjectService userProjectService;
    @Autowired
    private BizQueryAdminMapper queryAdminMapper;
    @Autowired
    private IBizVersionService versionService;

    @Override
    @DataScope(deptAlias = "q", userAlias = "q", userField = "create_user_id", permission = "biz:query:list")
    public List<BizQuery> selectBizQueryList(BizQuery query)
    {
        if (query.getParams() == null)
        {
            query.setParams(new java.util.HashMap<>());
        }
        try
        {
            query.getParams().put("loginUserId", SecurityUtils.getUserId());
        }
        catch (Exception ignored)
        {
        }
        List<BizQuery> list = queryMapper.selectBizQueryList(query);
        if (list != null)
        {
            for (BizQuery q : list)
            {
                if (q != null)
                {
                    q.setAccessPwd(BizAccessPwdHelper.maskForApi(q.getAccessPwd()));
                }
            }
        }
        return list;
    }

    @Override
    public BizQueryDetailVo selectDetail(Long queryId)
    {
        BizQuery query = requireQuery(queryId);
        checkOwner(query);
        return buildDetail(query);
    }

    @Override
    public BizQuery insertBizQuery(BizQuery query)
    {
        query.setStatus("0");
        query.setRowCount(0);
        query.setViewCount(0L);
        query.setSearchCount(0L);
        query.setParseStatus("0");
        query.setCreateUserId(SecurityUtils.getUserId());
        query.setDeptId(SecurityUtils.getDeptId());
        query.setCreateBy(SecurityUtils.getUsername());
        if (StringUtils.isEmpty(query.getQueryName()))
        {
            throw new ServiceException("查询名称不能为空");
        }
        query.setAccessPwd(BizAccessPwdHelper.encodeForStore(query.getAccessPwd()));
        queryMapper.insertBizQuery(query);

        BizQueryPage page = new BizQueryPage();
        page.setQueryId(query.getQueryId());
        page.setTitle(query.getQueryName());
        page.setSubtitle(StringUtils.nvl(query.getQueryDesc(), ""));
        page.setThemeColor("#1677ff");
        page.setResultTips("未查询到相关数据");
        pageMapper.insertPage(page);
        query.setAccessPwd(BizAccessPwdHelper.maskForApi(query.getAccessPwd()));
        return query;
    }

    @Override
    public int updateBizQuery(BizQuery query)
    {
        BizQuery db = requireQuery(query.getQueryId());
        checkOwner(db);
        query.setAccessPwd(BizAccessPwdHelper.prepareForUpdate(query.getAccessPwd()));
        query.setUpdateBy(SecurityUtils.getUsername());
        // ownership can only change via transferOwnership
        query.setCreateUserId(null);
        query.setCreateBy(null);
        if (query.getEndTime() != null
            && (db.getEndTime() == null || !query.getEndTime().equals(db.getEndTime())))
        {
            query.setRemindSent("0");
        }
        return queryMapper.updateBizQuery(query);
    }

    @Override
    @Transactional
    public int deleteBizQueryByIds(Long[] queryIds)
    {
        for (Long id : queryIds)
        {
            BizQuery db = requireQuery(id);
            assertQueryOwner(db);
            fieldMapper.deleteByQueryId(id);
            rowMapper.deleteByQueryId(id);
            pageMapper.deleteByQueryId(id);
            relationMapper.deleteByQueryId(id);
            datasetRowMapper.deleteByQueryId(id);
            datasetMapper.deleteByQueryId(id);
            queryAdminMapper.deleteByQueryId(id);
        }
        return queryMapper.deleteBizQueryByIds(queryIds);
    }

    @Override
    @Transactional
    public BizQueryDetailVo uploadExcel(Long queryId, MultipartFile file, String mode) throws Exception
    {
        uploadDataset(queryId, file, null, "1", mode);
        return materializeJoin(queryId);
    }

    private void parseExcelFile(Long queryId, String absPath, boolean append, String stored, String username) throws Exception
    {
        BizQuery query = requireQuery(queryId);
        List<BizQueryField> fields = new ArrayList<>();
        List<BizQueryRow> rows = new ArrayList<>();
        String sheetName;
        DataFormatter formatter = new DataFormatter();
        List<BizQueryField> existFields = fieldMapper.selectFieldsByQueryId(queryId);

        try (InputStream in = new FileInputStream(new File(absPath)); Workbook wb = WorkbookFactory.create(in))
        {
            Sheet sheet = wb.getSheetAt(0);
            sheetName = sheet.getSheetName();
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null)
            {
                throw new ServiceException("Excel 表头为空");
            }
            int colCount = Math.min(header.getLastCellNum(), MAX_COLS);
            if (colCount <= 0)
            {
                throw new ServiceException("未识别到有效表头");
            }

            if (append)
            {
                if (existFields == null || existFields.isEmpty())
                {
                    throw new ServiceException("请先覆盖上传建立字段后再追加");
                }
                if (colCount != existFields.size())
                {
                    throw new ServiceException("追加模式要求列数与现有字段一致");
                }
                fields = existFields;
            }
            else
            {
            for (int c = 0; c < colCount; c++)
            {
                String name = formatter.formatCellValue(header.getCell(c)).trim();
                if (StringUtils.isEmpty(name))
                {
                    name = "列" + (c + 1);
                }
                BizQueryField field = new BizQueryField();
                field.setQueryId(queryId);
                field.setFieldKey("c" + (c + 1));
                field.setFieldName(name);
                field.setFieldLabel(name);
                field.setDataType("string");
                field.setSort(c + 1);
                    BizQueryFieldInferHelper.apply(field, c);
                fields.add(field);
                }
                // ensure at least one required query field
                boolean hasRequired = fields.stream().anyMatch(f -> "1".equals(f.getIsQuery()) && !"0".equals(f.getIsRequired()));
                if (!hasRequired)
                {
                    for (BizQueryField f : fields)
                    {
                        if ("1".equals(f.getIsQuery()))
                        {
                            f.setIsRequired("1");
                            break;
                        }
                    }
                }
            }

            int startNo = append ? (rowMapper.selectMaxRowNo(queryId) == null ? 0 : rowMapper.selectMaxRowNo(queryId)) : 0;
            int existCount = append ? (query.getRowCount() == null ? 0 : query.getRowCount()) : 0;
            int firstData = sheet.getFirstRowNum() + 1;
            int last = sheet.getLastRowNum();
            int dataRows = 0;
            for (int r = firstData; r <= last; r++)
            {
                Row row = sheet.getRow(r);
                if (row == null || isEmptyRow(row, colCount, formatter))
                {
                    continue;
                }
                dataRows++;
                if (existCount + dataRows > MAX_ROWS)
                {
                    throw new ServiceException("数据行数不能超过 " + MAX_ROWS);
                }
                Map<String, String> map = new LinkedHashMap<>();
                for (int c = 0; c < colCount; c++)
                {
                    map.put(fields.get(c).getFieldKey(), formatter.formatCellValue(row.getCell(c)).trim());
                }
                BizQueryRow qr = new BizQueryRow();
                qr.setQueryId(queryId);
                qr.setRowNo(startNo + dataRows);
                qr.setRowData(JSON.toJSONString(map));
                rows.add(qr);
            }
            if (rows.isEmpty())
            {
                throw new ServiceException("Excel 无有效数据行");
            }
        }

        if (!append)
        {
        fieldMapper.deleteByQueryId(queryId);
        rowMapper.deleteByQueryId(queryId);
        fieldMapper.batchInsertFields(fields);
        }
        int batch = 500;
        for (int i = 0; i < rows.size(); i += batch)
        {
            rowMapper.batchInsertRows(rows.subList(i, Math.min(i + batch, rows.size())));
        }

        int totalRows = append
            ? (query.getRowCount() == null ? 0 : query.getRowCount()) + rows.size()
            : rows.size();
        BizQuery upd = new BizQuery();
        upd.setQueryId(queryId);
        upd.setSourceFile(stored);
        upd.setSheetName(sheetName);
        upd.setRowCount(totalRows);
        upd.setParseStatus("0");
        upd.setParseMsg("解析完成");
        upd.setUpdateBy(username);
        queryMapper.updateBizQuery(upd);

        BizQueryPage page = pageMapper.selectByQueryId(queryId);
        if (page != null && StringUtils.isEmpty(page.getTitle()))
        {
            page.setTitle(query.getQueryName());
            pageMapper.updatePage(page);
        }
    }

    private String toLocalPath(String stored)
    {
        if (StringUtils.isEmpty(stored))
        {
            throw new ServiceException("文件路径无效");
        }
        if (stored.startsWith(Constants.RESOURCE_PREFIX))
        {
            return RuoYiConfig.getProfile() + stored.substring(Constants.RESOURCE_PREFIX.length());
        }
        if (stored.startsWith("/"))
        {
            return RuoYiConfig.getProfile() + stored;
        }
        return RuoYiConfig.getUploadPath() + "/" + stored;
    }

    @Override
    @Transactional
    public int saveFields(Long queryId, List<BizQueryField> fields)
    {
        BizQuery query = requireQuery(queryId);
        checkOwner(query);
        if (fields == null || fields.isEmpty())
        {
            throw new ServiceException("字段不能为空");
        }
        boolean seenDefaultSort = false;
        for (BizQueryField f : fields)
        {
            f.setQueryId(queryId);
            String op = StringUtils.isEmpty(f.getQueryType()) ? "EQ" : f.getQueryType().trim().toUpperCase();
            if (!ALLOWED_QUERY_OPS.contains(op))
            {
                throw new ServiceException("不支持的匹配方式: " + f.getQueryType());
            }
            f.setQueryType(op);
            if ("1".equals(f.getIsSortable()))
            {
                if (seenDefaultSort)
                {
                    f.setIsSortable("0");
                }
                else
                {
                    seenDefaultSort = true;
                }
            }
            else if (f.getIsSortable() == null)
            {
                f.setIsSortable("0");
            }
            if (StringUtils.isEmpty(f.getIsRequired()))
            {
                f.setIsRequired("1");
            }
            if (!"0".equals(f.getIsRequired()))
            {
                f.setIsRequired("1");
            }
            if (StringUtils.isEmpty(f.getMaskType()))
            {
                f.setMaskType("none");
            }
            String mask = f.getMaskType().trim().toLowerCase();
            if (!Arrays.asList("none", "phone", "idcard", "name", "email").contains(mask))
            {
                mask = "none";
            }
            f.setMaskType(mask);
            if (!"1".equals(f.getIsQuery()))
            {
                f.setIsRequired("0");
            }
            fieldMapper.updateField(f);
        }
        return fields.size();
    }

    @Override
    public int savePage(BizQueryPage page)
    {
        if (page.getQueryId() == null)
        {
            throw new ServiceException("queryId 不能为空");
        }
        BizQuery query = requireQuery(page.getQueryId());
        checkOwner(query);
        BizQueryPage db = pageMapper.selectByQueryId(page.getQueryId());
        if (db == null)
        {
            return pageMapper.insertPage(page);
        }
        page.setPageId(db.getPageId());
        return pageMapper.updatePage(page);
    }

    @Override
    public String publish(Long queryId)
    {
        BizQuery query = requireQuery(queryId);
        checkOwner(query);
        return doPublish(query, SecurityUtils.getUsername());
    }

    @Override
    public String publishInternal(Long queryId, String updateBy)
    {
        BizQuery query = requireQuery(queryId);
        return doPublish(query, StringUtils.isEmpty(updateBy) ? "system" : updateBy);
    }

    private String doPublish(BizQuery query, String updateBy)
    {
        Long queryId = query.getQueryId();
        List<BizQueryField> fields = fieldMapper.selectFieldsByQueryId(queryId);
        assertPublishReady(query, fields, "发布");
        String code = query.getPublicCode();
        if (StringUtils.isEmpty(code))
        {
            code = genUniqueCode();
        }
        BizQuery upd = new BizQuery();
        upd.setQueryId(queryId);
        upd.setPublicCode(code);
        upd.setStatus("1");
        upd.setUpdateBy(updateBy);
        upd.getParams().put("clearPublishAt", true);
        queryMapper.updateBizQuery(upd);
        BizQueryIndexHelper.refreshEqIndexes(queryId, fields);
        return code;
    }

    /** Shared publish readiness: data + query/list fields + at least one required query field. */
    static void assertPublishReady(BizQuery query, List<BizQueryField> fields, String actionLabel)
    {
        if (fields == null || fields.isEmpty() || query.getRowCount() == null || query.getRowCount() <= 0)
        {
            throw new ServiceException("请先上传数据再" + actionLabel);
        }
        boolean hasQuery = fields.stream().anyMatch(f -> "1".equals(f.getIsQuery()));
        boolean hasList = fields.stream().anyMatch(f -> "1".equals(f.getIsList()));
        boolean hasRequired = fields.stream().anyMatch(f -> "1".equals(f.getIsQuery()) && !"0".equals(f.getIsRequired()));
        if (!hasQuery)
        {
            throw new ServiceException("请至少配置一个查询条件字段后再" + actionLabel);
        }
        if (!hasRequired)
        {
            throw new ServiceException("请至少配置一个必填查询条件后再" + actionLabel + "（防撞库）");
        }
        if (!hasList)
        {
            throw new ServiceException("请至少配置一个结果展示字段后再" + actionLabel);
        }
    }

    @Override
    public int offline(Long queryId)
    {
        BizQuery query = requireQuery(queryId);
        checkOwner(query);
        BizQuery upd = new BizQuery();
        upd.setQueryId(queryId);
        upd.setStatus("2");
        upd.setUpdateBy(SecurityUtils.getUsername());
        return queryMapper.updateBizQuery(upd);
    }

    @Override
    public Map<String, Object> openMeta(String code, String accessPwd)
    {
        BizQuery query = requirePublished(code);
        Map<String, Object> data = new HashMap<>();
        data.put("code", query.getPublicCode());
        data.put("queryName", query.getQueryName());
        data.put("queryDesc", query.getQueryDesc());
        boolean needPwd = StringUtils.isNotEmpty(query.getAccessPwd());
        data.put("needPwd", needPwd);
        data.put("needCaptcha", "1".equals(query.getNeedCaptcha()));
        data.put("dailyLimit", query.getDailyLimit() == null ? 0 : query.getDailyLimit());
        if (needPwd && !BizAccessPwdHelper.matches(query.getAccessPwd(), accessPwd))
        {
            data.put("ready", false);
            data.put("unlocked", false);
            return data;
        }
        List<BizQueryField> fields = fieldMapper.selectFieldsByQueryId(query.getQueryId());
        BizQueryPage page = pageMapper.selectByQueryId(query.getQueryId());

        queryMapper.increaseViewCount(query.getQueryId());
        BizAccessLogHelper.log("query", query.getQueryId(), query.getPublicCode(), "view");

        data.put("page", page);
        data.put("queryFields", fields.stream().filter(f -> "1".equals(f.getIsQuery())).collect(Collectors.toList()));
        data.put("listFields", fields.stream().filter(f -> "1".equals(f.getIsList())).collect(Collectors.toList()));
        data.put("ready", true);
        data.put("unlocked", true);
        return data;
    }

    @Override
    public List<BizQueryRow> openSearch(String code, Map<String, Object> params, int pageNum, int pageSize, String accessPwd,
        String captchaCode, String captchaUuid)
    {
        BizQuery query = requirePublished(code);
        assertAccessPwd(query.getAccessPwd(), accessPwd);
        OpenQueryGuard.assertSearchAllowed(query, captchaCode, captchaUuid);
        List<BizQueryField> fields = fieldMapper.selectFieldsByQueryId(query.getQueryId());
        List<Map<String, String>> conditions = buildOpenConditions(query.getQueryId(), params);
        requireOpenConditions(query.getQueryId(), conditions);
        queryMapper.increaseSearchCount(query.getQueryId());
        PageHelper.startPage(pageNum, pageSize);
        List<BizQueryRow> rows = rowMapper.searchRows(query.getQueryId(), conditions, resolveOrderKey(query.getQueryId()));
        long hitTotal = new com.github.pagehelper.PageInfo<>(rows).getTotal();
        BizAccessLogHelper.log("query", query.getQueryId(), query.getPublicCode(), "search",
            BizAccessLogHelper.buildQuerySearchDetail(fields, params, hitTotal, pageNum));
        return maskRows(query.getQueryId(), rows);
    }

    @Override
    public void openExport(String code, Map<String, Object> params, String accessPwd, HttpServletResponse response,
        String captchaCode, String captchaUuid) throws Exception
    {
        BizQuery query = requirePublished(code);
        assertAccessPwd(query.getAccessPwd(), accessPwd);
        OpenQueryGuard.assertAccessAllowed(query, captchaCode, captchaUuid);
        List<BizQueryField> fields = fieldMapper.selectFieldsByQueryId(query.getQueryId()).stream()
            .filter(f -> "1".equals(f.getIsList()))
            .collect(Collectors.toList());
        if (fields.isEmpty())
        {
            fields = fieldMapper.selectFieldsByQueryId(query.getQueryId());
        }
        List<Map<String, String>> conditions = buildOpenConditions(query.getQueryId(), params);
        requireOpenConditions(query.getQueryId(), conditions);
        BizAccessLogHelper.log("query", query.getQueryId(), query.getPublicCode(), "export",
            BizAccessLogHelper.buildQuerySearchDetail(fieldMapper.selectFieldsByQueryId(query.getQueryId()), params, 0, 1));
        PageHelper.startPage(1, 5000, false);
        List<BizQueryRow> rows = maskRows(query.getQueryId(),
            rowMapper.searchRows(query.getQueryId(), conditions, resolveOrderKey(query.getQueryId())));
        writeExcel(query.getQueryName(), query.getSheetName(), fields, rows, response);
    }

    @Override
    public void openExportPdf(String code, Map<String, Object> params, String accessPwd, HttpServletResponse response,
        String captchaCode, String captchaUuid) throws Exception
    {
        BizQuery query = requirePublished(code);
        assertAccessPwd(query.getAccessPwd(), accessPwd);
        OpenQueryGuard.assertAccessAllowed(query, captchaCode, captchaUuid);
        List<BizQueryField> fields = fieldMapper.selectFieldsByQueryId(query.getQueryId()).stream()
            .filter(f -> "1".equals(f.getIsList()))
            .collect(Collectors.toList());
        if (fields.isEmpty())
        {
            fields = fieldMapper.selectFieldsByQueryId(query.getQueryId());
        }
        List<Map<String, String>> conditions = buildOpenConditions(query.getQueryId(), params);
        requireOpenConditions(query.getQueryId(), conditions);
        BizAccessLogHelper.log("query", query.getQueryId(), query.getPublicCode(), "export_pdf",
            BizAccessLogHelper.buildQuerySearchDetail(fieldMapper.selectFieldsByQueryId(query.getQueryId()), params, 0, 1));
        PageHelper.startPage(1, 500, false);
        List<BizQueryRow> rows = maskRows(query.getQueryId(),
            rowMapper.searchRows(query.getQueryId(), conditions, resolveOrderKey(query.getQueryId())));
        BizQueryPdfHelper.writeScorecardPdf(query.getQueryName(), fields, rows, response);
    }

    @Override
    public void exportRows(Long queryId, HttpServletResponse response) throws Exception
    {
        BizQuery query = requireQuery(queryId);
        checkOwner(query);
        List<BizQueryField> fields = fieldMapper.selectFieldsByQueryId(queryId);
        List<BizQueryRow> rows = rowMapper.selectAllRows(queryId);
        writeExcel(query.getQueryName(), query.getSheetName(), fields, rows, response);
    }

    @Override
    public void exportRowsPdf(Long queryId, HttpServletResponse response) throws Exception
    {
        BizQuery query = requireQuery(queryId);
        checkOwner(query);
        List<BizQueryField> fields = fieldMapper.selectFieldsByQueryId(queryId).stream()
            .filter(f -> "1".equals(f.getIsList()))
            .collect(Collectors.toList());
        if (fields.isEmpty())
        {
            fields = fieldMapper.selectFieldsByQueryId(queryId);
        }
        List<BizQueryRow> rows = rowMapper.selectAllRows(queryId);
        if (rows != null && rows.size() > 500)
        {
            rows = rows.subList(0, 500);
        }
        BizQueryPdfHelper.writeScorecardPdf(query.getQueryName(), fields, rows, response);
    }

    private List<Map<String, String>> buildOpenConditions(Long queryId, Map<String, Object> params)
    {
        List<BizQueryField> fields = fieldMapper.selectFieldsByQueryId(queryId);
        Map<String, BizQueryField> fieldMap = fields.stream()
            .filter(f -> "1".equals(f.getIsQuery()))
            .collect(Collectors.toMap(BizQueryField::getFieldKey, f -> f, (a, b) -> a));
        List<Map<String, String>> conditions = new ArrayList<>();
        if (params == null)
        {
            return conditions;
        }
            for (Map.Entry<String, Object> e : params.entrySet())
            {
                String key = e.getKey();
                if (!fieldMap.containsKey(key))
                {
                    continue;
                }
            Object rawVal = e.getValue();
            if (rawVal == null)
                {
                    continue;
                }
                BizQueryField f = fieldMap.get(key);
            String op = StringUtils.isEmpty(f.getQueryType()) ? "EQ" : f.getQueryType().trim().toUpperCase();
            Map<String, String> c = new HashMap<>();
            c.put("key", key);
            c.put("op", op);
            if ("BETWEEN".equals(op))
            {
                String from = null;
                String to = null;
                if (rawVal instanceof List)
                {
                    List<?> list = (List<?>) rawVal;
                    if (list.size() >= 2)
                    {
                        from = list.get(0) == null ? null : String.valueOf(list.get(0)).trim();
                        to = list.get(1) == null ? null : String.valueOf(list.get(1)).trim();
                    }
                }
                else
                {
                    String raw = String.valueOf(rawVal).trim();
                    if (StringUtils.isEmpty(raw))
                {
                    continue;
                }
                    String[] parts = raw.split(",", 2);
                    if (parts.length >= 2)
                    {
                        from = parts[0].trim();
                        to = parts[1].trim();
                    }
                }
                if (StringUtils.isEmpty(from) || StringUtils.isEmpty(to))
                {
                    continue;
                }
                c.put("value", from);
                c.put("value2", to);
            }
            else if ("IN".equals(op))
            {
                List<String> parts = new ArrayList<>();
                if (rawVal instanceof List)
                {
                    for (Object o : (List<?>) rawVal)
                    {
                        if (o == null)
                        {
                            continue;
                        }
                        String s = String.valueOf(o).trim();
                        if (StringUtils.isNotEmpty(s) && !"null".equalsIgnoreCase(s))
                        {
                            parts.add(s);
                        }
                    }
                }
                else
                {
                    for (String part : String.valueOf(rawVal).split("[,，]"))
                    {
                        String s = part.trim();
                        if (StringUtils.isNotEmpty(s) && !"null".equalsIgnoreCase(s))
                        {
                            parts.add(s);
                        }
                    }
                }
                if (parts.isEmpty())
                {
                    continue;
                }
                c.put("value", String.join(",", parts));
            }
            else
            {
                String value = String.valueOf(rawVal).trim();
                if (StringUtils.isEmpty(value) || "null".equalsIgnoreCase(value))
                {
                    continue;
                }
                c.put("value", value);
            }
                conditions.add(c);
            }
        return conditions;
    }

    private void requireOpenConditions(Long queryId, List<Map<String, String>> conditions)
    {
        List<BizQueryField> queryFields = fieldMapper.selectFieldsByQueryId(queryId).stream()
            .filter(f -> "1".equals(f.getIsQuery()))
            .collect(Collectors.toList());
        if (queryFields.isEmpty())
        {
            throw new ServiceException("未配置查询条件字段");
        }
        Set<String> present = new HashSet<>();
        if (conditions != null)
        {
            for (Map<String, String> c : conditions)
            {
                if (c != null && StringUtils.isNotEmpty(c.get("key")))
                {
                    present.add(c.get("key"));
                }
            }
        }
        if (present.isEmpty())
        {
            throw new ServiceException("请至少填写一项查询条件");
        }
        List<String> missing = new ArrayList<>();
        for (BizQueryField f : queryFields)
        {
            boolean required = !"0".equals(f.getIsRequired());
            if (required && !present.contains(f.getFieldKey()))
            {
                missing.add(StringUtils.isNotEmpty(f.getFieldLabel()) ? f.getFieldLabel() : f.getFieldName());
            }
        }
        if (!missing.isEmpty())
        {
            throw new ServiceException("请填写必填查询条件：" + String.join("、", missing));
        }
    }

    private List<BizQueryRow> maskRows(Long queryId, List<BizQueryRow> rows)
    {
        if (rows == null || rows.isEmpty())
        {
            return rows;
        }
        List<BizQueryField> fields = fieldMapper.selectFieldsByQueryId(queryId);
        Map<String, String> maskMap = new HashMap<>();
        for (BizQueryField f : fields)
        {
            if (f != null && StringUtils.isNotEmpty(f.getFieldKey())
                && StringUtils.isNotEmpty(f.getMaskType()) && !"none".equalsIgnoreCase(f.getMaskType()))
            {
                maskMap.put(f.getFieldKey(), f.getMaskType());
            }
        }
        if (maskMap.isEmpty())
        {
            return rows;
        }
        for (BizQueryRow row : rows)
        {
            if (row == null || StringUtils.isEmpty(row.getRowData()))
            {
                continue;
            }
            try
            {
                Map<String, String> map = JSON.parseObject(row.getRowData(), new TypeReference<Map<String, String>>() {});
                if (map == null)
                {
                    continue;
                }
                boolean changed = false;
                for (Map.Entry<String, String> e : maskMap.entrySet())
                {
                    if (map.containsKey(e.getKey()))
                    {
                        map.put(e.getKey(), BizMaskHelper.mask(map.get(e.getKey()), e.getValue()));
                        changed = true;
                    }
                }
                if (changed)
                {
                    row.setRowData(JSON.toJSONString(map));
                }
            }
            catch (Exception ignored)
            {
            }
        }
        return rows;
    }

    private void writeExcel(String queryName, String sheetName, List<BizQueryField> fields, List<BizQueryRow> rows,
        HttpServletResponse response) throws Exception
    {
        try (Workbook wb = new XSSFWorkbook())
        {
            Sheet sheet = wb.createSheet(StringUtils.isEmpty(sheetName) ? "data" : sheetName);
            Row header = sheet.createRow(0);
            for (int i = 0; i < fields.size(); i++)
            {
                header.createCell(i).setCellValue(StringUtils.nvl(fields.get(i).getFieldLabel(), fields.get(i).getFieldName()));
            }
            int r = 1;
            for (BizQueryRow row : rows)
            {
                Map<String, String> map = JSON.parseObject(row.getRowData(), new TypeReference<Map<String, String>>() {});
                Row excelRow = sheet.createRow(r++);
                for (int i = 0; i < fields.size(); i++)
                {
                    String v = map == null ? "" : map.get(fields.get(i).getFieldKey());
                    excelRow.createCell(i).setCellValue(v == null ? "" : v);
                }
            }
            String fileName = URLEncoder.encode(queryName + ".xlsx", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
            wb.write(response.getOutputStream());
        }
    }


    @Override
    public Map<String, Object> previewMeta(Long queryId)
    {
        BizQuery query = requireQuery(queryId);
        checkOwner(query);
        List<BizQueryField> fields = fieldMapper.selectFieldsByQueryId(queryId);
        BizQueryPage page = pageMapper.selectByQueryId(queryId);
        Map<String, Object> data = new HashMap<>();
        data.put("queryId", queryId);
        data.put("queryName", query.getQueryName());
        data.put("queryDesc", query.getQueryDesc());
        data.put("status", query.getStatus());
        data.put("page", page);
        data.put("queryFields", fields.stream().filter(f -> "1".equals(f.getIsQuery())).collect(Collectors.toList()));
        data.put("listFields", fields.stream().filter(f -> "1".equals(f.getIsList())).collect(Collectors.toList()));
        data.put("ready", fields != null && !fields.isEmpty());
        return data;
    }

    @Override
    public List<BizQueryRow> previewSearch(Long queryId, Map<String, Object> params, int pageNum, int pageSize)
    {
        BizQuery query = requireQuery(queryId);
        checkOwner(query);
        List<Map<String, String>> conditions = buildOpenConditions(queryId, params);
        PageHelper.startPage(pageNum, pageSize);
        return rowMapper.searchRows(queryId, conditions, resolveOrderKey(queryId));
    }


    private String resolveOrderKey(Long queryId)
    {
        List<BizQueryField> fields = fieldMapper.selectFieldsByQueryId(queryId);
        if (fields == null)
        {
            return null;
        }
        for (BizQueryField f : fields)
        {
            if ("1".equals(f.getIsSortable()) && StringUtils.isNotEmpty(f.getFieldKey()))
            {
                return f.getFieldKey();
            }
        }
        return null;
    }

    @Override
    public List<BizQueryRow> sampleRows(Long queryId, int limit)
    {
        BizQuery query = requireQuery(queryId);
        checkOwner(query);
        int n = Math.min(Math.max(limit, 1), 50);
        return rowMapper.selectSampleRows(queryId, n);
    }

    @Override
    public List<Map<String, Object>> fieldDist(Long queryId, String fieldKey)
    {
        BizQuery query = requireQuery(queryId);
        checkOwner(query);
        if (StringUtils.isEmpty(fieldKey))
        {
            throw new ServiceException("请选择统计字段");
        }
        // only allow known field keys
        List<BizQueryField> fields = fieldMapper.selectFieldsByQueryId(queryId);
        boolean ok = fields.stream().anyMatch(f -> fieldKey.equals(f.getFieldKey()));
        if (!ok)
        {
            throw new ServiceException("无效字段");
        }
        return rowMapper.selectFieldDist(queryId, fieldKey);
    }

    @Override
    public List<Map<String, Object>> openFieldDist(String code, String fieldKey, Map<String, Object> params, String accessPwd,
        String captchaCode, String captchaUuid)
    {
        BizQuery query = requirePublished(code);
        assertAccessPwd(query.getAccessPwd(), accessPwd);
        OpenQueryGuard.assertAccessAllowed(query, captchaCode, captchaUuid);
        if (StringUtils.isEmpty(fieldKey))
        {
            throw new ServiceException("请选择统计字段");
        }
        List<BizQueryField> fields = fieldMapper.selectFieldsByQueryId(query.getQueryId());
        boolean ok = fields.stream().anyMatch(f -> fieldKey.equals(f.getFieldKey()));
        if (!ok)
        {
            throw new ServiceException("无效字段");
        }
        List<Map<String, String>> conditions = buildOpenConditions(query.getQueryId(), params);
        requireOpenConditions(query.getQueryId(), conditions);
        List<Map<String, Object>> dist = rowMapper.selectFieldDistFiltered(query.getQueryId(), conditions, fieldKey);
        String maskType = fields.stream()
            .filter(f -> fieldKey.equals(f.getFieldKey()))
            .map(BizQueryField::getMaskType)
            .findFirst().orElse("none");
        if (StringUtils.isNotEmpty(maskType) && !"none".equalsIgnoreCase(maskType) && dist != null)
        {
            for (Map<String, Object> item : dist)
            {
                if (item != null && item.get("value") != null)
                {
                    item.put("value", BizMaskHelper.mask(String.valueOf(item.get("value")), maskType));
                }
            }
        }
        return dist;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizQuery copyQuery(Long queryId)
    {
        BizQuery src = requireQuery(queryId);
        checkOwner(src);
        BizQuery neo = new BizQuery();
        String name = src.getQueryName() == null ? "查询" : src.getQueryName();
        if (!name.endsWith("（副本）") && !name.endsWith("(副本)"))
        {
            name = name + "（副本）";
        }
        if (name.length() > 100)
        {
            name = name.substring(0, 100);
        }
        neo.setQueryName(name);
        neo.setQueryDesc(src.getQueryDesc());
        neo.setAccessPwd(src.getAccessPwd());
        neo.setNeedCaptcha(StringUtils.isEmpty(src.getNeedCaptcha()) ? "0" : src.getNeedCaptcha());
        neo.setDailyLimit(src.getDailyLimit() == null ? 0 : src.getDailyLimit());
        neo.setSourceFile(src.getSourceFile());
        neo.setSheetName(src.getSheetName());
        insertBizQuery(neo);

        // Persist security settings (insert defaults may omit them)
        BizQuery sec = new BizQuery();
        sec.setQueryId(neo.getQueryId());
        sec.setNeedCaptcha(neo.getNeedCaptcha());
        sec.setDailyLimit(neo.getDailyLimit());
        // accessPwd already hashed in insertBizQuery — do not overwrite with API mask
        queryMapper.updateBizQuery(sec);

        BizQueryPage srcPage = pageMapper.selectByQueryId(queryId);
        if (srcPage != null)
        {
            BizQueryPage page = pageMapper.selectByQueryId(neo.getQueryId());
            if (page != null)
            {
                page.setTitle(StringUtils.isNotEmpty(srcPage.getTitle()) ? srcPage.getTitle() : neo.getQueryName());
                page.setSubtitle(srcPage.getSubtitle());
                page.setThemeColor(srcPage.getThemeColor());
                page.setBannerUrl(srcPage.getBannerUrl());
                page.setLayoutJson(srcPage.getLayoutJson());
                page.setResultTips(srcPage.getResultTips());
                pageMapper.updatePage(page);
            }
        }

        // Copy multi-datasets + relations (keep join config)
        Map<Long, Long> datasetIdMap = new HashMap<>();
        List<BizQueryDataset> srcDatasets = datasetMapper.selectByQueryId(queryId);
        if (srcDatasets != null)
        {
            for (BizQueryDataset ds : srcDatasets)
            {
                Long oldId = ds.getDatasetId();
                ds.setDatasetId(null);
                ds.setQueryId(neo.getQueryId());
                datasetMapper.insertDataset(ds);
                datasetIdMap.put(oldId, ds.getDatasetId());
                List<BizQueryDatasetRow> dsRows = datasetRowMapper.selectByDatasetId(oldId);
                if (dsRows != null && !dsRows.isEmpty())
                {
                    for (BizQueryDatasetRow r : dsRows)
                    {
                        r.setRowId(null);
                        r.setQueryId(neo.getQueryId());
                        r.setDatasetId(ds.getDatasetId());
                    }
                    int batch = 500;
                    for (int i = 0; i < dsRows.size(); i += batch)
                    {
                        datasetRowMapper.batchInsertRows(dsRows.subList(i, Math.min(i + batch, dsRows.size())));
                    }
                }
            }
        }
        List<BizQueryRelation> srcRels = relationMapper.selectByQueryId(queryId);
        if (srcRels != null)
        {
            for (BizQueryRelation rel : srcRels)
            {
                Long left = datasetIdMap.get(rel.getLeftDatasetId());
                Long right = datasetIdMap.get(rel.getRightDatasetId());
                if (left == null || right == null)
                {
                    continue;
                }
                rel.setRelationId(null);
                rel.setQueryId(neo.getQueryId());
                rel.setLeftDatasetId(left);
                rel.setRightDatasetId(right);
                relationMapper.insertRelation(rel);
            }
        }

        List<BizQueryField> fields = fieldMapper.selectFieldsByQueryId(queryId);
        if (fields != null && !fields.isEmpty())
        {
            for (BizQueryField f : fields)
            {
                f.setFieldId(null);
                f.setQueryId(neo.getQueryId());
            }
            fieldMapper.batchInsertFields(fields);
        }

        List<BizQueryRow> rows = rowMapper.selectAllRows(queryId);
        if (rows != null && !rows.isEmpty())
        {
            List<BizQueryRow> copies = new ArrayList<>();
            for (BizQueryRow r : rows)
            {
                BizQueryRow nr = new BizQueryRow();
                nr.setQueryId(neo.getQueryId());
                nr.setRowNo(r.getRowNo());
                nr.setRowData(r.getRowData());
                copies.add(nr);
            }
            int batch = 500;
            for (int i = 0; i < copies.size(); i += batch)
            {
                rowMapper.batchInsertRows(copies.subList(i, Math.min(i + batch, copies.size())));
            }
            BizQuery upd = new BizQuery();
            upd.setQueryId(neo.getQueryId());
            upd.setRowCount(copies.size());
            upd.setParseStatus("0");
            queryMapper.updateBizQuery(upd);
            neo.setRowCount(copies.size());
        }
        return neo;
    }

    @Override
    public List<Map<String, Object>> listTemplates()
    {
        return BizQueryTemplates.list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizQuery createFromTemplate(String templateKey)
    {
        BizQueryTemplates.TemplateDef def = BizQueryTemplates.require(templateKey);
        BizQuery neo = new BizQuery();
        neo.setQueryName(def.queryName);
        neo.setQueryDesc(def.queryDesc);
        insertBizQuery(neo);

        BizQueryPage page = pageMapper.selectByQueryId(neo.getQueryId());
        if (page != null && def.pageHints != null)
        {
            Object title = def.pageHints.get("title");
            Object subtitle = def.pageHints.get("subtitle");
            Object tips = def.pageHints.get("resultTips");
            Object color = def.pageHints.get("themeColor");
            Object layout = def.pageHints.get("layoutJson");
            if (title != null) { page.setTitle(String.valueOf(title)); }
            if (subtitle != null) { page.setSubtitle(String.valueOf(subtitle)); }
            if (tips != null) { page.setResultTips(String.valueOf(tips)); }
            if (color != null) { page.setThemeColor(String.valueOf(color)); }
            if (layout != null)
            {
                page.setLayoutJson(layout instanceof String ? String.valueOf(layout) : JSON.toJSONString(layout));
            }
            pageMapper.updatePage(page);
        }

        List<BizQueryField> fields = new ArrayList<>();
        for (BizQueryField f : def.fields)
        {
            BizQueryField nf = new BizQueryField();
            nf.setQueryId(neo.getQueryId());
            nf.setFieldKey(f.getFieldKey());
            nf.setFieldName(f.getFieldName());
            nf.setFieldLabel(f.getFieldLabel());
            nf.setDataType(f.getDataType());
            nf.setIsQuery(f.getIsQuery());
            nf.setQueryType(f.getQueryType());
            nf.setHtmlType(f.getHtmlType());
            nf.setIsList(f.getIsList());
            nf.setIsSortable(f.getIsSortable());
            nf.setSort(f.getSort());
            nf.setWidth(f.getWidth());
            nf.setIsRequired(f.getIsRequired());
            nf.setMaskType(f.getMaskType());
            fields.add(nf);
        }
        if (!fields.isEmpty())
        {
            fieldMapper.batchInsertFields(fields);
        }
        if (def.sampleRows != null && !def.sampleRows.isEmpty())
        {
            List<BizQueryRow> rows = new ArrayList<>();
            int no = 1;
            for (Map<String, String> sample : def.sampleRows)
            {
                BizQueryRow qr = new BizQueryRow();
                qr.setQueryId(neo.getQueryId());
                qr.setRowNo(no++);
                qr.setRowData(JSON.toJSONString(sample));
                rows.add(qr);
            }
            rowMapper.batchInsertRows(rows);
            BizQuery upd = new BizQuery();
            upd.setQueryId(neo.getQueryId());
            upd.setRowCount(rows.size());
            upd.setParseStatus("0");
            queryMapper.updateBizQuery(upd);
            neo.setRowCount(rows.size());
            neo.setParseStatus("0");
        }
        return neo;
    }

    @Override
    public List<BizQueryDataset> listDatasets(Long queryId)
    {
        BizQuery query = requireQuery(queryId);
        checkOwner(query);
        return datasetMapper.selectByQueryId(queryId);
    }

    @Override
    @Transactional
    public BizQueryDataset uploadDataset(Long queryId, MultipartFile file, String datasetName, String isPrimary, String mode) throws Exception
    {
        BizQuery query = requireQuery(queryId);
        checkOwner(query);
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("请上传 Excel 文件");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !(filename.endsWith(".xlsx") || filename.endsWith(".xls")))
        {
            throw new ServiceException("仅支持 .xlsx / .xls 文件");
        }
        boolean append = "append".equalsIgnoreCase(mode);
        String stored = FileUploadUtils.upload(RuoYiConfig.getUploadPath(), file);
        String absPath = toLocalPath(stored);

        List<BizQueryDataset> existing = datasetMapper.selectByQueryId(queryId);
        boolean makePrimary = "1".equals(isPrimary) || existing == null || existing.isEmpty();

        DataFormatter formatter = new DataFormatter();
        List<BizQueryJoinHelper.HeaderCol> headers = new ArrayList<>();
        List<BizQueryDatasetRow> rows = new ArrayList<>();
        String sheetName;
        try (InputStream in = new FileInputStream(new File(absPath)); Workbook wb = WorkbookFactory.create(in))
        {
            Sheet sheet = wb.getSheetAt(0);
            sheetName = sheet.getSheetName();
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null)
            {
                throw new ServiceException("Excel 表头为空");
            }
            int colCount = Math.min(header.getLastCellNum(), MAX_COLS);
            if (colCount <= 0)
            {
                throw new ServiceException("未识别到有效表头");
            }
            for (int c = 0; c < colCount; c++)
            {
                String name = formatter.formatCellValue(header.getCell(c)).trim();
                if (StringUtils.isEmpty(name))
                {
                    name = "列" + (c + 1);
                }
                headers.add(new BizQueryJoinHelper.HeaderCol("c" + (c + 1), name));
            }

            BizQueryDataset target = null;
            if (append)
            {
                if (makePrimary)
                {
                    target = findPrimary(existing);
                }
                if (target == null && existing != null && !existing.isEmpty())
                {
                    target = existing.get(existing.size() - 1);
                }
                if (target == null)
                {
                    throw new ServiceException("请先覆盖上传建立数据表后再追加");
                }
                List<BizQueryJoinHelper.HeaderCol> oldH = BizQueryJoinHelper.parseHeaders(target.getHeadersJson());
                if (oldH.size() != headers.size())
                {
                    throw new ServiceException("追加模式要求列数与现有表头一致");
                }
                headers = oldH;
            }

            int startNo = 0;
            Long datasetIdForAppend = null;
            if (append && target != null)
            {
                datasetIdForAppend = target.getDatasetId();
                Integer maxNo = datasetRowMapper.selectMaxRowNo(datasetIdForAppend);
                startNo = maxNo == null ? 0 : maxNo;
            }

            int firstData = sheet.getFirstRowNum() + 1;
            int last = sheet.getLastRowNum();
            int dataRows = 0;
            int existCount = append && target != null && target.getRowCount() != null ? target.getRowCount() : 0;
            for (int r = firstData; r <= last; r++)
            {
                Row row = sheet.getRow(r);
                if (row == null || isEmptyRow(row, colCount, formatter))
                {
                    continue;
                }
                dataRows++;
                if (existCount + dataRows > MAX_ROWS)
                {
                    throw new ServiceException("单个数据表行数不能超过 " + MAX_ROWS);
                }
                Map<String, String> map = new LinkedHashMap<>();
                for (int c = 0; c < colCount; c++)
                {
                    map.put(headers.get(c).key, formatter.formatCellValue(row.getCell(c)).trim());
                }
                BizQueryDatasetRow dr = new BizQueryDatasetRow();
                dr.setQueryId(queryId);
                dr.setRowNo(startNo + dataRows);
                dr.setRowData(JSON.toJSONString(map));
                rows.add(dr);
            }
            if (rows.isEmpty() && !append)
            {
                throw new ServiceException("Excel 无有效数据行");
            }

            if (append && target != null)
            {
                for (BizQueryDatasetRow dr : rows)
                {
                    dr.setDatasetId(target.getDatasetId());
                }
                int batch = 500;
                for (int i = 0; i < rows.size(); i += batch)
                {
                    datasetRowMapper.batchInsertRows(rows.subList(i, Math.min(i + batch, rows.size())));
                }
                BizQueryDataset upd = new BizQueryDataset();
                upd.setDatasetId(target.getDatasetId());
                upd.setSourceFile(stored);
                upd.setSheetName(sheetName);
                upd.setRowCount(existCount + rows.size());
                datasetMapper.updateDataset(upd);
                // 追加后重新物化结果表，避免公开查询仍是旧数据
                materializeJoin(queryId);
                return datasetMapper.selectById(target.getDatasetId());
            }

            // replace: update existing primary in place when uploading as primary
            BizQueryDataset replaceTarget = null;
            if (makePrimary)
            {
                replaceTarget = findPrimary(existing);
            }
            if (replaceTarget != null)
            {
                datasetRowMapper.deleteByDatasetId(replaceTarget.getDatasetId());
                for (BizQueryDatasetRow dr : rows)
                {
                    dr.setDatasetId(replaceTarget.getDatasetId());
                }
                int batch = 500;
                for (int i = 0; i < rows.size(); i += batch)
                {
                    datasetRowMapper.batchInsertRows(rows.subList(i, Math.min(i + batch, rows.size())));
                }
                datasetMapper.clearPrimary(queryId);
                BizQueryDataset upd = new BizQueryDataset();
                upd.setDatasetId(replaceTarget.getDatasetId());
                upd.setDatasetName(StringUtils.isNotEmpty(datasetName) ? datasetName.trim() : replaceTarget.getDatasetName());
                upd.setIsPrimary("1");
                upd.setSourceFile(stored);
                upd.setSheetName(sheetName);
                upd.setRowCount(rows.size());
                upd.setHeadersJson(JSON.toJSONString(headers));
                datasetMapper.updateDataset(upd);

                BizQuery qUpd = new BizQuery();
                qUpd.setQueryId(queryId);
                qUpd.setSourceFile(stored);
                qUpd.setSheetName(sheetName);
                qUpd.setUpdateBy(SecurityUtils.getUsername());
                queryMapper.updateBizQuery(qUpd);
                return datasetMapper.selectById(replaceTarget.getDatasetId());
            }

            String code = nextDatasetCode(existing);
            String name = StringUtils.isNotEmpty(datasetName) ? datasetName.trim()
                : (StringUtils.isNotEmpty(sheetName) ? sheetName : code);
            if (makePrimary)
            {
                datasetMapper.clearPrimary(queryId);
            }
            BizQueryDataset ds = new BizQueryDataset();
            ds.setQueryId(queryId);
            ds.setDatasetCode(code);
            ds.setDatasetName(name);
            ds.setIsPrimary(makePrimary ? "1" : "0");
            ds.setSourceFile(stored);
            ds.setSheetName(sheetName);
            ds.setRowCount(rows.size());
            ds.setHeadersJson(JSON.toJSONString(headers));
            ds.setSort(existing == null ? 0 : existing.size());
            datasetMapper.insertDataset(ds);

            for (BizQueryDatasetRow dr : rows)
            {
                dr.setDatasetId(ds.getDatasetId());
            }
            int batch = 500;
            for (int i = 0; i < rows.size(); i += batch)
            {
                datasetRowMapper.batchInsertRows(rows.subList(i, Math.min(i + batch, rows.size())));
            }

            if (makePrimary)
            {
                BizQuery qUpd = new BizQuery();
                qUpd.setQueryId(queryId);
                qUpd.setSourceFile(stored);
                qUpd.setSheetName(sheetName);
                qUpd.setUpdateBy(SecurityUtils.getUsername());
                queryMapper.updateBizQuery(qUpd);
            }
            return datasetMapper.selectById(ds.getDatasetId());
        }
    }

    @Override
    @Transactional
    public int updateDataset(BizQueryDataset dataset)
    {
        if (dataset == null || dataset.getDatasetId() == null)
        {
            throw new ServiceException("datasetId 不能为空");
        }
        BizQueryDataset db = datasetMapper.selectById(dataset.getDatasetId());
        if (db == null)
        {
            throw new ServiceException("数据表不存在");
        }
        BizQuery query = requireQuery(db.getQueryId());
        checkOwner(query);
        if ("1".equals(dataset.getIsPrimary()))
        {
            datasetMapper.clearPrimary(db.getQueryId());
            dataset.setIsPrimary("1");
        }
        dataset.setDatasetId(db.getDatasetId());
        return datasetMapper.updateDataset(dataset);
    }

    @Override
    @Transactional
    public int deleteDataset(Long queryId, Long datasetId)
    {
        BizQuery query = requireQuery(queryId);
        checkOwner(query);
        BizQueryDataset db = datasetMapper.selectById(datasetId);
        if (db == null || !queryId.equals(db.getQueryId()))
        {
            throw new ServiceException("数据表不存在");
        }
        relationMapper.deleteByDatasetId(datasetId);
        datasetRowMapper.deleteByDatasetId(datasetId);
        datasetMapper.deleteById(datasetId);
        List<BizQueryDataset> left = datasetMapper.selectByQueryId(queryId);
        if (left != null && !left.isEmpty() && left.stream().noneMatch(d -> "1".equals(d.getIsPrimary())))
        {
            BizQueryDataset first = left.get(0);
            BizQueryDataset upd = new BizQueryDataset();
            upd.setDatasetId(first.getDatasetId());
            upd.setIsPrimary("1");
            datasetMapper.updateDataset(upd);
        }
        return 1;
    }

    @Override
    public List<BizQueryRelation> listRelations(Long queryId)
    {
        BizQuery query = requireQuery(queryId);
        checkOwner(query);
        List<BizQueryRelation> list = relationMapper.selectByQueryId(queryId);
        for (BizQueryRelation r : list)
        {
            r.setJoinKeys(BizQueryJoinHelper.parseJoinKeys(r));
        }
        return list;
    }

    @Override
    @Transactional
    public int saveRelations(Long queryId, List<BizQueryRelation> relations)
    {
        BizQuery query = requireQuery(queryId);
        checkOwner(query);
        Map<Long, BizQueryDataset> dsMap = datasetMapper.selectByQueryId(queryId).stream()
            .collect(Collectors.toMap(BizQueryDataset::getDatasetId, d -> d, (a, b) -> a));
        relationMapper.deleteByQueryId(queryId);
        if (relations == null || relations.isEmpty())
        {
            return 0;
        }
        int sort = 0;
        for (BizQueryRelation rel : relations)
        {
            if (rel.getLeftDatasetId() == null || rel.getRightDatasetId() == null)
            {
                throw new ServiceException("请选择左右数据表");
            }
            if (rel.getLeftDatasetId().equals(rel.getRightDatasetId()))
            {
                throw new ServiceException("左右数据表不能相同");
            }
            if (!dsMap.containsKey(rel.getLeftDatasetId()) || !dsMap.containsKey(rel.getRightDatasetId()))
            {
                throw new ServiceException("关联引用了不存在的数据表");
            }
            List<BizQueryRelation.JoinKey> keys = rel.getJoinKeys();
            if ((keys == null || keys.isEmpty()) && StringUtils.isNotEmpty(rel.getJoinKeysJson()))
            {
                keys = BizQueryJoinHelper.parseJoinKeys(rel);
            }
            if (keys == null || keys.isEmpty())
            {
                throw new ServiceException("每条关联至少配置一个关联字段对");
            }
            List<BizQueryRelation.JoinKey> cleaned = new ArrayList<>();
            for (BizQueryRelation.JoinKey jk : keys)
            {
                if (jk == null || StringUtils.isEmpty(jk.getLeftKey()) || StringUtils.isEmpty(jk.getRightKey()))
                {
                    continue;
                }
                cleaned.add(jk);
            }
            if (cleaned.isEmpty())
            {
                throw new ServiceException("每条关联至少配置一个有效的关联字段对");
            }
            String joinType = StringUtils.isEmpty(rel.getJoinType()) ? "LEFT" : rel.getJoinType().trim().toUpperCase();
            if (!"LEFT".equals(joinType) && !"INNER".equals(joinType))
            {
                joinType = "LEFT";
            }
            String multiMatch = StringUtils.isEmpty(rel.getMultiMatch()) ? "EXPAND" : rel.getMultiMatch().trim().toUpperCase();
            if (!"EXPAND".equals(multiMatch) && !"FIRST".equals(multiMatch)
                && !"LAST".equals(multiMatch) && !"CONCAT".equals(multiMatch))
            {
                multiMatch = "EXPAND";
            }
            BizQueryRelation row = new BizQueryRelation();
            row.setQueryId(queryId);
            row.setLeftDatasetId(rel.getLeftDatasetId());
            row.setRightDatasetId(rel.getRightDatasetId());
            row.setJoinType(joinType);
            row.setMultiMatch(multiMatch);
            row.setJoinKeysJson(JSON.toJSONString(cleaned));
            row.setSort(sort++);
            relationMapper.insertRelation(row);
        }
        return sort;
    }

    @Override
    @Transactional
    public BizQueryDetailVo materializeJoin(Long queryId)
    {
        BizQuery query = requireQuery(queryId);
        checkOwner(query);
        List<BizQueryDataset> datasets = datasetMapper.selectByQueryId(queryId);
        if (datasets == null || datasets.isEmpty())
        {
            throw new ServiceException("请先上传数据表");
        }
        List<BizQueryRelation> relations = relationMapper.selectByQueryId(queryId);
        Map<Long, List<Map<String, String>>> rowsByDs = new HashMap<>();
        for (BizQueryDataset ds : datasets)
        {
            List<BizQueryDatasetRow> raw = datasetRowMapper.selectByDatasetId(ds.getDatasetId());
            List<Map<String, String>> maps = new ArrayList<>();
            if (raw != null)
            {
                for (BizQueryDatasetRow r : raw)
                {
                    Map<String, String> m = JSON.parseObject(r.getRowData(), new TypeReference<Map<String, String>>() {});
                    maps.add(m == null ? new LinkedHashMap<>() : m);
                }
            }
            rowsByDs.put(ds.getDatasetId(), maps);
        }
        List<BizQueryField> oldFields = fieldMapper.selectFieldsByQueryId(queryId);
        BizQueryJoinHelper.MaterializeResult mr = BizQueryJoinHelper.materialize(datasets, relations, rowsByDs, oldFields);
        if (mr.rows.size() > MAX_ROWS)
        {
            throw new ServiceException("关联结果超过 " + MAX_ROWS + " 行，请改用 INNER 关联或精简数据");
        }

        // ensure required query field
        boolean hasRequired = mr.fields.stream().anyMatch(f -> "1".equals(f.getIsQuery()) && !"0".equals(f.getIsRequired()));
        if (!hasRequired)
        {
            for (BizQueryField f : mr.fields)
            {
                if ("1".equals(f.getIsQuery()))
                {
                    f.setIsRequired("1");
                    break;
                }
            }
        }

        versionService.snapshotQueryIfNeeded(queryId, "before-materialize");
        fieldMapper.deleteByQueryId(queryId);
        rowMapper.deleteByQueryId(queryId);
        if (!mr.fields.isEmpty())
        {
            for (BizQueryField f : mr.fields)
            {
                f.setQueryId(queryId);
            }
            fieldMapper.batchInsertFields(mr.fields);
        }
        List<BizQueryRow> outRows = new ArrayList<>(mr.rows.size());
        int no = 1;
        for (Map<String, String> m : mr.rows)
        {
            BizQueryRow qr = new BizQueryRow();
            qr.setQueryId(queryId);
            qr.setRowNo(no++);
            qr.setRowData(JSON.toJSONString(m));
            outRows.add(qr);
        }
        int batch = 1000;
        for (int i = 0; i < outRows.size(); i += batch)
        {
            rowMapper.batchInsertRows(outRows.subList(i, Math.min(i + batch, outRows.size())));
        }

        BizQueryDataset primary = datasets.stream().filter(d -> "1".equals(d.getIsPrimary())).findFirst().orElse(datasets.get(0));
        BizQuery upd = new BizQuery();
        upd.setQueryId(queryId);
        upd.setRowCount(outRows.size());
        upd.setParseStatus("0");
        String msg = "关联完成，结果 " + outRows.size() + " 行";
        if (mr.unmatchedLeft > 0)
        {
            msg += "（未匹配主表侧约 " + mr.unmatchedLeft + " 次）";
        }
        upd.setParseMsg(StringUtils.substring(msg, 0, 480));
        upd.setSourceFile(primary.getSourceFile());
        upd.setSheetName(primary.getSheetName());
        upd.setUpdateBy(SecurityUtils.getUsername());
        queryMapper.updateBizQuery(upd);
        BizQueryIndexHelper.refreshEqIndexes(queryId, mr.fields);
        BizQueryDetailVo detail = selectDetail(queryId);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("resultRows", outRows.size());
        report.put("unmatchedLeft", mr.unmatchedLeft);
        report.put("relations", mr.relationReports);
        detail.setJoinReport(report);
        return detail;
    }

    @Override
    public List<com.ruoyi.biz.domain.BizAccessLog> listAccessLogs(Long queryId, String action, int limit)
    {
        BizQuery query = requireQuery(queryId);
        checkOwner(query);
        int lim = limit <= 0 ? 50 : Math.min(limit, 200);
        return accessLogMapper.selectByTarget("query", queryId, StringUtils.isEmpty(action) ? null : action, lim);
    }

    private BizQueryDataset findPrimary(List<BizQueryDataset> list)
    {
        if (list == null)
        {
            return null;
        }
        for (BizQueryDataset d : list)
        {
            if ("1".equals(d.getIsPrimary()))
            {
                return d;
            }
        }
        return null;
    }

    private String nextDatasetCode(List<BizQueryDataset> existing)
    {
        int n = existing == null ? 0 : existing.size();
        Set<String> used = new HashSet<>();
        if (existing != null)
        {
            for (BizQueryDataset d : existing)
            {
                used.add(d.getDatasetCode());
            }
        }
        for (int i = 1; i < 100; i++)
        {
            String code = "ds" + (n + i);
            if (!used.contains(code))
            {
                return code;
            }
        }
        return "ds" + System.currentTimeMillis();
    }

    @Override
    public void checkOwner(BizQuery query)
    {
        Long uid = null;
        try
        {
            uid = SecurityUtils.getUserId();
        }
        catch (Exception ignored)
        {
        }
        if (uid != null && query.getQueryId() != null
            && queryAdminMapper.countByQueryAndUser(query.getQueryId(), uid) > 0)
        {
            return;
        }
        projectScopeHelper.assertAccess(query.getCreateUserId(), query.getDeptId(),
            "biz:query:list,biz:query:query,biz:query:edit", "无权操作该查询项目");
    }

    @Override
    public int transferOwnership(Long queryId, Long targetUserId)
    {
        BizUserProjectServiceImpl.assertAdminManage();
        BizQuery db = requireQuery(queryId);
        checkOwner(db);
        Map<String, Object> user = userProjectService.requireActiveUser(targetUserId);
        BizQuery upd = new BizQuery();
        upd.setQueryId(queryId);
        upd.setCreateUserId(targetUserId);
        Object deptId = user.get("deptId");
        upd.setDeptId(deptId == null ? null : Long.valueOf(String.valueOf(deptId)));
        upd.setCreateBy(String.valueOf(user.get("userName")));
        upd.setUpdateBy(SecurityUtils.getUsername());
        return queryMapper.transferOwner(upd);
    }

    private void assertQueryOwner(BizQuery query)
    {
        if (SecurityUtils.isAdmin())
        {
            return;
        }
        Long uid = SecurityUtils.getUserId();
        if (uid != null && uid.equals(query.getCreateUserId()))
        {
            return;
        }
        throw new ServiceException("仅查询归属人可执行此操作");
    }

    @Override
    public List<com.ruoyi.biz.domain.BizQueryAdmin> listQueryAdmins(Long queryId)
    {
        BizQuery query = requireQuery(queryId);
        checkOwner(query);
        return queryAdminMapper.selectByQueryId(queryId);
    }

    @Override
    public List<Map<String, Object>> searchUsersForAdmin(String keyword)
    {
        String kw = StringUtils.trim(keyword);
        if (StringUtils.isEmpty(kw) || kw.length() < 2)
        {
            throw new ServiceException("请输入至少 2 个字符搜索用户");
        }
        return queryAdminMapper.searchUsers(kw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addQueryAdmin(Long queryId, Long userId, String keyword)
    {
        BizQuery query = requireQuery(queryId);
        assertQueryOwner(query);
        Long targetId = userId;
        if (targetId == null)
        {
            String kw = StringUtils.trim(keyword);
            if (StringUtils.isEmpty(kw))
            {
                throw new ServiceException("请输入用户名或手机号");
            }
            Map<String, Object> found = queryAdminMapper.findUserByNameOrPhone(kw);
            if (found == null || found.isEmpty())
            {
                throw new ServiceException("未找到该用户名或手机号对应的用户");
            }
            targetId = Long.valueOf(String.valueOf(found.get("userId")));
        }
        if (targetId.equals(query.getCreateUserId()))
        {
            throw new ServiceException("归属人无需添加为协作者");
        }
        if (queryAdminMapper.countByQueryAndUser(queryId, targetId) > 0)
        {
            throw new ServiceException("该用户已是协作者");
        }
        com.ruoyi.biz.domain.BizQueryAdmin admin = new com.ruoyi.biz.domain.BizQueryAdmin();
        admin.setQueryId(queryId);
        admin.setUserId(targetId);
        admin.setCreateBy(SecurityUtils.getUsername());
        return queryAdminMapper.insert(admin);
    }

    @Override
    public int removeQueryAdmin(Long queryId, Long userId)
    {
        BizQuery query = requireQuery(queryId);
        assertQueryOwner(query);
        return queryAdminMapper.deleteByQueryAndUser(queryId, userId);
    }

    private void assertAccessPwd(String expected, String actual)
    {
        if (!BizAccessPwdHelper.matches(expected, actual))
        {
            throw new ServiceException("访问密码错误");
        }
    }

    private BizQuery requireQuery(Long queryId)
    {
        BizQuery query = queryMapper.selectBizQueryById(queryId);
        if (query == null)
        {
            throw new ServiceException("查询项目不存在");
        }
        return query;
    }

    private BizQuery requirePublished(String code)
    {
        if (StringUtils.isEmpty(code))
        {
            throw new ServiceException("无效链接");
        }
        BizQuery query = queryMapper.selectBizQueryByCode(code);
        if (query == null)
        {
            throw new ServiceException("查询不存在或未发布");
        }
        if ("3".equals(query.getStatus()))
        {
            throw new ServiceException("查询已结束");
        }
        if (!"1".equals(query.getStatus()))
        {
            throw new ServiceException("查询不存在或未发布");
        }
        Date now = new Date();
        if (query.getStartTime() != null && now.before(query.getStartTime()))
        {
            throw new ServiceException("查询尚未开始");
        }
        if (query.getEndTime() != null && now.after(query.getEndTime()))
        {
            throw new ServiceException("查询已结束");
        }
        return query;
    }

    private BizQueryDetailVo buildDetail(BizQuery query)
    {
        query.setAccessPwd(BizAccessPwdHelper.maskForApi(query.getAccessPwd()));
        BizQueryDetailVo vo = new BizQueryDetailVo();
        vo.setQuery(query);
        vo.setFields(fieldMapper.selectFieldsByQueryId(query.getQueryId()));
        vo.setPage(pageMapper.selectByQueryId(query.getQueryId()));
        List<BizQueryDataset> datasets = datasetMapper.selectByQueryId(query.getQueryId());
        vo.setDatasets(datasets);
        List<BizQueryRelation> relations = relationMapper.selectByQueryId(query.getQueryId());
        for (BizQueryRelation r : relations)
        {
            r.setJoinKeys(BizQueryJoinHelper.parseJoinKeys(r));
        }
        vo.setRelations(relations);
        return vo;
    }

    private String genUniqueCode()
    {
        for (int i = 0; i < 20; i++)
        {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 8; j++)
            {
                sb.append(CODE_CHARS.charAt(ThreadLocalRandom.current().nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (queryMapper.selectBizQueryByCode(code) == null)
            {
                return code;
            }
        }
        throw new ServiceException("生成短码失败，请重试");
    }

    private boolean isEmptyRow(Row row, int colCount, DataFormatter formatter)
    {
        for (int c = 0; c < colCount; c++)
        {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK
                && StringUtils.isNotEmpty(formatter.formatCellValue(cell).trim()))
            {
                return false;
            }
        }
        return true;
    }
}
