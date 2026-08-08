package com.ruoyi.biz.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.biz.domain.BizSurveyRevision;

public interface BizSurveyRevisionMapper
{
    Integer selectMaxRevNo(Long surveyId);

    int insert(BizSurveyRevision rev);

    List<BizSurveyRevision> selectBySurveyId(Long surveyId);

    BizSurveyRevision selectById(Long revId);

    List<Long> selectOldRevIds(@Param("surveyId") Long surveyId, @Param("keep") int keep);

    int deleteByIds(@Param("revIds") List<Long> revIds);
}
