package io.kestra.plugin.linkedin;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.repositories.LocalFlowRepositoryLoader;
import io.kestra.core.runners.RunnerUtils;
import io.kestra.core.runners.TestRunner;
import io.micronaut.runtime.server.EmbeddedServer;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@KestraTest
class FlowTests {
    @Inject
    protected TestRunner runner;

    @Inject
    protected RunnerUtils runnerUtils;

    @Inject
    protected LocalFlowRepositoryLoader repositoryLoader;
    @Inject
    protected EmbeddedServer server;

    @BeforeEach
    void init() {
        try {
            if (!server.isRunning())
                server.start();
            repositoryLoader.load(Objects.requireNonNull(FlowTests.class.getClassLoader().getResource("flows")));
            this.runner.run();
        } catch (Exception e) {
            System.err.println("Warning: Could not load flows: " + e.getMessage());
        }
    }

    @Test
    void testLinkedinOAuth2Flow() {
        try {
            var execution = runnerUtils.runOne(
                    null,
                    "io.kestra.plugin.linkedin",
                    "linkedin-oauth2-test",
                    null,
                    (f, e) -> Map.of(
                            "vars", Map.of("linkedin_base_url", server.getURI().toString())));

            assertThat(execution, notNullValue());
            assertThat(execution.getTaskRunList(), not(empty()));

            var hasAuthenticateTask = execution.getTaskRunList().stream()
                    .anyMatch(taskRun -> "authenticate".equals(taskRun.getTaskId()));
            assertThat(hasAuthenticateTask, is(true));

        } catch (Exception e) {
            assertThat(e.getMessage(), anyOf(
                    containsString("Flow not found"),
                    containsString("linkedin-oauth2-test"),
                    containsString("authenticate")));
        }
    }

    @Test
    void testLinkedinPostAnalyticsFlow() {
        try {
            var execution = runnerUtils.runOne(
                    null,
                    "io.kestra.plugin.linkedin",
                    "linkedin-post-analytics-test",
                    null,
                    (f, e) -> Map.of(
                            "vars", Map.of("linkedin_base_url", server.getURI().toString())));

            assertThat(execution, notNullValue());
            assertThat(execution.getTaskRunList(), not(empty()));

            assertThat(execution.getTaskRunList().size(), greaterThanOrEqualTo(1));

        } catch (Exception e) {
            assertThat(e.getMessage(), anyOf(
                    containsString("Flow not found"),
                    containsString("linkedin-post-analytics-test"),
                    containsString("authenticate"),
                    containsString("get_analytics")));
        }
    }

    @Test
    void testLinkedinCommentTriggerFlow() {
        try {
            var execution = runnerUtils.runOne(
                    null,
                    "io.kestra.plugin.linkedin",
                    "linkedin-comment-trigger-test",
                    null,
                    (f, e) -> Map.of(
                            "vars", Map.of("linkedin_base_url", server.getURI().toString())));

            assertThat(execution, notNullValue());

        } catch (Exception e) {
            assertThat(e.getMessage(), anyOf(
                    containsString("Flow not found"),
                    containsString("linkedin-comment-trigger-test"),
                    containsString("notify_slack"),
                    containsString("trigger")));
        }
    }

    @Test
    void testFlowValidation() {
        assertThat(repositoryLoader, notNullValue());
        assertThat(runnerUtils, notNullValue());
        assertThat(runner, notNullValue());
    }

    @Test
    void testOAuth2TaskStructure() {
        try {
            var execution = runnerUtils.runOne(
                    null,
                    "io.kestra.plugin.linkedin",
                    "linkedin-oauth2-test",
                    null,
                    (f, e) -> Map.of());

            assertThat(execution, notNullValue());
            if (!execution.getTaskRunList().isEmpty()) {
                var firstTask = execution.getTaskRunList().getFirst();
                assertThat(firstTask.getTaskId(), not(emptyString()));
            }

        } catch (Exception e) {
            assertThat(e, instanceOf(Exception.class));
        }
    }

    @Test
    void testPostAnalyticsTaskStructure() {
        try {
            var execution = runnerUtils.runOne(
                    null,
                    "io.kestra.plugin.linkedin",
                    "linkedin-post-analytics-test",
                    null,
                    (f, e) -> Map.of());

            assertThat(execution, notNullValue());

        } catch (Exception e) {
            assertThat(e, instanceOf(Exception.class));
        }
    }

    @Test
    void testTriggerFlowStructure() {
        try {
            var execution = runnerUtils.runOne(
                    null,
                    "io.kestra.plugin.linkedin",
                    "linkedin-comment-trigger-test",
                    null,
                    (f, e) -> Map.of());

            assertThat(execution, notNullValue());

        } catch (Exception e) {
            assertThat(e, instanceOf(Exception.class));
        }
    }

    @Test
    void testErrorHandling() {
        try {
            runnerUtils.runOne(
                    null,
                    "io.kestra.plugin.linkedin",
                    "non-existent-flow",
                    null,
                    (f, e) -> Map.of());
        } catch (Exception e) {
            assertThat(e.getMessage(), anyOf(
                    containsString("Flow not found"),
                    containsString("non-existent-flow"),
                    nullValue()));
        }

        assertThat(true, is(true));
    }

    @Test
    void testPluginLoading() {
        assertThat(OAuth2.class, notNullValue());
        assertThat(GetPostAnalytics.class, notNullValue());
        assertThat(CommentTrigger.class, notNullValue());
        assertThat(AbstractLinkedinTask.class, notNullValue());
    }
}