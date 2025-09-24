package io.kestra.plugin.linkedin;

import io.kestra.core.serializers.JacksonMapper;
import com.fasterxml.jackson.databind.JsonNode;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(title = "Get LinkedIn Post Analytics (Reactions)", description = "Retrieve detailed reactions analytics for one or more LinkedIn posts/activities including reaction types, counts, and actor information")
@Plugin(examples = {
        @Example(title = "Get reactions for multiple posts", full = true, code = """
                id: linkedin_post_analytics
                namespace: company.team
                tasks:
                  - id: authenticate
                    type: io.kestra.plugin.linkedin.OAuth2
                    clientId: "{{ secret('LINKEDIN_CLIENT_ID') }}"
                    clientSecret: "{{ secret('LINKEDIN_CLIENT_SECRET') }}"
                    refreshToken: "{{ secret('LINKEDIN_REFRESH_TOKEN') }}"
                  - id: get_analytics
                    type: io.kestra.plugin.linkedin.PostAnalytics
                    accessToken: "{{ outputs.authenticate.accessToken }}"
                    activityUrns:
                      - "urn:li:activity:7374025671234244609"
                """)
})
public class GetPostAnalytics extends AbstractLinkedinTask implements RunnableTask<GetPostAnalytics.Output> {

    @Schema(title = "Activity URNs", description = "List of LinkedIn activity URNs to get reactions analytics for")
    @NotNull
    private Property<List<String>> activityUrns;

    @Override
    public Output run(RunContext runContext) throws Exception {
        List<String> rActivityUrns = runContext.render(this.activityUrns).asList(String.class);
        List<PostReactionsData> results = new ArrayList<>();

        try (HttpClient httpClient = createLinkedinHttpRequestFactory(runContext)) {
            for (String activityUrn : rActivityUrns) {
                try {
                    String encodedUrn = URLEncoder.encode(activityUrn, StandardCharsets.UTF_8);

                    String finalUrl = getLinkedinApiBaseUrl(runContext) + "/reactions/(entity:" + encodedUrn
                            + ")?q=entity&sort=(value:REVERSE_CHRONOLOGICAL)";

                    HttpRequest request = HttpRequest.builder()
                            .uri(URI.create(finalUrl))
                            .method("GET")
                            .addHeader("LinkedIn-Version", "202509")
                            .addHeader("X-Restli-Protocol-Version", "2.0.0")
                            .build();
                    HttpResponse<String> response = httpClient.request(request, String.class);

                    String responseBody = response.getBody();

                    JsonNode jsonResponse = JacksonMapper.ofJson().readTree(responseBody);
                    PostReactionsData postData = parsePostReactions(activityUrn, jsonResponse);
                    results.add(postData);

                } catch (Exception e) {
                    runContext.logger().error("Failed to retrieve reactions for URN: " + activityUrn, e);
                    results.add(PostReactionsData.builder()
                            .activityUrn(activityUrn)
                            .totalReactions(0)
                            .reactions(new ArrayList<>())
                            .reactionsSummary(new HashMap<>())
                            .error("Failed: " + e.getMessage())
                            .build());
                    throw new RuntimeException("Failed to retrieve reactions for: " + activityUrn, e);
                }
            }
        }
        return Output.builder()
                .posts(results)
                .totalPosts(results.size())
                .totalReactions(results.stream().mapToInt(PostReactionsData::getTotalReactions).sum())
                .build();
    }

    private PostReactionsData parsePostReactions(String activityUrn, JsonNode jsonResponse) {
        List<ReactionData> reactions = new ArrayList<>();
        Map<String, Integer> reactionsSummary = new HashMap<>();
        int totalReactions = 0;

        if (jsonResponse.has("elements")) {
            JsonNode elements = jsonResponse.get("elements");
            for (JsonNode element : elements) {
                ReactionData reaction = parseReactionElement(element);
                reactions.add(reaction);

                String reactionType = reaction.getReactionType();
                if (reactionType != null) {
                    reactionsSummary.put(reactionType, reactionsSummary.getOrDefault(reactionType, 0) + 1);
                }
            }
        }

        if (jsonResponse.has("paging")) {
            JsonNode paging = jsonResponse.get("paging");
            totalReactions = paging.has("total") ? paging.get("total").asInt() : reactions.size();
        } else {
            totalReactions = reactions.size();
        }

        return PostReactionsData.builder()
                .activityUrn(activityUrn)
                .totalReactions(totalReactions)
                .reactions(reactions)
                .reactionsSummary(reactionsSummary)
                .build();
    }

    private ReactionData parseReactionElement(JsonNode reactionObj) {
        ReactionData.ReactionDataBuilder builder = ReactionData.builder();

        if (reactionObj.has("id"))
            builder.reactionId(reactionObj.get("id").asText());
        if (reactionObj.has("reactionType"))
            builder.reactionType(reactionObj.get("reactionType").asText());
        if (reactionObj.has("root"))
            builder.rootUrn(reactionObj.get("root").asText());

        if (reactionObj.has("created")) {
            JsonNode created = reactionObj.get("created");
            if (created.has("actor"))
                builder.actorUrn(created.get("actor").asText());
            if (created.has("time"))
                builder.createdTime(created.get("time").asLong());
            if (created.has("impersonator"))
                builder.impersonatorUrn(created.get("impersonator").asText());
        }

        if (reactionObj.has("lastModified")) {
            JsonNode lastModified = reactionObj.get("lastModified");
            if (lastModified.has("time"))
                builder.lastModifiedTime(lastModified.get("time").asLong());
        }

        return builder.build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        private final List<PostReactionsData> posts;
        private final Integer totalPosts;
        private final Integer totalReactions;
    }

    @Builder
    @Getter
    public static class PostReactionsData {
        private final String activityUrn;
        private final Integer totalReactions;
        private final List<ReactionData> reactions;
        private final Map<String, Integer> reactionsSummary;
        private final String error;
    }

    @Builder
    @Getter
    public static class ReactionData {
        private final String reactionId;
        private final String reactionType;
        private final String actorUrn;
        private final String rootUrn;
        private final Long createdTime;
        private final Long lastModifiedTime;
        private final String impersonatorUrn;
    }
}
