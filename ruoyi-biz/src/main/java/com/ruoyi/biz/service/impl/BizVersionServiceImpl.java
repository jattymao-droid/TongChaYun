package com.ruoyi.biz.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.ruoyi.biz.domain.BizProjectAudit;
import com.ruoyi.biz.domain.BizQuery;
import com.ruoyi.biz.domain.BizQueryField;
import com.ruoyi.biz.domain.BizQueryRevision;
import com.ruoyi.biz.domain.BizQueryRow;
import com.ruoyi.biz.domain.BizSurvey;
import com.ruoyi.biz.domain.BizSurveyQuestion;
import com.ruoyi.biz.domain.BizSurveyRevision;
import com.ruoyi.biz.mapper.BizProjectAuditMapper;
import com.ruoyi.biz.mapper.BizQueryAdminMapper;
import com.ruoyi.biz.mapper.BizQueryFieldMapper;
import com.ruoyi.biz.mapper.BizQueryMapper;
import com.ruoyi.biz.mapper.BizQueryRevisionMapper;
import com.ruoyi.biz.mapper.BizQueryRowMapper;
import com.ruoyi.biz.mapper.BizSurveyAdminMapper;
import com.ruoyi.biz.mapper.BizSurveyMapper;
import com.ruoyi.biz.mapper.BizSurveyQuestionMapper;
import com.ruoyi.biz.mapper.BizSurveyRevisionMapper;
import com.ruoyi.biz.service.IBizVersionService;
import com.ruoyi.biz.utils.BizProjectScopeHelper;
import com.ruoyi.biz.utils.BizQueryIndexHelper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;

@Service
public class BizVersionServiceImpl implements IBizVersionService
{
    private static final int KEEP_REVISIONS = 5;

    @Autowired
    private BizQueryMapper queryMapper;
    @Autowired
    private BizQueryFieldMapper fieldMapper;
    @Autowired
    private BizQueryRowMapper rowMapper;
    @Autowired
    private BizQueryRevisionMapper queryRevisionMapper;
    @Autowired
    private BizQueryAdminMapper queryAdminMapper;
    @Autowired
    private BizSurveyMapper surveyMapper;
    @Autowired
    private BizSurveyQuestionMapper questionMapper;
    @Autowired
    private BizSurveyRevisionMapper surveyRevisionMapper;
    @Autowired
    private BizSurveyAdminMapper surveyAdminMapper;
    @Autowired
    private BizProjectAuditMapper auditMapper;
    @Autowired
    private BizProjectScopeHelper projectScopeHelper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long snapshotQueryIfNeeded(Long queryId, String remark)
    {
        if (queryId == null)
        {
            return null;
        }
        List<BizQueryRow> rows = rowMapper.selectAllRows(queryId);
        List<BizQueryField> fields = fieldMapper.selectFieldsByQueryId(queryId);
        if ((rows == null || rows.isEmpty()) && (fields == null || fields.isEmpty()))
        {
            return null;
        }
        Integer max = queryRevisionMapper.selectMaxRevNo(queryId);
        int next = (max == null ? 0 : max) + 1;
        BizQueryRevision rev = new BizQueryRevision();
        rev.setQueryId(queryId);
        rev.setRevNo(next);
        rev.setRowCount(rows == null ? 0 : rows.size());
        rev.setFieldsJson(JSON.toJSONString(fields == null ? new ArrayList<>() : fields));
        rev.setCreateBy(safeUser());
        rev.setRemark(StringUtils.substring(StringUtils.nvl(remark, "overwrite"), 0, 200));
        queryRevisionMapper.insertRevision(rev);
        if (rows != null && !rows.isEmpty())
        {
            int batch = 500;
            for (int i = 0; i < rows.size(); i += batch)
            {
                queryRevisionMapper.batchInsertRows(rev.getRevId(), rows.subList(i, Math.min(i + batch, rows.size())));
            }
        }
        pruneQuery(queryId);
        audit("query", queryId, "snapshot", "rev=" + next + ", rows=" + rev.getRowCount());
        return rev.getRevId();
    }

    @Override
    public List<BizQueryRevision> listQueryRevisions(Long queryId)
    {
        assertQueryAccess(requireQuery(queryId));
        List<BizQueryRevision> list = queryRevisionMapper.selectByQueryId(queryId);
        if (list != null)
        {
            for (BizQueryRevision r : list)
            {
                r.setFieldsJson(null);
            }
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackQuery(Long queryId, Long revId)
    {
        assertQueryAccess(requireQuery(queryId));
        BizQueryRevision rev = queryRevisionMapper.selectById(revId);
        if (rev == null || !queryId.equals(rev.getQueryId()))
        {
            throw new ServiceException("版本不存在");
        }
        snapshotQueryIfNeeded(queryId, "before-rollback");
        List<BizQueryField> fields = JSON.parseObject(rev.getFieldsJson(), new TypeReference<List<BizQueryField>>() {});
        List<BizQueryRow> rows = queryRevisionMapper.selectRowsByRevId(revId);
        fieldMapper.deleteByQueryId(queryId);
        rowMapper.deleteByQueryId(queryId);
        if (fields != null && !fields.isEmpty())
        {
            for (BizQueryField f : fields)
            {
                f.setFieldId(null);
                f.setQueryId(queryId);
            }
            fieldMapper.batchInsertFields(fields);
        }
        if (rows != null && !rows.isEmpty())
        {
            for (BizQueryRow r : rows)
            {
                r.setRowId(null);
                r.setQueryId(queryId);
            }
            int batch = 500;
            for (int i = 0; i < rows.size(); i += batch)
            {
                rowMapper.batchInsertRows(rows.subList(i, Math.min(i + batch, rows.size())));
            }
        }
        BizQuery upd = new BizQuery();
        upd.setQueryId(queryId);
        upd.setRowCount(rows == null ? 0 : rows.size());
        upd.setParseStatus("0");
        upd.setParseMsg(StringUtils.substring("已回滚到版本 #" + rev.getRevNo(), 0, 480));
        upd.setUpdateBy(safeUser());
        queryMapper.updateBizQuery(upd);
        BizQueryIndexHelper.refreshEqIndexes(queryId, fieldMapper.selectFieldsByQueryId(queryId));
        audit("query", queryId, "rollback", "to rev=" + rev.getRevNo() + ", rows=" + upd.getRowCount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long snapshotSurveyDesign(Long surveyId, String remark)
    {
        List<BizSurveyQuestion> questions = questionMapper.selectBySurveyId(surveyId);
        if (questions == null || questions.isEmpty())
        {
            return null;
        }
        BizSurvey survey = surveyMapper.selectBizSurveyById(surveyId);
        Integer max = surveyRevisionMapper.selectMaxRevNo(surveyId);
        int next = (max == null ? 0 : max) + 1;
        BizSurveyRevision rev = new BizSurveyRevision();
        rev.setSurveyId(surveyId);
        rev.setRevNo(next);
        rev.setDesignJson(JSON.toJSONString(questions));
        rev.setThemeJson(survey == null ? null : survey.getThemeJson());
        rev.setCreateBy(safeUser());
        rev.setRemark(StringUtils.substring(StringUtils.nvl(remark, "save"), 0, 200));
        surveyRevisionMapper.insert(rev);
        pruneSurvey(surveyId);
        audit("survey", surveyId, "snapshot", "rev=" + next + ", questions=" + questions.size());
        return rev.getRevId();
    }

    @Override
    public List<BizSurveyRevision> listSurveyRevisions(Long surveyId)
    {
        assertSurveyAccess(requireSurvey(surveyId));
        List<BizSurveyRevision> list = surveyRevisionMapper.selectBySurveyId(surveyId);
        if (list != null)
        {
            for (BizSurveyRevision r : list)
            {
                int qn = 0;
                if (StringUtils.isNotEmpty(r.getDesignJson()))
                {
                    try
                    {
                        List<?> qs = JSON.parseArray(r.getDesignJson());
                        qn = qs == null ? 0 : qs.size();
                    }
                    catch (Exception ignored)
                    {
                    }
                }
                String base = StringUtils.nvl(r.getRemark(), "");
                r.setRemark(StringUtils.substring((base + " 题目" + qn + "道").trim(), 0, 200));
                r.setDesignJson(null);
                r.setThemeJson(null);
            }
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackSurvey(Long surveyId, Long revId)
    {
        assertSurveyAccess(requireSurvey(surveyId));
        BizSurveyRevision rev = surveyRevisionMapper.selectById(revId);
        if (rev == null || !surveyId.equals(rev.getSurveyId()))
        {
            throw new ServiceException("版本不存在");
        }
        snapshotSurveyDesign(surveyId, "before-rollback");
        List<BizSurveyQuestion> questions = JSON.parseObject(rev.getDesignJson(), new TypeReference<List<BizSurveyQuestion>>() {});
        if (questions == null || questions.isEmpty())
        {
            throw new ServiceException("历史版本无题目，无法恢复");
        }
        questionMapper.deleteBySurveyId(surveyId);
        int sort = 0;
        for (BizSurveyQuestion q : questions)
        {
            q.setQuestionId(null);
            q.setSurveyId(surveyId);
            q.setSort(sort++);
        }
        questionMapper.batchInsert(questions);
        if (rev.getThemeJson() != null)
        {
            BizSurvey upd = new BizSurvey();
            upd.setSurveyId(surveyId);
            upd.setThemeJson(rev.getThemeJson());
            upd.setUpdateBy(safeUser());
            surveyMapper.updateBizSurvey(upd);
        }
        audit("survey", surveyId, "rollback", "to rev=" + rev.getRevNo());
    }

    @Override
    public void audit(String projectType, Long projectId, String action, String detail)
    {
        if (projectId == null || StringUtils.isEmpty(action))
        {
            return;
        }
        try
        {
            BizProjectAudit a = new BizProjectAudit();
            a.setProjectType(projectType);
            a.setProjectId(projectId);
            a.setAction(action);
            a.setDetail(StringUtils.substring(StringUtils.nvl(detail, ""), 0, 500));
            a.setOperName(safeUser());
            auditMapper.insert(a);
        }
        catch (Exception ignored)
        {
        }
    }

    @Override
    public List<BizProjectAudit> listAudit(String projectType, Long projectId)
    {
        if ("query".equals(projectType))
        {
            assertQueryAccess(requireQuery(projectId));
        }
        else
        {
            assertSurveyAccess(requireSurvey(projectId));
        }
        BizProjectAudit q = new BizProjectAudit();
        q.setProjectType(projectType);
        q.setProjectId(projectId);
        return auditMapper.selectList(q);
    }

    private void pruneQuery(Long queryId)
    {
        List<Long> old = queryRevisionMapper.selectOldRevIds(queryId, KEEP_REVISIONS);
        if (old != null && !old.isEmpty())
        {
            queryRevisionMapper.deleteRowsByRevIds(old);
            queryRevisionMapper.deleteByIds(old);
        }
    }

    private void pruneSurvey(Long surveyId)
    {
        List<Long> old = surveyRevisionMapper.selectOldRevIds(surveyId, KEEP_REVISIONS);
        if (old != null && !old.isEmpty())
        {
            surveyRevisionMapper.deleteByIds(old);
        }
    }

    private void assertQueryAccess(BizQuery query)
    {
        Long uid = null;
        try { uid = SecurityUtils.getUserId(); } catch (Exception ignored) {}
        if (uid != null && query.getQueryId() != null
            && queryAdminMapper.countByQueryAndUser(query.getQueryId(), uid) > 0)
        {
            return;
        }
        projectScopeHelper.assertAccess(query.getCreateUserId(), query.getDeptId(),
            "biz:query:list,biz:query:query,biz:query:edit", "无权操作该查询项目");
    }

    private void assertSurveyAccess(BizSurvey survey)
    {
        Long uid = null;
        try { uid = SecurityUtils.getUserId(); } catch (Exception ignored) {}
        if (uid != null && survey.getSurveyId() != null
            && surveyAdminMapper.countBySurveyAndUser(survey.getSurveyId(), uid) > 0)
        {
            return;
        }
        projectScopeHelper.assertAccess(survey.getCreateUserId(), survey.getDeptId(),
            "biz:survey:list,biz:survey:query,biz:survey:edit", "无权操作该问卷");
    }

    private BizQuery requireQuery(Long queryId)
    {
        BizQuery q = queryMapper.selectBizQueryById(queryId);
        if (q == null)
        {
            throw new ServiceException("查询项目不存在");
        }
        return q;
    }

    private BizSurvey requireSurvey(Long surveyId)
    {
        BizSurvey s = surveyMapper.selectBizSurveyById(surveyId);
        if (s == null)
        {
            throw new ServiceException("问卷不存在");
        }
        return s;
    }

    private String safeUser()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception e)
        {
            return "system";
        }
    }
}
