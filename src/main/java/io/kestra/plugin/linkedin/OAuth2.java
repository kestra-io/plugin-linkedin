package io.kestra.plugin.linkedin;

import io.kestra.core.serializers.JacksonMapper;
import com.fasterxml.jackson.databind.JsonNode;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.http.client.configurations.HttpConfiguration;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(title = "Authenticate with LinkedIn using OAuth2",
        description = "This task allows you to authenticate with LinkedIn API using OAuth2 refresh token flow.")
@Plugin(examples = {
          @Example(title = "Authentication with LinkedIn",
                   full = true, 
                   code = """
                        id: linkedin_auth
                        namespace: company.team

                        tasks:
                          - id: authenticate
                            type: io.kestra.plugin.linkedin.OAuth2
                            clientId: "{{ secret('LINKEDIN_CLIENT_ID') }}"
                            clientSecret: "{{ secret('LINKEDIN_CLIENT_SECRET') }}"
                            refreshToken: "{{ secret('LINKEDIN_REFRESH_TOKEN') }}"
                """)
})
public class OAuth2 extends Task implements RunnableTask<OAuth2.Output> {
        @Schema(title = "The OAuth2 Client ID", description = "OAuth2 client ID from LinkedIn Developer Portal")
        @NotNull
        private Property<String> clientId;

        @Schema(title = "The OAuth2 Client Secret", description = "OAuth2 client secret from LinkedIn Developer Portal")
        @NotNull
        private Property<String> clientSecret;

        @Schema(title = "The OAuth2 Refresh Token", description = "Refresh token obtained during the initial authorization flow")
        @NotNull
        private Property<String> refreshToken;

        @Schema(title = "Token endpoint URL", description = "The LinkedIn OAuth2 token endpoint URL")
        @Builder.Default
        private Property<String> tokenUrl = Property.ofValue("https://www.linkedin.com/oauth/v2/accessToken");

        @Override
        public Output run(RunContext runContext) throws Exception {
                String rClientId = runContext.render(this.clientId).as(String.class).orElseThrow();
                String rClientSecret = runContext.render(this.clientSecret).as(String.class).orElseThrow();
                String rRefreshToken = runContext.render(this.refreshToken).as(String.class).orElseThrow();
                String rTokenUrl = runContext.render(this.tokenUrl).as(String.class)
                                .orElse("https://www.linkedin.com/oauth/v2/accessToken");

                try {
                        HttpConfiguration httpConfiguration = HttpConfiguration.builder()
                                        .build();

                        Map<String, Object> formData = new HashMap<>();
                        formData.put("grant_type", "refresh_token");
                        formData.put("refresh_token", rRefreshToken);
                        formData.put("client_id", rClientId);
                        formData.put("client_secret", rClientSecret);

                        HttpRequest request = HttpRequest.builder()
                                        .uri(URI.create(rTokenUrl))
                                        .method("POST")
                                        .body(HttpRequest.UrlEncodedRequestBody.builder()
                                                        .charset(StandardCharsets.UTF_8)
                                                        .content(formData)
                                                        .build())
                                        .addHeader("Accept", "application/json")
                                        .build();

                        try (HttpClient httpClient = HttpClient.builder()
                                        .runContext(runContext)
                                        .configuration(httpConfiguration)
                                        .build()) {

                                HttpResponse<String> response = httpClient.request(request, String.class);
                                String responseBody = response.getBody();

                                if (response.getStatus().getCode() >= 400) {
                                        runContext.logger().error("OAuth2 token refresh failed with status: {} - {}",
                                                        response.getStatus().getCode(), responseBody);
                                        throw new RuntimeException("LinkedIn OAuth2 authentication failed with status: "
                                                        +
                                                        response.getStatus().getCode() + " - " + responseBody);
                                }

                                JsonNode jsonResponse = JacksonMapper.ofJson().readTree(responseBody);

                                String accessToken = jsonResponse.has("access_token")
                                                ? jsonResponse.get("access_token").asText()
                                                : null;
                                String tokenType = jsonResponse.has("token_type")
                                                ? jsonResponse.get("token_type").asText()
                                                : "Bearer";
                                Long expiresIn = jsonResponse.has("expires_in")
                                                ? jsonResponse.get("expires_in").asLong()
                                                : null;
                                String scope = jsonResponse.has("scope") ? jsonResponse.get("scope").asText() : null;

                                if (accessToken == null) {
                                        throw new RuntimeException("No access token received in OAuth2 response");
                                }

                                Instant expiresAt = expiresIn != null ? Instant.now().plusSeconds(expiresIn) : null;

                                runContext.logger().info(
                                                "Successfully refreshed LinkedIn OAuth2 token, expires in {} seconds",
                                                expiresIn);

                                return Output.builder()
                                                .accessToken(accessToken)
                                                .tokenType(tokenType)
                                                .expiresIn(expiresIn)
                                                .scope(scope)
                                                .expiresAt(expiresAt)
                                                .build();
                        }

                } catch (Exception e) {
                        runContext.logger().error("Failed to refresh LinkedIn OAuth2 token", e);
                        throw new RuntimeException("LinkedIn OAuth2 authentication failed: " + e.getMessage(), e);
                }
        }

        @Builder
        @Getter
        public static class Output implements io.kestra.core.models.tasks.Output {
                @Schema(title = "Access Token", description = "OAuth2 access token for LinkedIn API authentication")
                private final String accessToken;

                @Schema(title = "Token Type", description = "Type of the access token (typically 'Bearer')")
                private final String tokenType;

                @Schema(title = "Expires In Seconds", description = "Number of seconds until the token expires")
                private final Long expiresIn;

                @Schema(title = "Token Scope", description = "Granted OAuth2 scopes for LinkedIn API")
                private final String scope;

                @Schema(title = "Expiration time", description = "The exact time when the token expires")
                private final Instant expiresAt;
        }
}