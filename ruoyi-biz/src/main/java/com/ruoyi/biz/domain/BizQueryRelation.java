package com.ruoyi.biz.domain;

import java.io.Serializable;
import java.util.List;

/** Dataset join config: biz_query_relation */
public class BizQueryRelation implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long relationId;
    private Long queryId;
    private Long leftDatasetId;
    private Long rightDatasetId;
    /** LEFT / INNER */
    private String joinType;
    /** EXPAND / FIRST / LAST / CONCAT ?? how to handle 1:N matches */
    private String multiMatch;
    /** JSON array of join key pairs */
    private String joinKeysJson;
    private Integer sort;

    private List<JoinKey> joinKeys;
    private String leftDatasetName;
    private String rightDatasetName;

    public static class JoinKey implements Serializable
    {
        private static final long serialVersionUID = 1L;
        private String leftKey;
        private String rightKey;

        public String getLeftKey() { return leftKey; }
        public void setLeftKey(String leftKey) { this.leftKey = leftKey; }
        public String getRightKey() { return rightKey; }
        public void setRightKey(String rightKey) { this.rightKey = rightKey; }
    }

    public Long getRelationId() { return relationId; }
    public void setRelationId(Long relationId) { this.relationId = relationId; }
    public Long getQueryId() { return queryId; }
    public void setQueryId(Long queryId) { this.queryId = queryId; }
    public Long getLeftDatasetId() { return leftDatasetId; }
    public void setLeftDatasetId(Long leftDatasetId) { this.leftDatasetId = leftDatasetId; }
    public Long getRightDatasetId() { return rightDatasetId; }
    public void setRightDatasetId(Long rightDatasetId) { this.rightDatasetId = rightDatasetId; }
    public String getJoinType() { return joinType; }
    public void setJoinType(String joinType) { this.joinType = joinType; }
    public String getMultiMatch() { return multiMatch; }
    public void setMultiMatch(String multiMatch) { this.multiMatch = multiMatch; }
    public String getJoinKeysJson() { return joinKeysJson; }
    public void setJoinKeysJson(String joinKeysJson) { this.joinKeysJson = joinKeysJson; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public List<JoinKey> getJoinKeys() { return joinKeys; }
    public void setJoinKeys(List<JoinKey> joinKeys) { this.joinKeys = joinKeys; }
    public String getLeftDatasetName() { return leftDatasetName; }
    public void setLeftDatasetName(String leftDatasetName) { this.leftDatasetName = leftDatasetName; }
    public String getRightDatasetName() { return rightDatasetName; }
    public void setRightDatasetName(String rightDatasetName) { this.rightDatasetName = rightDatasetName; }
}
