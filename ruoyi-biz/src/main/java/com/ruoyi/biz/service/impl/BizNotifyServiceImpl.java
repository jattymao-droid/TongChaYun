package com.ruoyi.biz.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.biz.domain.BizSurveyNotify;
import com.ruoyi.biz.mapper.BizSurveyNotifyMapper;
import com.ruoyi.biz.service.IBizNotifyService;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;

@Service
public class BizNotifyServiceImpl implements IBizNotifyService
{
    @Autowired
    private BizSurveyNotifyMapper notifyMapper;

    @Override
    public List<BizSurveyNotify> selectList(BizSurveyNotify query)
    {
        if (!SecurityUtils.isAdmin())
        {
            query.setUserId(SecurityUtils.getUserId());
        }
        else if (query.getUserId() == null)
        {
            query.setUserId(SecurityUtils.getUserId());
        }
        return notifyMapper.selectNotifyList(query);
    }

    @Override
    public List<BizSurveyNotify> selectTop(int limit)
    {
        int lim = limit <= 0 ? 10 : Math.min(limit, 20);
        return notifyMapper.selectNotifyTop(SecurityUtils.getUserId(), lim);
    }

    @Override
    public long countUnread()
    {
        return notifyMapper.countUnread(SecurityUtils.getUserId());
    }

    @Override
    public int markRead(Long notifyId)
    {
        return notifyMapper.markRead(notifyId, SecurityUtils.getUserId());
    }

    @Override
    public int markAllRead()
    {
        return notifyMapper.markAllRead(SecurityUtils.getUserId());
    }

    @Override
    public void createAnswerNotify(Long userId, Long surveyId, Long answerId, String surveyName)
    {
        createAnswerNotify(userId, surveyId, answerId, surveyName, null);
    }

    @Override
    public void createAnswerNotify(Long userId, Long surveyId, Long answerId, String surveyName, String channelCode)
    {
        if (userId == null || surveyId == null)
        {
            return;
        }
        BizSurveyNotify n = new BizSurveyNotify();
        n.setUserId(userId);
        n.setSurveyId(surveyId);
        n.setAnswerId(answerId);
        String name = StringUtils.isEmpty(surveyName) ? "问卷" : surveyName;
        n.setTitle("收到新答卷：" + name);
        StringBuilder content = new StringBuilder("答卷ID ").append(answerId);
        if (StringUtils.isNotEmpty(channelCode))
        {
            content.append("，渠道 ").append(channelCode);
        }
        content.append("，请及时查看。");
        n.setContent(content.toString());
        notifyMapper.insertNotify(n);
    }

    @Override
    public void createSimpleNotify(Long userId, Long surveyId, String title, String content)
    {
        if (userId == null || StringUtils.isEmpty(title))
        {
            return;
        }
        BizSurveyNotify n = new BizSurveyNotify();
        n.setUserId(userId);
        n.setSurveyId(surveyId);
        n.setTitle(StringUtils.substring(title, 0, 200));
        n.setContent(StringUtils.substring(StringUtils.nvl(content, ""), 0, 500));
        notifyMapper.insertNotify(n);
    }
}
