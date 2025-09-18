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
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.conditions.ConditionContext;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.triggers.*;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Trigger on new LinkedIn comments",
    description = "Monitors LinkedIn posts for new comments and triggers executions when found."
)
@Plugin(
    examples = {
        @Example(
            title = "Monitor post for new comments",
            full = true,
            code = """
                id: linkedin_comment_monitor
                namespace: company.team
                tasks:
                  - id: authenticate
                    type: io.kestra.plugin.linkedin.OAuth2
                    clientId: "{{ secret('LINKEDIN_CLIENT_ID') }}"
                    clientSecret: "{{ secret('LINKEDIN_CLIENT_SECRET') }}"
                    refreshToken: "{{ secret('LINKEDIN_REFRESH_TOKEN') }}"
                  - id: notify_slack
                    type: io.kestra.plugin.notifications.slack.SlackIncomingWebhook
                    url: "{{ secret('SLACK_WEBHOOK_URL') }}"
                    payload: |
                      {
                        "text": "New LinkedIn comment from {{ trigger.actorUrn }}: {{ trigger.commentText }}"
                      }
                triggers:
                  - id: new_comment_trigger
                    type: io.kestra.plugin.linkedin.CommentTrigger
                    accessToken: "{{ outputs.authenticate.accessToken }}"
                    postUrns:
                      - "urn:li:ugcPost:7374025671234244609"
                    interval: PT30M
                """
        ),
        @Example(
            title = "Monitor multiple posts for comments",
            code = """
                triggers:
                  - id: multi_post_comments
                    type: io.kestra.plugin.linkedin.CommentTrigger
                    accessToken: "{{ secret('LINKEDIN_ACCESS_TOKEN') }}"
                    postUrns:
                      - "urn:li:ugcPost:7374025671234244609"
                      - "urn:li:ugcPost:7374025671234244610"
                    interval: PT15M
                """
        )
    }
)
public class CommentTrigger extends AbstractTrigger implements PollingTriggerInterface, TriggerOutput<CommentTrigger.Output> {

    @Schema(
        title = "Access Token",
        description = "The OAuth2 access token for LinkedIn API authentication"
    )
    @NotNull
    private Property<String> accessToken;

    @Schema(
        title = "Post URNs",
        description = "List of LinkedIn post URNs to monitor for new comments"
    )
    @NotNull
    private Property<List<String>> postUrns;

    @Schema(
        title = "Polling interval",
        description = "How often to check for new comments"
    )
    @PluginProperty
    @Builder.Default
    private Duration interval = Duration.ofMinutes(30);

    @Schema(
        title = "LinkedIn API Version",
        description = "LinkedIn API version header"
    )
    @Builder.Default
    private Property<String> linkedinVersion = Property.ofValue("202502");

    @Schema(
        title = "Application Name",
        description = "Name of the application making the request"
    )
    @Builder.Default
    private Property<String> applicationName = Property.ofValue("kestra-linkedin-plugin");

    @Override
    public Duration getInterval() {
        return this.interval;
    }

    @Override
    public Optional<Execution> evaluate(ConditionContext conditionContext, TriggerContext context) throws Exception {
        RunContext runContext = conditionContext.getRunContext();
        String renderedAccessToken = runContext.render(this.accessToken).as(String.class).orElseThrow();
        List<String> renderedPostUrns = runContext.render(this.postUrns).asList(String.class);
        String renderedLinkedinVersion = runContext.render(this.linkedinVersion).as(String.class).orElse("202502");

        HttpRequestFactory requestFactory = createLinkedinHttpRequestFactory(renderedAccessToken);

        List<String> postsToMonitor = new ArrayList<>();
        if (this.postUrns != null) {
            postsToMonitor.addAll(renderedPostUrns);
        }

        if (postsToMonitor.isEmpty()) {
            runContext.logger().warn("No post URNs provided to monitor");
            return Optional.empty();
        }

        runContext.logger().info("Checking for new comments on {} posts", postsToMonitor.size());

        Instant lastCheckTime = context.getNextExecutionDate() != null
            ? context.getNextExecutionDate().toInstant().minus(this.interval)
            : Instant.now().minus(this.interval);

        List<CommentData> newComments = new ArrayList<>();
        Gson gson = new Gson();

        try {
            for (String postUrn : postsToMonitor) {
                String encodedUrn = URLEncoder.encode(postUrn, StandardCharsets.UTF_8);
                String apiUrl = "https://api.linkedin.com/rest/socialActions/" + encodedUrn + "/comments";

                GenericUrl url = new GenericUrl(apiUrl);
                HttpRequest request = requestFactory.buildGetRequest(url);

                request.getHeaders().set("LinkedIn-Version", renderedLinkedinVersion);
                request.getHeaders().set("X-Restli-Protocol-Version", "2.0.0");

                HttpResponse response = request.execute();
                String responseBody = response.parseAsString();

                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                if (jsonResponse.has("elements")) {
                    JsonArray elements = jsonResponse.getAsJsonArray("elements");

                    for (JsonElement element : elements) {
                        JsonObject commentObj = element.getAsJsonObject();
                        CommentData comment = parseCommentData(postUrn, commentObj, lastCheckTime);

                        if (comment != null) {
                            newComments.add(comment);
                        }
                    }
                }
            }

            if (newComments.isEmpty()) {
                runContext.logger().info("No new comments found since last check");
                return Optional.empty();
            }

            runContext.logger().info("Found {} new comments", newComments.size());

            // Get the most recent comment for the output
            CommentData latest = newComments.stream()
                .max((c1, c2) -> c1.getCreatedTime().compareTo(c2.getCreatedTime()))
                .orElse(newComments.get(0));

            Output output = Output.builder()
                .postUrn(latest.getPostUrn())
                .commentId(latest.getCommentId())
                .commentUrn(latest.getCommentUrn())
                .commentText(latest.getCommentText())
                .actorUrn(latest.getActorUrn())
                .agentUrn(latest.getAgentUrn())
                .createdTime(latest.getCreatedTime())
                .newCommentsCount(newComments.size())
                .allNewComments(newComments)
                .build();

            Execution execution = TriggerService.generateExecution(this, conditionContext, context, output);
            return Optional.of(execution);

        } catch (Exception e) {
            runContext.logger().error("Error checking for new comments", e);
            throw new RuntimeException("Failed to check for new comments: " + e.getMessage(), e);
        }
    }

    private CommentData parseCommentData(String postUrn, JsonObject commentObj, Instant lastCheckTime) {
        if (!commentObj.has("created") || !commentObj.has("message")) {
            return null;
        }

        JsonObject created = commentObj.getAsJsonObject("created");
        if (!created.has("time")) {
            return null;
        }

        long createdTimeMs = created.get("time").getAsLong();
        Instant createdTime = Instant.ofEpochMilli(createdTimeMs);

        // Only include comments created after the last check
        if (!createdTime.isAfter(lastCheckTime)) {
            return null;
        }

        JsonObject message = commentObj.getAsJsonObject("message");
        String commentText = message.has("text") ? message.get("text").getAsString() : "";

        String commentId = commentObj.has("id") ? commentObj.get("id").getAsString() : null;
        String commentUrn = commentObj.has("commentUrn") ? commentObj.get("commentUrn").getAsString() : null;
        String actorUrn = commentObj.has("actor") ? commentObj.get("actor").getAsString() : null;
        String agentUrn = commentObj.has("agent") ? commentObj.get("agent").getAsString() : null;

        return CommentData.builder()
            .postUrn(postUrn)
            .commentId(commentId)
            .commentUrn(commentUrn)
            .commentText(commentText)
            .actorUrn(actorUrn)
            .agentUrn(agentUrn)
            .createdTime(createdTime)
            .build();
    }

    private HttpRequestFactory createLinkedinHttpRequestFactory(String accessToken) throws Exception {
        com.google.api.client.auth.oauth2.Credential credential = 
            new com.google.api.client.auth.oauth2.Credential(
                com.google.api.client.auth.oauth2.BearerToken.authorizationHeaderAccessMethod()
            );
        credential.setAccessToken(accessToken);

        return new com.google.api.client.http.javanet.NetHttpTransport().createRequestFactory(credential);
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Post URN that received the comment")
        private final String postUrn;

        @Schema(title = "Comment ID")
        private final String commentId;

        @Schema(title = "Comment URN")
        private final String commentUrn;

        @Schema(title = "Comment text content")
        private final String commentText;

        @Schema(title = "Actor URN (who made the comment)")
        private final String actorUrn;

        @Schema(title = "Agent URN (impersonator if applicable)")
        private final String agentUrn;

        @Schema(title = "When the comment was created")
        private final Instant createdTime;

        @Schema(title = "Total number of new comments found")
        private final Integer newCommentsCount;

        @Schema(title = "All new comments found")
        private final List<CommentData> allNewComments;
    }

    @Builder
    @Getter
    public static class CommentData {
        @Schema(title = "Post URN")
        private final String postUrn;

        @Schema(title = "Comment ID")
        private final String commentId;

        @Schema(title = "Comment URN")
        private final String commentUrn;

        @Schema(title = "Comment text")
        private final String commentText;

        @Schema(title = "Actor URN")
        private final String actorUrn;

        @Schema(title = "Agent URN")
        private final String agentUrn;

        @Schema(title = "Created time")
        private final Instant createdTime;
    }
}