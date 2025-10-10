package io.kestra.plugin.linkedin;

import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.core.http.client.*;
import io.kestra.core.http.client.configurations.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public class AbstractLinkedinTask extends Task {

    @Schema(title = "Access Token", description = "The OAuth2 access token for LinkedIn API authentication")
    @NotNull
    protected Property<String> accessToken;

    @Schema(title = "Application Name", description = "Name of the application making the request")
    @Builder.Default
    protected Property<String> applicationName = Property.ofValue("kestra-linkedin-plugin");

    @Schema(title = "LinkedIn API Version", description = "LinkedIn API version to use")
    @Builder.Default
    protected Property<String> apiVersion = Property.ofValue("202509");

    @Schema(title = "Base API URL", description = "The base API URL of the linkedin")
    @Builder.Default
    protected Property<String> apiBaseUrl = Property.ofValue("https://api.linkedin.com/rest");

    protected HttpClient createLinkedinHttpRequestFactory(RunContext runContext) throws Exception {
        String rAccessToken = runContext.render(this.accessToken).as(String.class).orElseThrow();

        HttpConfiguration httpConfiguration = HttpConfiguration.builder()
                .auth(BearerAuthConfiguration.builder()
                        .token(Property.ofValue(rAccessToken))
                        .build())
                .build();
        return HttpClient.builder()
                .runContext(runContext)
                .configuration(httpConfiguration)
                .build();
    }

    protected String getLinkedinApiBaseUrl(RunContext runContext) throws Exception {
        return runContext.render(this.apiBaseUrl).as(String.class).orElse("https://api.linkedin.com/rest");
    }
}