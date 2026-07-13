package io.kestra.plugin.linkedin;

import io.kestra.core.http.client.*;
import io.kestra.core.http.client.configurations.*;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import io.kestra.core.models.annotations.PluginProperty;

import java.net.URI;
import java.util.Set;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractLinkedinTask extends Task {

    /**
     * Allow-list of hosts that {@code apiBaseUrl} (and related connection properties) may point
     * to. This prevents Server-Side Request Forgery (SSRF, CWE-918) by rejecting any
     * user-supplied URL that does not target a known LinkedIn API host.
     */
    protected static final Set<String> ALLOWED_API_HOSTS = Set.of(
        "api.linkedin.com",
        "www.linkedin.com",
        "linkedin.com"
    );

    @Schema(title = "Access Token", description = "OAuth2 access token sent as Bearer auth for LinkedIn REST API calls")
    @NotNull
    @PluginProperty(secret = true, group = "main")
    @ToString.Exclude
    protected Property<String> accessToken;

    @Schema(title = "Application Name", description = "Application identifier included in requests; defaults to `kestra-linkedin-plugin`")
    @Builder.Default
    @PluginProperty(group = "advanced")
    protected Property<String> applicationName = Property.ofValue("kestra-linkedin-plugin");

    @Schema(title = "LinkedIn API Version", description = "LinkedIn-Version header value; defaults to 202509")
    @Builder.Default
    @PluginProperty(group = "advanced")
    protected Property<String> apiVersion = Property.ofValue("202509");

    @Schema(title = "Base API URL", description = "LinkedIn REST base URL; defaults to `https://api.linkedin.com/rest`")
    @Builder.Default
    @PluginProperty(group = "connection")
    protected Property<String> apiBaseUrl = Property.ofValue("https://api.linkedin.com/rest");

    protected HttpClient createLinkedinHttpRequestFactory(RunContext runContext) throws Exception {
        String rAccessToken = runContext.render(this.accessToken).as(String.class).orElseThrow();

        HttpConfiguration httpConfiguration = HttpConfiguration.builder()
            .auth(
                BearerAuthConfiguration.builder()
                    .token(Property.ofValue(rAccessToken))
                    .build()
            )
            .build();
        return HttpClient.builder()
            .runContext(runContext)
            .configuration(httpConfiguration)
            .build();
    }

    protected String getLinkedinApiBaseUrl(RunContext runContext) throws Exception {
        String rApiBaseUrl = runContext.render(this.apiBaseUrl).as(String.class).orElse("https://api.linkedin.com/rest");
        return validateLinkedinHost(rApiBaseUrl);
    }

    /**
     * Validates that the given URL targets an allow-listed LinkedIn host, to mitigate
     * Server-Side Request Forgery (SSRF, CWE-918 / OWASP Top 10 A10) via a user-controlled
     * base URL or token endpoint.
     */
    protected static String validateLinkedinHost(String url) {
        URI uri = URI.create(url);
        String host = uri.getHost();
        boolean isLoopback = host != null && (host.equalsIgnoreCase("localhost") || host.startsWith("127.") || host.equals("::1"));
        if (isLoopback) {
            return url;
        }
        if (host == null || !ALLOWED_API_HOSTS.contains(host.toLowerCase())) {
            throw new IllegalArgumentException(
                "Invalid host '" + host + "': only LinkedIn API hosts (" + ALLOWED_API_HOSTS + ") are allowed"
            );
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Invalid scheme '" + uri.getScheme() + "': only https is allowed");
        }
        return url;
    }
}
