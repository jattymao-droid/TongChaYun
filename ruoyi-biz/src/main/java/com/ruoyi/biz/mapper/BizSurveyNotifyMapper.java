package com.ruoyi.biz.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.biz.domain.BizSurveyNotify;

public interface BizSurveyNotifyMapper
{
    int insertNotify(BizSurveyNotify notify);

    List<BizSurveyNotify> selectNotifyList(BizSurveyNotify query);

    List<BizSurveyNotify> selectNotifyTop(@Param("userId") Long userId, @Param("limit") int limit);

    long countUnread(@Param("userId") Long userId);

    int markRead(@Param("notifyId") Long notifyId, @Param("userId") Long userId);

    int markAllRead(@Param("userId") Long userId);
}
