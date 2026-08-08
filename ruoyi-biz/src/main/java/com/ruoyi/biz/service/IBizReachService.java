package com.ruoyi.biz.service;

import java.util.Date;
import java.util.Map;

/**
 * P22 reach & schedule: auto publish/expire/remind and publish mail.
 */
public interface IBizReachService
{
    /** Scan due tasks: schedule publish, auto expire, deadline remind. */
    Map<String, Integer> runDueTasks();

    /** Schedule survey publish (keep draft until publishAt). */
    String scheduleSurveyPublish(Long surveyId, Date publishAt);

    /** Cancel survey schedule. */
    int cancelSurveySchedule(Long surveyId);

    /** Schedule query publish. */
    String scheduleQueryPublish(Long queryId, Date publishAt);

    /** Cancel query schedule. */
    int cancelQuerySchedule(Long queryId);

    /** Optional mail after publish. */
    boolean sendPublishNotify(String type, Long projectId, String toEmails, String link);

    /** Answer mail if survey.mailNotify=1. */
    void sendAnswerMailIfNeeded(com.ruoyi.biz.domain.BizSurvey survey, Long answerId, String channelCode);
}
