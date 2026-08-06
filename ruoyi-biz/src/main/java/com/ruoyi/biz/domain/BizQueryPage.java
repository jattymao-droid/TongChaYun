package com.ruoyi.biz.domain;

import java.io.Serializable;

public class BizQueryPage implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long pageId;
    private Long queryId;
    private String title;
    private String subtitle;
    private String themeColor;
    private String bannerUrl;
    private String layoutJson;
    private String resultTips;

    public Long getPageId() { return pageId; }
    public void setPageId(Long pageId) { this.pageId = pageId; }
    public Long getQueryId() { return queryId; }
    public void setQueryId(Long queryId) { this.queryId = queryId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getThemeColor() { return themeColor; }
    public void setThemeColor(String themeColor) { this.themeColor = themeColor; }
    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }
    public String getLayoutJson() { return layoutJson; }
    public void setLayoutJson(String layoutJson) { this.layoutJson = layoutJson; }
    public String getResultTips() { return resultTips; }
    public void setResultTips(String resultTips) { this.resultTips = resultTips; }
}
