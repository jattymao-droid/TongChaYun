package com.ruoyi.biz.mapper;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.biz.domain.BizSurveyDraft;

public interface BizSurveyDraftMapper
{
    BizSurveyDraft selectBySurveyAndToken(@Param("surveyId") Long surveyId, @Param("clientToken") String clientToken);

    int upsertDraft(BizSurveyDraft draft);

    int deleteBySurveyAndToken(@Param("surveyId") Long surveyId, @Param("clientToken") String clientToken);

    int deleteBySurveyId(Long surveyId);
}
