package com.ruoyi.biz.service;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.biz.domain.BizQuery;
import com.ruoyi.biz.domain.BizQueryDataset;
import com.ruoyi.biz.domain.BizQueryField;
import com.ruoyi.biz.domain.BizQueryPage;
import com.ruoyi.biz.domain.BizQueryRelation;
import com.ruoyi.biz.domain.BizQueryRow;
import com.ruoyi.biz.domain.vo.BizQueryDetailVo;

public interface IBizQueryService
{
    List<BizQuery> selectBizQueryList(BizQuery query);

    BizQueryDetailVo selectDetail(Long queryId);

    BizQuery insertBizQuery(BizQuery query);

    int updateBizQuery(BizQuery query);

    int deleteBizQueryByIds(Long[] queryIds);

    /** mode: replace | append */
    BizQueryDetailVo uploadExcel(Long queryId, MultipartFile file, String mode) throws Exception;

    int saveFields(Long queryId, List<BizQueryField> fields);

    int savePage(BizQueryPage page);

    String publish(Long queryId);

    int offline(Long queryId);

    Map<String, Object> openMeta(String code, String accessPwd);

    List<BizQueryRow> openSearch(String code, Map<String, Object> params, int pageNum, int pageSize, String accessPwd,
        String captchaCode, String captchaUuid);

    void openExport(String code, Map<String, Object> params, String accessPwd, HttpServletResponse response,
        String captchaCode, String captchaUuid) throws Exception;

    void openExportPdf(String code, Map<String, Object> params, String accessPwd, HttpServletResponse response,
        String captchaCode, String captchaUuid) throws Exception;

    void exportRows(Long queryId, HttpServletResponse response) throws Exception;

    void exportRowsPdf(Long queryId, HttpServletResponse response) throws Exception;

    Map<String, Object> previewMeta(Long queryId);

    List<BizQueryRow> previewSearch(Long queryId, Map<String, Object> params, int pageNum, int pageSize);

    void checkOwner(BizQuery query);

    BizQuery copyQuery(Long queryId);

    List<Map<String, Object>> fieldDist(Long queryId, String fieldKey);

    /** First N imported rows for field-config preview. */
    List<BizQueryRow> sampleRows(Long queryId, int limit);

    /** Open field distribution under current search conditions. */
    List<Map<String, Object>> openFieldDist(String code, String fieldKey, Map<String, Object> params, String accessPwd,
        String captchaCode, String captchaUuid);

    List<Map<String, Object>> listTemplates();

    BizQuery createFromTemplate(String templateKey);

    List<BizQueryDataset> listDatasets(Long queryId);

    /** Upload a dataset Excel. isPrimary: 1/0/null(auto). mode: replace|append */
    BizQueryDataset uploadDataset(Long queryId, MultipartFile file, String datasetName, String isPrimary, String mode) throws Exception;

    int updateDataset(BizQueryDataset dataset);

    int deleteDataset(Long queryId, Long datasetId);

    List<BizQueryRelation> listRelations(Long queryId);

    /** Replace all relations; each may contain multiple joinKeys */
    int saveRelations(Long queryId, List<BizQueryRelation> relations);

    /** Materialize join result into biz_query_row / biz_query_field */
    BizQueryDetailVo materializeJoin(Long queryId);

    /** Recent public access logs for audit */
    java.util.List<com.ruoyi.biz.domain.BizAccessLog> listAccessLogs(Long queryId, String action, int limit);

    /** Admin: reassign project owner */
    int transferOwnership(Long queryId, Long targetUserId);

    List<com.ruoyi.biz.domain.BizQueryAdmin> listQueryAdmins(Long queryId);

    List<Map<String, Object>> searchUsersForAdmin(String keyword);

    int addQueryAdmin(Long queryId, Long userId, String keyword);

    int removeQueryAdmin(Long queryId, Long userId);
}

