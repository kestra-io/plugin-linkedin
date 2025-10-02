package io.kestra.plugin.linkedin;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.micronaut.runtime.server.EmbeddedServer;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@KestraTest
class GetPostAnalyticsTest {
    @Inject
    RunContextFactory runContextFactory;

    @Inject
    EmbeddedServer server;

    @BeforeEach
    void ensureServer() {
        if (!server.isRunning())
            server.start();
    }

    @Test
    void shouldFetchAndParseReactions() throws Exception {
        String baseUrl = server.getURI().toString();
        String activityUrn = "urn:li:activity:123456789";

        GetPostAnalytics task = GetPostAnalytics.builder()
                .accessToken(Property.ofValue("test-access-token"))
                .apiBaseUrl(Property.ofValue(baseUrl))
                .activityUrns(Property.ofValue(List.of(activityUrn)))
                .build();

        var runContext = runContextFactory.of(Map.of());
        var out = task.run(runContext);

        assertThat(out.getTotalPosts(), equalTo(1));
        assertThat(out.getTotalReactions(), equalTo(2));
        assertThat(out.getPosts(), hasSize(1));

        var post = out.getPosts().getFirst();
        assertThat(post.getActivityUrn(), equalTo(activityUrn));
        assertThat(post.getTotalReactions(), equalTo(2));
        assertThat(post.getReactions(), hasSize(2));
        assertThat(post.getReactionsSummary(), allOf(
                hasEntry("LIKE", 1),
                hasEntry("CELEBRATE", 1)));

        var r1 = post.getReactions().getFirst();
        assertThat(r1.getReactionId(), equalTo("r1"));
        assertThat(r1.getReactionType(), equalTo("LIKE"));
        assertThat(r1.getActorUrn(), equalTo("urn:li:person:abc"));
        assertThat(r1.getRootUrn(), equalTo(activityUrn));
        assertThat(r1.getCreatedTime(), equalTo(1700000000000L));
        assertThat(r1.getLastModifiedTime(), equalTo(1700000005000L));
    }
}