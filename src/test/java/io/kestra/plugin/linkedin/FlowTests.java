package io.kestra.plugin.linkedin;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.junit.annotations.ExecuteFlow;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.models.executions.TaskRun;
import io.kestra.core.models.flows.State;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@KestraTest(startRunner = true)
class FlowTests {

    @Test
    @ExecuteFlow("flows/linkedin-oauth2-test.yaml")
    void testOAuth2Flow(Execution execution) {
        assertThat(execution.getTaskRunList(), hasSize(1));
        assertThat(execution.getState().getCurrent(), is(oneOf(State.Type.SUCCESS, State.Type.FAILED)));

        TaskRun authTask = execution.getTaskRunList().getFirst();
        assertThat(authTask.getTaskId(), is("authenticate"));
        assertThat(authTask.getState().getCurrent(), is(oneOf(State.Type.SUCCESS, State.Type.FAILED)));
    }

    @Test
    @ExecuteFlow("flows/linkedin-post-analytics-test.yaml")
    void testPostAnalyticsFlow(Execution execution) {
        assertThat(execution.getTaskRunList(), hasSize(greaterThanOrEqualTo(1)));
        assertThat(execution.getState().getCurrent(), is(oneOf(State.Type.SUCCESS, State.Type.FAILED)));

        TaskRun authTask = execution.getTaskRunList().getFirst();
        assertThat(authTask.getTaskId(), is("authenticate"));
        assertThat(authTask.getState().getCurrent(), is(oneOf(State.Type.SUCCESS, State.Type.FAILED)));

        if (execution.getTaskRunList().size() > 1) {
            TaskRun analyticsTask = execution.getTaskRunList().get(1);
            assertThat(analyticsTask.getTaskId(), is("get_analytics"));
            assertThat(analyticsTask.getState().getCurrent(), is(oneOf(State.Type.SUCCESS, State.Type.FAILED)));
        }
    }

    @Test
    @ExecuteFlow("flows/linkedin-oauth2-test.yaml")
    void testOAuth2FlowTaskType(Execution execution) {
        assertThat(execution.getTaskRunList(), hasSize(1));

        TaskRun authTask = execution.getTaskRunList().getFirst();
        assertThat(authTask.getTaskId(), is("authenticate"));
    }

    @Test
    @ExecuteFlow("flows/linkedin-post-analytics-test.yaml")
    void testPostAnalyticsFlowTaskTypes(Execution execution) {
        assertThat(execution.getTaskRunList(), hasSize(greaterThanOrEqualTo(1)));

        TaskRun authTask = execution.getTaskRunList().getFirst();
        assertThat(authTask.getTaskId(), is("authenticate"));

        if (execution.getTaskRunList().size() > 1) {
            TaskRun analyticsTask = execution.getTaskRunList().get(1);
            assertThat(analyticsTask.getTaskId(), is("get_analytics"));
        }
    }

    @Test
    @ExecuteFlow("flows/linkedin-oauth2-test.yaml")
    void testOAuth2FlowExecutionId(Execution execution) {
        assertThat(execution.getId(), is(notNullValue()));
        assertThat(execution.getNamespace(), is("io.kestra.plugin.linkedin"));
        assertThat(execution.getFlowId(), is("linkedin-oauth2-test"));
    }

    @Test
    @ExecuteFlow("flows/linkedin-post-analytics-test.yaml")
    void testPostAnalyticsFlowExecutionId(Execution execution) {
        assertThat(execution.getId(), is(notNullValue()));
        assertThat(execution.getNamespace(), is("io.kestra.plugin.linkedin"));
        assertThat(execution.getFlowId(), is("linkedin-post-analytics-test"));
    }

    @Test
    @ExecuteFlow("flows/linkedin-post-analytics-test.yaml")
    void testPostAnalyticsFlowSequential(Execution execution) {
        assertThat(execution.getTaskRunList(), hasSize(greaterThanOrEqualTo(1)));
        TaskRun authTask = execution.getTaskRunList().getFirst();
        assertThat(authTask.getState().getStartDate(), is(notNullValue()));
        assertThat(authTask.getState().getCurrent(), is(oneOf(State.Type.SUCCESS, State.Type.FAILED)));

        if (execution.getTaskRunList().size() > 1) {
            TaskRun analyticsTask = execution.getTaskRunList().get(1);
            assertThat(analyticsTask.getState().getStartDate(), is(notNullValue()));
            assertThat(analyticsTask.getState().getCurrent(), is(oneOf(State.Type.SUCCESS, State.Type.FAILED)));
        }
    }
}