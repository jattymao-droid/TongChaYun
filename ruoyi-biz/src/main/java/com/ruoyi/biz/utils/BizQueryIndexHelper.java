package com.ruoyi.biz.utils;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import com.ruoyi.biz.domain.BizQueryField;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.spring.SpringUtils;

/**
 * Create partial btree indexes on EQ query keys to speed open search.
 */
public final class BizQueryIndexHelper
{
    private static final Pattern SAFE_KEY = Pattern.compile("^[A-Za-z0-9_]{1,64}$");
    private static final int MAX_INDEXES = 8;

    private BizQueryIndexHelper() {}

    public static void refreshEqIndexes(Long queryId, List<BizQueryField> fields)
    {
        if (queryId == null || fields == null || fields.isEmpty())
        {
            return;
        }
        try
        {
            JdbcTemplate jdbc = SpringUtils.getBean(JdbcTemplate.class);
            int created = 0;
            for (BizQueryField f : fields)
            {
                if (created >= MAX_INDEXES)
                {
                    break;
                }
                if (!"1".equals(f.getIsQuery()))
                {
                    continue;
                }
                String qt = StringUtils.isEmpty(f.getQueryType()) ? "EQ" : f.getQueryType().trim().toUpperCase();
                if (!"EQ".equals(qt) && !"IN".equals(qt))
                {
                    continue;
                }
                String key = f.getFieldKey();
                if (StringUtils.isEmpty(key) || !SAFE_KEY.matcher(key).matches())
                {
                    continue;
                }
                String idx = "idx_biz_qr_" + queryId + "_" + key.toLowerCase();
                if (idx.length() > 60)
                {
                    idx = "idx_biz_qr_" + queryId + "_k" + created;
                }
                jdbc.execute("DROP INDEX IF EXISTS " + idx);
                String sql = "CREATE INDEX IF NOT EXISTS " + idx
                    + " ON biz_query_row ((row_data->>'" + key + "'))"
                    + " WHERE query_id = " + queryId;
                jdbc.execute(sql);
                created++;
            }
        }
        catch (Exception ignored)
        {
            // index refresh must not break materialize/publish
        }
    }
}
