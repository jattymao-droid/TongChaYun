package com.ruoyi.biz.domain.vo;

import java.util.List;
import com.ruoyi.biz.domain.BizQuery;
import com.ruoyi.biz.domain.BizQueryDataset;
import com.ruoyi.biz.domain.BizQueryField;
import com.ruoyi.biz.domain.BizQueryPage;
import com.ruoyi.biz.domain.BizQueryRelation;

public class BizQueryDetailVo
{
    private BizQuery query;
    private List<BizQueryField> fields;
    private BizQueryPage page;
    private List<BizQueryDataset> datasets;
    private List<BizQueryRelation> relations;
    private Object joinReport;

    public BizQuery getQuery() { return query; }
    public void setQuery(BizQuery query) { this.query = query; }
    public List<BizQueryField> getFields() { return fields; }
    public void setFields(List<BizQueryField> fields) { this.fields = fields; }
    public BizQueryPage getPage() { return page; }
    public void setPage(BizQueryPage page) { this.page = page; }
    public List<BizQueryDataset> getDatasets() { return datasets; }
    public void setDatasets(List<BizQueryDataset> datasets) { this.datasets = datasets; }
    public List<BizQueryRelation> getRelations() { return relations; }
    public void setRelations(List<BizQueryRelation> relations) { this.relations = relations; }
    public Object getJoinReport() { return joinReport; }
    public void setJoinReport(Object joinReport) { this.joinReport = joinReport; }
}
