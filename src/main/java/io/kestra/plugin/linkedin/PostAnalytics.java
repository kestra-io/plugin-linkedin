package io.kestra.plugin.linkedin;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.GenericUrl;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.flows.GenericFlow;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Get LinkedIn Post Analytics (Reactions)",
    description = "Retrieve detailed reactions analytics for one or more LinkedIn posts/activities including reaction types, counts, and actor information"
)
@Plugin(
    examples = {
        @Example(
            title = "Get reactions for multiple posts",
            full = true,
            code = """
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
                """
        )
    }
)
public class PostAnalytics extends AbstractLinkedinTask implements RunnableTask<PostAnalytics.Output> {

    @Schema(
        title = "Activity URNs",
        description = "List of LinkedIn activity URNs to get reactions analytics for"
    )
    @NotNull
    private Property<List<String>> activityUrns;

    @Override
    public Output run(RunContext runContext) throws Exception {
        HttpRequestFactory requestFactory = createLinkedinHttpRequestFactory(runContext);
        List<String> renderedActivityUrns = runContext.render(this.activityUrns).asList(String.class);
        List<PostReactionsData> results = new ArrayList<>();
        Gson gson = new Gson();

        for (String activityUrn : renderedActivityUrns) {
            try {
                String encodedUrn = URLEncoder.encode(activityUrn, StandardCharsets.UTF_8);
               
                String finalUrl = "https://api.linkedin.com/rest/reactions/(entity:" + encodedUrn + ")?q=entity&sort=(value:REVERSE_CHRONOLOGICAL)";
                GenericUrl url = new GenericUrl(finalUrl,true);

                HttpRequest request = requestFactory.buildGetRequest(url);

                request.getHeaders().set("LinkedIn-Version", "202509");
                request.getHeaders().set("X-Restli-Protocol-Version", "2.0.0");

                HttpResponse response = request.execute();
                String responseBody = response.parseAsString();

                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                PostReactionsData postData = parsePostReactions(activityUrn, jsonResponse);
                results.add(postData);

            } catch (Exception e) {
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

        return Output.builder()
            .posts(results)
            .totalPosts(results.size())
            .totalReactions(results.stream().mapToInt(PostReactionsData::getTotalReactions).sum())
            .build();
    }

    private PostReactionsData parsePostReactions(String activityUrn, JsonObject jsonResponse) {
        List<ReactionData> reactions = new ArrayList<>();
        Map<String, Integer> reactionsSummary = new HashMap<>();
        int totalReactions = 0;

        if (jsonResponse.has("elements")) {
            JsonArray elements = jsonResponse.getAsJsonArray("elements");
            for (JsonElement element : elements) {
                JsonObject reactionObj = element.getAsJsonObject();
                ReactionData reaction = parseReactionElement(reactionObj);
                reactions.add(reaction);

                String reactionType = reaction.getReactionType();
                if (reactionType != null) {
                    reactionsSummary.put(reactionType, reactionsSummary.getOrDefault(reactionType, 0) + 1);
                }
            }
        }

        if (jsonResponse.has("paging")) {
            JsonObject paging = jsonResponse.getAsJsonObject("paging");
            totalReactions = paging.has("total") ? paging.get("total").getAsInt() : reactions.size();
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

    private ReactionData parseReactionElement(JsonObject reactionObj) {
        ReactionData.ReactionDataBuilder builder = ReactionData.builder();

        if (reactionObj.has("id")) builder.reactionId(reactionObj.get("id").getAsString());
        if (reactionObj.has("reactionType")) builder.reactionType(reactionObj.get("reactionType").getAsString());
        if (reactionObj.has("root")) builder.rootUrn(reactionObj.get("root").getAsString());

        if (reactionObj.has("created")) {
            JsonObject created = reactionObj.getAsJsonObject("created");
            if (created.has("actor")) builder.actorUrn(created.get("actor").getAsString());
            if (created.has("time")) builder.createdTime(created.get("time").getAsLong());
            if (created.has("impersonator")) builder.impersonatorUrn(created.get("impersonator").getAsString());
        }

        if (reactionObj.has("lastModified")) {
            JsonObject lastModified = reactionObj.getAsJsonObject("lastModified");
            if (lastModified.has("time")) builder.lastModifiedTime(lastModified.get("time").getAsLong());
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
