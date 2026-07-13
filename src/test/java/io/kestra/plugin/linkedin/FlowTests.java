package io.kestra.plugin.linkedin;

import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.ExecuteFlow;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.executions.Execution;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@KestraTest(startRunner = true)
class FlowTests {

    @Test
    @ExecuteFlow("flows/linkedin-comment-trigger-test.yaml")
    void commentTriggerFlow(Execution execution) {
        assertThat(execution, notNullValue());
        assertThat(execution.getTaskRunList(), not(empty()));
        assertThat(
            execution.getTaskRunList().stream()
                .anyMatch(taskRun -> "log_comment".equals(taskRun.getTaskId())),
            is(true)
        );
    }

    @Test
    @ExecuteFlow("flows/linkedin-oauth2-test.yaml")
    void oauth2Flow(Execution execution) {
        assertThat(execution, notNullValue());
        assertThat(execution.getTaskRunList(), not(empty()));
        assertThat(
            execution.getTaskRunList().stream()
                .anyMatch(taskRun -> "authenticate".equals(taskRun.getTaskId())),
            is(true)
        );
    }

    @Test
    @ExecuteFlow("flows/linkedin-post-analytics-test.yaml")
    void postAnalyticsFlow(Execution execution) {
        assertThat(execution, notNullValue());
        assertThat(execution.getTaskRunList(), not(empty()));
    }

    @Test
    void pluginLoading() {
        assertThat(OAuth2.class, notNullValue());
        assertThat(GetPostAnalytics.class, notNullValue());
        assertThat(CommentTrigger.class, notNullValue());
        assertThat(AbstractLinkedinTask.class, notNullValue());
    }
}
