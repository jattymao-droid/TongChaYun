package com.ruoyi.biz.service;

import java.util.List;
import com.ruoyi.biz.domain.BizProjectAudit;
import com.ruoyi.biz.domain.BizQueryRevision;
import com.ruoyi.biz.domain.BizSurveyRevision;

public interface IBizVersionService
{
    /** Snapshot current query fields+rows before overwrite. No-op if empty. */
    Long snapshotQueryIfNeeded(Long queryId, String remark);

    List<BizQueryRevision> listQueryRevisions(Long queryId);

    void rollbackQuery(Long queryId, Long revId);

    /** Snapshot current survey design before save. */
    Long snapshotSurveyDesign(Long surveyId, String remark);

    List<BizSurveyRevision> listSurveyRevisions(Long surveyId);

    void rollbackSurvey(Long surveyId, Long revId);

    void audit(String projectType, Long projectId, String action, String detail);

    List<BizProjectAudit> listAudit(String projectType, Long projectId);
}
