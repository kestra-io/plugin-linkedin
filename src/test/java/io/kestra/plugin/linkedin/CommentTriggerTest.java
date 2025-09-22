package io.kestra.plugin.linkedin;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@KestraTest
class CommentTriggerTest {

    @Test
    void shouldBuildTrigger() {
        CommentTrigger trigger = CommentTrigger.builder()
            .accessToken(Property.ofValue("test-access-token"))
            .postUrns(Property.ofValue(List.of("urn:li:activity:1234567890")))
            .interval(Duration.ofMinutes(15))
            .build();

        assertThat(trigger.getAccessToken(), is(notNullValue()));
        assertThat(trigger.getPostUrns(), is(notNullValue()));
        assertThat(trigger.getInterval(), is(Duration.ofMinutes(15)));
        assertThat(trigger.getLinkedinVersion(), is(notNullValue()));
        assertThat(trigger.getApplicationName(), is(notNullValue()));
    }

    @Test
    void shouldUseDefaultValues() throws Exception {
        CommentTrigger trigger = CommentTrigger.builder()
            .accessToken(Property.ofValue("test-access-token"))
            .postUrns(Property.ofValue(List.of("urn:li:activity:1234567890")))
            .build();

        assertThat(trigger.getInterval(), is(Duration.ofMinutes(30)));

        assertThat(trigger.getLinkedinVersion(), is(notNullValue()));
        assertThat(trigger.getApplicationName(), is(notNullValue()));
    }

    @Test
    void shouldValidateRequiredFields() {
        CommentTrigger trigger = CommentTrigger.builder()
            .accessToken(Property.ofValue("test-access-token"))
            .postUrns(Property.ofValue(List.of("urn:li:activity:1234567890")))
            .build();

        assertThat(trigger.getAccessToken(), is(notNullValue()));
        assertThat(trigger.getPostUrns(), is(notNullValue()));
    }

    @Test
    void shouldHandleMultiplePostUrns() {
        List<String> postUrns = List.of(
            "urn:li:activity:1234567890",
            "urn:li:activity:0987654321",
            "urn:li:activity:1122334455"
        );

        CommentTrigger trigger = CommentTrigger.builder()
            .accessToken(Property.ofValue("test-access-token"))
            .postUrns(Property.ofValue(postUrns))
            .build();

        assertThat(trigger.getPostUrns(), is(notNullValue()));
        assertThat(trigger.getPostUrns(), is(instanceOf(Property.class)));
    }

}