package com.ruoyi.biz.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.biz.domain.BizSurveyQuestion;

public interface BizSurveyQuestionMapper
{
    List<BizSurveyQuestion> selectBySurveyId(Long surveyId);

    int batchInsert(List<BizSurveyQuestion> list);

    int updateQuestion(BizSurveyQuestion question);

    int deleteByIds(@Param("ids") Long[] ids);

    int deleteBySurveyId(Long surveyId);
}
