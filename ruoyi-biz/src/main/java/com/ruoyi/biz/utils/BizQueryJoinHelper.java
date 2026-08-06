package com.ruoyi.biz.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.ruoyi.biz.domain.BizQueryDataset;
import com.ruoyi.biz.domain.BizQueryField;
import com.ruoyi.biz.domain.BizQueryRelation;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;

/**
 * Materialize multi-dataset joins into flat row maps (Plan A).
 * Supports compound join keys: multiple leftKey/rightKey pairs.
 */
public final class BizQueryJoinHelper
{
    private BizQueryJoinHelper() {}

    public static class HeaderCol
    {
        public String key;
        public String name;

        public HeaderCol() {}

        public HeaderCol(String key, String name)
        {
            this.key = key;
            this.name = name;
        }
    }

    public static class RelationReport
    {
        public String leftName;
        public String rightName;
        public String joinType;
        public String multiMatch;
        public int leftRows;
        public int matchedLeft;
        public int unmatchedLeft;
        public int multiHitLeft;
        public int resultRows;
        public java.util.List<String> unmatchedSamples = new ArrayList<>();
    }

    public static class MaterializeResult
    {
        public List<BizQueryField> fields = new ArrayList<>();
        public List<Map<String, String>> rows = new ArrayList<>();
        public int unmatchedLeft;
        public int expandedRows;
        public List<RelationReport> relationReports = new ArrayList<>();
    }

    public static String resultFieldKey(BizQueryDataset ds, String rawKey)
    {
        if (ds == null || "1".equals(ds.getIsPrimary()))
        {
            return rawKey;
        }
        return ds.getDatasetCode() + "_" + rawKey;
    }

    public static List<HeaderCol> parseHeaders(String headersJson)
    {
        if (StringUtils.isEmpty(headersJson))
        {
            return new ArrayList<>();
        }
        List<HeaderCol> list = JSON.parseObject(headersJson, new TypeReference<List<HeaderCol>>() {});
        return list == null ? new ArrayList<>() : list;
    }

    public static List<BizQueryRelation.JoinKey> parseJoinKeys(BizQueryRelation rel)
    {
        if (rel.getJoinKeys() != null && !rel.getJoinKeys().isEmpty())
        {
            return rel.getJoinKeys();
        }
        if (StringUtils.isEmpty(rel.getJoinKeysJson()))
        {
            return new ArrayList<>();
        }
        List<BizQueryRelation.JoinKey> keys = JSON.parseObject(rel.getJoinKeysJson(),
            new TypeReference<List<BizQueryRelation.JoinKey>>() {});
        return keys == null ? new ArrayList<>() : keys;
    }

    public static MaterializeResult materialize(List<BizQueryDataset> datasets,
        List<BizQueryRelation> relations,
        Map<Long, List<Map<String, String>>> rowsByDataset,
        List<BizQueryField> oldFields)
    {
        if (datasets == null || datasets.isEmpty())
        {
            throw new ServiceException("\u8bf7\u5148\u4e0a\u4f20\u81f3\u5c11\u4e00\u4e2a\u6570\u636e\u8868");
        }
        BizQueryDataset primary = null;
        Map<Long, BizQueryDataset> dsMap = new LinkedHashMap<>();
        for (BizQueryDataset d : datasets)
        {
            dsMap.put(d.getDatasetId(), d);
            if ("1".equals(d.getIsPrimary()))
            {
                primary = d;
            }
        }
        if (primary == null)
        {
            primary = datasets.get(0);
            primary.setIsPrimary("1");
        }

        List<Map<String, String>> current = copyRows(rowsByDataset.get(primary.getDatasetId()));
        if (current == null || current.isEmpty())
        {
            throw new ServiceException("\u4e3b\u8868\u65e0\u6570\u636e\uff0c\u65e0\u6cd5\u751f\u6210\u5173\u8054\u7ed3\u679c");
        }

        Map<String, Long> resultKeyOwner = new LinkedHashMap<>();
        List<HeaderCol> primaryHeaders = parseHeaders(primary.getHeadersJson());
        for (HeaderCol h : primaryHeaders)
        {
            resultKeyOwner.put(resultFieldKey(primary, h.key), primary.getDatasetId());
        }
        if (!current.isEmpty())
        {
            for (String k : current.get(0).keySet())
            {
                resultKeyOwner.putIfAbsent(k, primary.getDatasetId());
            }
        }

        Set<Long> included = new LinkedHashSet<>();
        included.add(primary.getDatasetId());

        int unmatched = 0;
        List<BizQueryRelation> rels = relations == null ? new ArrayList<>() : relations;
        List<RelationReport> resultReports = new ArrayList<>();

        for (BizQueryRelation rel : rels)
        {
            BizQueryDataset leftDs = dsMap.get(rel.getLeftDatasetId());
            BizQueryDataset rightDs = dsMap.get(rel.getRightDatasetId());
            if (leftDs == null || rightDs == null)
            {
                throw new ServiceException("\u5173\u8054\u914d\u7f6e\u5f15\u7528\u4e86\u4e0d\u5b58\u5728\u7684\u6570\u636e\u8868");
            }
            if (!included.contains(leftDs.getDatasetId()))
            {
                throw new ServiceException("\u8bf7\u6309\u987a\u5e8f\u914d\u7f6e\u5173\u8054\uff1a\u5de6\u8868\u300c"
                    + leftDs.getDatasetName() + "\u300d\u5c1a\u672a\u52a0\u5165\u7ed3\u679c");
            }
            if (included.contains(rightDs.getDatasetId()))
            {
                throw new ServiceException("\u53f3\u8868\u300c" + rightDs.getDatasetName()
                    + "\u300d\u5df2\u5728\u7ed3\u679c\u4e2d\uff0c\u8bf7\u52ff\u91cd\u590d\u5173\u8054");
            }
            List<BizQueryRelation.JoinKey> keys = parseJoinKeys(rel);
            if (keys.isEmpty())
            {
                throw new ServiceException("\u5173\u8054\u300c" + leftDs.getDatasetName() + " \u2192 " + rightDs.getDatasetName()
                    + "\u300d\u81f3\u5c11\u914d\u7f6e\u4e00\u4e2a\u5173\u8054\u5b57\u6bb5");
            }
            for (BizQueryRelation.JoinKey jk : keys)
            {
                if (StringUtils.isEmpty(jk.getLeftKey()) || StringUtils.isEmpty(jk.getRightKey()))
                {
                    throw new ServiceException("\u5173\u8054\u5b57\u6bb5\u4e0d\u80fd\u4e3a\u7a7a");
                }
            }

            List<Map<String, String>> rightRows = rowsByDataset.get(rightDs.getDatasetId());
            if (rightRows == null)
            {
                rightRows = new ArrayList<>();
            }

            Map<String, List<Map<String, String>>> index = new HashMap<>();
            for (Map<String, String> rr : rightRows)
            {
                String ck = buildRightCompoundKey(rr, keys);
                Map<String, String> prefixed = prefixRow(rr, rightDs);
                index.computeIfAbsent(ck, k -> new ArrayList<>()).add(prefixed);
            }

            boolean inner = "INNER".equalsIgnoreCase(rel.getJoinType());
            String multiMatch = normalizeMultiMatch(rel.getMultiMatch());
            List<Map<String, String>> next = new ArrayList<>();
            int matchedLeft = 0;
            int relUnmatched = 0;
            int multiHitLeft = 0;
            List<String> samples = new ArrayList<>();
            for (Map<String, String> left : current)
            {
                String ck = buildLeftCompoundKey(left, leftDs, keys);
                List<Map<String, String>> matches = index.get(ck);
                if (matches == null || matches.isEmpty())
                {
                    unmatched++;
                    relUnmatched++;
                    if (samples.size() < 12 && StringUtils.isNotEmpty(ck))
                    {
                        samples.add(ck);
                    }
                    if (!inner)
                    {
                        next.add(new LinkedHashMap<>(left));
                    }
                }
                else
                {
                    matchedLeft++;
                    if (matches.size() > 1)
                    {
                        multiHitLeft++;
                    }
                    List<Map<String, String>> chosen = pickMatches(matches, multiMatch);
                    for (Map<String, String> m : chosen)
                    {
                        Map<String, String> merged = new LinkedHashMap<>(left);
                        merged.putAll(m);
                        next.add(merged);
                        if (next.size() > 20000)
                        {
                            throw new ServiceException("\u5173\u8054\u5c55\u5f00\u8d85\u8fc7 20000 \u884c\uff0c\u8bf7\u6539\u7528 FIRST/LAST/CONCAT \u6216 INNER");
                        }
                    }
                }
            }
            RelationReport rr = new RelationReport();
            rr.leftName = leftDs.getDatasetName();
            rr.rightName = rightDs.getDatasetName();
            rr.joinType = StringUtils.isEmpty(rel.getJoinType()) ? "LEFT" : rel.getJoinType();
            rr.multiMatch = multiMatch;
            rr.leftRows = current.size();
            rr.matchedLeft = matchedLeft;
            rr.unmatchedLeft = relUnmatched;
            rr.multiHitLeft = multiHitLeft;
            rr.resultRows = next.size();
            rr.unmatchedSamples = samples;
            resultReports.add(rr);
            current = next;

            List<HeaderCol> rh = parseHeaders(rightDs.getHeadersJson());
            for (HeaderCol h : rh)
            {
                resultKeyOwner.put(resultFieldKey(rightDs, h.key), rightDs.getDatasetId());
            }
            included.add(rightDs.getDatasetId());
        }

        MaterializeResult result = new MaterializeResult();
        result.rows = current;
        result.unmatchedLeft = unmatched;
        result.expandedRows = current.size();
        result.relationReports = resultReports;
        result.fields = buildFields(datasets, included, resultKeyOwner, oldFields, current);
        return result;
    }

    private static String normalizeMultiMatch(String mode)
    {
        if (StringUtils.isEmpty(mode))
        {
            return "EXPAND";
        }
        String m = mode.trim().toUpperCase();
        if ("FIRST".equals(m) || "LAST".equals(m) || "CONCAT".equals(m) || "EXPAND".equals(m))
        {
            return m;
        }
        return "EXPAND";
    }

    private static List<Map<String, String>> pickMatches(List<Map<String, String>> matches, String multiMatch)
    {
        if (matches == null || matches.isEmpty())
        {
            return new ArrayList<>();
        }
        if ("FIRST".equals(multiMatch))
        {
            List<Map<String, String>> one = new ArrayList<>(1);
            one.add(matches.get(0));
            return one;
        }
        if ("LAST".equals(multiMatch))
        {
            List<Map<String, String>> one = new ArrayList<>(1);
            one.add(matches.get(matches.size() - 1));
            return one;
        }
        if ("CONCAT".equals(multiMatch))
        {
            List<Map<String, String>> one = new ArrayList<>(1);
            one.add(concatMatches(matches));
            return one;
        }
        return matches;
    }

    private static Map<String, String> concatMatches(List<Map<String, String>> matches)
    {
        Map<String, String> out = new LinkedHashMap<>();
        Set<String> keys = new LinkedHashSet<>();
        for (Map<String, String> m : matches)
        {
            keys.addAll(m.keySet());
        }
        for (String key : keys)
        {
            LinkedHashSet<String> vals = new LinkedHashSet<>();
            for (Map<String, String> m : matches)
            {
                String v = m.get(key);
                if (StringUtils.isNotEmpty(v))
                {
                    vals.add(v.trim());
                }
            }
            out.put(key, String.join("\uFF1B", vals));
        }
        return out;
    }

    private static List<Map<String, String>> copyRows(List<Map<String, String>> src)
    {
        if (src == null)
        {
            return new ArrayList<>();
        }
        List<Map<String, String>> out = new ArrayList<>(src.size());
        for (Map<String, String> m : src)
        {
            out.add(new LinkedHashMap<>(m));
        }
        return out;
    }

    private static Map<String, String> prefixRow(Map<String, String> raw, BizQueryDataset ds)
    {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : raw.entrySet())
        {
            out.put(resultFieldKey(ds, e.getKey()), e.getValue() == null ? "" : e.getValue());
        }
        return out;
    }

    private static String buildRightCompoundKey(Map<String, String> row, List<BizQueryRelation.JoinKey> keys)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++)
        {
            if (i > 0)
            {
                sb.append('\u0001');
            }
            String v = row.get(keys.get(i).getRightKey());
            sb.append(v == null ? "" : v.trim());
        }
        return sb.toString();
    }

    private static String buildLeftCompoundKey(Map<String, String> leftResultRow, BizQueryDataset leftDs,
        List<BizQueryRelation.JoinKey> keys)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++)
        {
            if (i > 0)
            {
                sb.append('\u0001');
            }
            String resultKey = resultFieldKey(leftDs, keys.get(i).getLeftKey());
            String v = leftResultRow.get(resultKey);
            sb.append(v == null ? "" : v.trim());
        }
        return sb.toString();
    }

    private static List<BizQueryField> buildFields(List<BizQueryDataset> datasets, Set<Long> included,
        Map<String, Long> resultKeyOwner, List<BizQueryField> oldFields, List<Map<String, String>> sampleRows)
    {
        Map<Long, BizQueryDataset> dsMap = new HashMap<>();
        for (BizQueryDataset d : datasets)
        {
            dsMap.put(d.getDatasetId(), d);
        }
        Map<String, BizQueryField> oldByKey = new HashMap<>();
        if (oldFields != null)
        {
            for (BizQueryField f : oldFields)
            {
                if (f != null && StringUtils.isNotEmpty(f.getFieldKey()))
                {
                    oldByKey.put(f.getFieldKey(), f);
                }
            }
        }

        List<BizQueryField> fields = new ArrayList<>();
        int sort = 0;
        for (BizQueryDataset d : datasets)
        {
            if (!included.contains(d.getDatasetId()))
            {
                continue;
            }
            List<HeaderCol> headers = parseHeaders(d.getHeadersJson());
            for (HeaderCol h : headers)
            {
                String key = resultFieldKey(d, h.key);
                fields.add(createField(key, h.name, d, sort++, oldByKey));
            }
        }
        if (sampleRows != null && !sampleRows.isEmpty())
        {
            Set<String> known = new LinkedHashSet<>();
            for (BizQueryField f : fields)
            {
                known.add(f.getFieldKey());
            }
            for (String k : sampleRows.get(0).keySet())
            {
                if (!known.contains(k))
                {
                    Long ownerId = resultKeyOwner.get(k);
                    BizQueryDataset owner = ownerId == null ? null : dsMap.get(ownerId);
                    fields.add(createField(k, k, owner, sort++, oldByKey));
                }
            }
        }
        return fields;
    }

    private static BizQueryField createField(String key, String headerName, BizQueryDataset ds, int sort,
        Map<String, BizQueryField> oldByKey)
    {
        BizQueryField field = new BizQueryField();
        field.setFieldKey(key);
        String label = headerName;
        if (ds != null && !"1".equals(ds.getIsPrimary()) && StringUtils.isNotEmpty(ds.getDatasetName()))
        {
            label = ds.getDatasetName() + "." + headerName;
        }
        field.setFieldName(label);
        field.setFieldLabel(label);
        field.setDataType("string");
        field.setSort(sort + 1);

        BizQueryField old = oldByKey.get(key);
        if (old != null)
        {
            field.setFieldLabel(StringUtils.isNotEmpty(old.getFieldLabel()) ? old.getFieldLabel() : label);
            field.setIsQuery(old.getIsQuery());
            field.setQueryType(old.getQueryType());
            field.setHtmlType(old.getHtmlType());
            field.setIsList(old.getIsList());
            field.setIsSortable(old.getIsSortable());
            field.setIsRequired(old.getIsRequired());
            field.setMaskType(old.getMaskType());
            field.setDictOptions(old.getDictOptions());
            field.setWidth(old.getWidth());
            field.setDataType(StringUtils.isNotEmpty(old.getDataType()) ? old.getDataType() : "string");
        }
        else
        {
            BizQueryFieldInferHelper.apply(field, sort);
        }
        return field;
    }
}
