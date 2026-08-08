package com.ruoyi.biz.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import com.ruoyi.biz.domain.BizSurveyQuestion;

class SurveyJumpHelperTest
{
    @Test
    void jumpToEndSkipsTrailingQuestions()
    {
        List<BizSurveyQuestion> qs = new ArrayList<>();
        qs.add(q(1L, "radio", "Q1", 0, "{\"jumps\":[{\"value\":\"A\",\"toSort\":-1}]}"));
        qs.add(q(2L, "input", "Q2", 1, null));
        qs.add(q(3L, "input", "Q3", 2, null));

        Map<Long, String> answers = new HashMap<>();
        answers.put(1L, "A");
        List<BizSurveyQuestion> visible = SurveyJumpHelper.visibleQuestions(qs, answers);
        assertEquals(1, visible.size());
        assertEquals(1L, visible.get(0).getQuestionId());
    }

    @Test
    void jumpToSortSkipsMiddle()
    {
        List<BizSurveyQuestion> qs = new ArrayList<>();
        qs.add(q(1L, "radio", "Q1", 0, "{\"jumps\":[{\"value\":\"B\",\"toSort\":2}]}"));
        qs.add(q(2L, "input", "Q2", 1, null));
        qs.add(q(3L, "input", "Q3", 2, null));

        Map<Long, String> answers = new HashMap<>();
        answers.put(1L, "B");
        List<Long> ids = SurveyJumpHelper.visibleQuestions(qs, answers).stream()
            .map(BizSurveyQuestion::getQuestionId).collect(Collectors.toList());
        assertEquals(List.of(1L, 3L), ids);
    }

    @Test
    void visibleIfHidesUnlessMatched()
    {
        List<BizSurveyQuestion> qs = new ArrayList<>();
        qs.add(q(1L, "yesno", "Show more?", 0, null));
        qs.add(q(2L, "input", "Detail", 1, "{\"visibleIf\":{\"sourceSort\":0,\"value\":\"1\"}}"));

        Map<Long, String> no = new HashMap<>();
        no.put(1L, "0");
        assertEquals(1, SurveyJumpHelper.visibleQuestions(qs, no).size());

        Map<Long, String> yes = new HashMap<>();
        yes.put(1L, "1");
        List<BizSurveyQuestion> visible = SurveyJumpHelper.visibleQuestions(qs, yes);
        assertEquals(2, visible.size());
        assertTrue(visible.stream().anyMatch(x -> x.getQuestionId().equals(2L)));
    }

    @Test
    void sequentialWithoutJumpShowsAll()
    {
        List<BizSurveyQuestion> qs = new ArrayList<>();
        qs.add(q(1L, "input", "A", 0, null));
        qs.add(q(2L, "input", "B", 1, null));
        assertEquals(2, SurveyJumpHelper.visibleQuestions(qs, new HashMap<>()).size());
    }

    @Test
    void jumpOntoPageBreakLandsOnFollowingQuestion()
    {
        List<BizSurveyQuestion> qs = new ArrayList<>();
        qs.add(q(1L, "radio", "Q1", 0, "{\"jumps\":[{\"value\":\"B\",\"toSort\":1}]}"));
        qs.add(q(2L, "page_break", "", 1, null));
        qs.add(q(3L, "input", "Q3", 2, null));

        Map<Long, String> answers = new HashMap<>();
        answers.put(1L, "B");
        List<Long> ids = SurveyJumpHelper.visibleQuestions(qs, answers).stream()
            .map(BizSurveyQuestion::getQuestionId).collect(Collectors.toList());
        assertEquals(List.of(1L, 3L), ids);
    }

    @Test
    void sequentialKeepsPageBreakInPath()
    {
        List<BizSurveyQuestion> qs = new ArrayList<>();
        qs.add(q(1L, "input", "A", 0, null));
        qs.add(q(2L, "page_break", "Next", 1, null));
        qs.add(q(3L, "input", "B", 2, null));
        List<Long> ids = SurveyJumpHelper.visibleQuestions(qs, new HashMap<>()).stream()
            .map(BizSurveyQuestion::getQuestionId).collect(Collectors.toList());
        assertEquals(List.of(1L, 2L, 3L), ids);
        assertTrue(SurveyJumpHelper.isDisplayOnly("page_break"));
    }

    private static BizSurveyQuestion q(Long id, String type, String title, int sort, String props)
    {
        BizSurveyQuestion q = new BizSurveyQuestion();
        q.setQuestionId(id);
        q.setQType(type);
        q.setTitle(title);
        q.setSort(sort);
        q.setRequired("0");
        q.setPropsJson(props);
        return q;
    }
}
