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

    /** 通用站内通知（截止提醒、自动发布等）；surveyId 可空 */
    void createSimpleNotify(Long userId, Long surveyId, String title, String content);
}
