package com.ruoyi.biz.service;

import java.util.List;
import com.ruoyi.biz.domain.BizSurveyNotify;

public interface IBizNotifyService
{
    List<BizSurveyNotify> selectList(BizSurveyNotify query);

    List<BizSurveyNotify> selectTop(int limit);

    long countUnread();

    int markRead(Long notifyId);

    int markAllRead();

    void createAnswerNotify(Long userId, Long surveyId, Long answerId, String surveyName);

    void createAnswerNotify(Long userId, Long surveyId, Long answerId, String surveyName, String channelCode);
}
