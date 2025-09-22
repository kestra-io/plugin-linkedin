package io.kestra.plugin.linkedin;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@KestraTest
class GetPostAnalyticsTest {

    @Test
    void shouldBuildTask() throws Exception {
        GetPostAnalytics task = GetPostAnalytics.builder()
            .accessToken(Property.ofValue("test-access-token"))
            .activityUrns(Property.ofValue(List.of("urn:li:activity:1234567890")))
            .build();

        assertThat(task.getAccessToken(), is(notNullValue()));
        assertThat(task.getActivityUrns(), is(notNullValue()));
    }

    @Test
    void shouldTestReactionDataClass() {
        GetPostAnalytics.ReactionData reactionData = GetPostAnalytics.ReactionData.builder()
            .reactionId("reaction123")
            .reactionType("LIKE")
            .actorUrn("urn:li:person:actor123")
            .rootUrn("urn:li:activity:1234567890")
            .createdTime(1640995200000L)
            .lastModifiedTime(1640995300000L)
            .impersonatorUrn("urn:li:organization:impersonator456")
            .build();

        assertThat(reactionData.getReactionId(), is("reaction123"));
        assertThat(reactionData.getReactionType(), is("LIKE"));
        assertThat(reactionData.getActorUrn(), is("urn:li:person:actor123"));
        assertThat(reactionData.getRootUrn(), is("urn:li:activity:1234567890"));
        assertThat(reactionData.getCreatedTime(), is(1640995200000L));
        assertThat(reactionData.getLastModifiedTime(), is(1640995300000L));
        assertThat(reactionData.getImpersonatorUrn(), is("urn:li:organization:impersonator456"));
    }

    @Test
    void shouldTestPostReactionsDataWithError() {
        GetPostAnalytics.PostReactionsData postData = GetPostAnalytics.PostReactionsData.builder()
            .activityUrn("urn:li:activity:1234567890")
            .totalReactions(0)
            .reactions(new ArrayList<>())
            .reactionsSummary(new HashMap<>())
            .error("Failed: API rate limit exceeded")
            .build();

        assertThat(postData.getActivityUrn(), is("urn:li:activity:1234567890"));
        assertThat(postData.getTotalReactions(), is(0));
        assertThat(postData.getReactions(), hasSize(0));
        assertThat(postData.getReactionsSummary(), is(anEmptyMap()));
        assertThat(postData.getError(), is("Failed: API rate limit exceeded"));
    }

    @Test
    void shouldTestOutputClass() {
        List<GetPostAnalytics.PostReactionsData> posts = new ArrayList<>();
        posts.add(GetPostAnalytics.PostReactionsData.builder()
            .activityUrn("urn:li:activity:1234567890")
            .totalReactions(5)
            .reactions(new ArrayList<>())
            .reactionsSummary(Map.of("LIKE", 3, "PRAISE", 2))
            .build());
        posts.add(GetPostAnalytics.PostReactionsData.builder()
            .activityUrn("urn:li:activity:0987654321")
            .totalReactions(3)
            .reactions(new ArrayList<>())
            .reactionsSummary(Map.of("LIKE", 2, "EMPATHY", 1))
            .build());

        GetPostAnalytics.Output output = GetPostAnalytics.Output.builder()
            .posts(posts)
            .totalPosts(2)
            .totalReactions(8)
            .build();

        assertThat(output.getPosts(), hasSize(2));
        assertThat(output.getTotalPosts(), is(2));
        assertThat(output.getTotalReactions(), is(8));
    }


    @Test
    void shouldTestReactionDataWithNullValues() {
        GetPostAnalytics.ReactionData reactionData = GetPostAnalytics.ReactionData.builder()
            .reactionId("reaction123")
            .reactionType(null)
            .actorUrn(null)
            .rootUrn(null)
            .createdTime(null)
            .lastModifiedTime(null)
            .impersonatorUrn(null)
            .build();

        assertThat(reactionData.getReactionId(), is("reaction123"));
        assertThat(reactionData.getReactionType(), is(nullValue()));
        assertThat(reactionData.getActorUrn(), is(nullValue()));
        assertThat(reactionData.getRootUrn(), is(nullValue()));
        assertThat(reactionData.getCreatedTime(), is(nullValue()));
        assertThat(reactionData.getLastModifiedTime(), is(nullValue()));
        assertThat(reactionData.getImpersonatorUrn(), is(nullValue()));
    }

    @Test
    void shouldTestEmptyReactionsScenario() {
        GetPostAnalytics.PostReactionsData postData = GetPostAnalytics.PostReactionsData.builder()
            .activityUrn("urn:li:activity:1234567890")
            .totalReactions(0)
            .reactions(new ArrayList<>())
            .reactionsSummary(new HashMap<>())
            .build();

        assertThat(postData.getActivityUrn(), is("urn:li:activity:1234567890"));
        assertThat(postData.getTotalReactions(), is(0));
        assertThat(postData.getReactions(), is(empty()));
        assertThat(postData.getReactionsSummary(), is(anEmptyMap()));
        assertThat(postData.getError(), is(nullValue()));
    }
}