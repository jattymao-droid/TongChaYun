package com.ruoyi.biz.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.biz.domain.BizSurveyAnswerItem;

public interface BizSurveyAnswerItemMapper
{
    List<BizSurveyAnswerItem> selectByAnswerId(Long answerId);

    List<BizSurveyAnswerItem> selectBySurveyId(Long surveyId);

    List<BizSurveyAnswerItem> selectByAnswerIds(@Param("answerIds") List<Long> answerIds);

    int batchInsert(List<BizSurveyAnswerItem> list);

    int deleteByAnswerIds(Long[] answerIds);

    int deleteBySurveyId(Long surveyId);
}
