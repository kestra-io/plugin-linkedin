package io.kestra.plugin.linkedin;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@KestraTest
class CommentTriggerTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void testTaskBuilderDefaults() throws IllegalVariableEvaluationException {
        RunContext runContext = runContextFactory.of(Map.of());
        CommentTrigger task = CommentTrigger.builder()
                .id("test-trigger")
                .type(CommentTrigger.class.getName())
                .accessToken(Property.ofValue("test-access-token"))
                .postUrns(Property.ofValue(List.of("urn:li:activity:123456789")))
                .interval(Duration.parse("PT5M"))
                .build();

        String applicationName = runContext.render(task.getApplicationName()).as(String.class).orElse(null);
        assertThat(applicationName, equalTo("kestra-linkedin-plugin"));
    }

    @Test
    void testCommentTriggerWithMultiplePostUrns() throws IllegalVariableEvaluationException {
        CommentTrigger task = CommentTrigger.builder()
                .id("test-trigger")
                .type(CommentTrigger.class.getName())
                .accessToken(Property.ofValue("test-access-token"))
                .postUrns(Property.ofValue(List.of("urn:li:activity:1", "urn:li:activity:2")))
                .interval(Duration.parse("PT5M"))
                .build();

        var runContext = runContextFactory.of(Map.of());
        var urns = runContext.render(task.getPostUrns()).asList(String.class);
        assertThat(urns, hasSize(2));
        assertThat(urns, contains("urn:li:activity:1", "urn:li:activity:2"));
    }

    @Test
    void testCommentTriggerPropertyRendering() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of());

        CommentTrigger task = CommentTrigger.builder()
                .id("test-trigger")
                .type(CommentTrigger.class.getName())
                .accessToken(Property.ofValue("abc"))
                .postUrns(Property.ofValue(List.of("urn:li:activity:42")))
                .interval(Duration.parse("PT5M"))
                .build();

        String token = runContext.render(task.getAccessToken()).as(String.class).orElse(null);
        List<String> urns = runContext.render(task.getPostUrns()).asList(String.class);

        assertThat(token, equalTo("abc"));
        assertThat(urns, contains("urn:li:activity:42"));
    }

}