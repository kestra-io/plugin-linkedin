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

    @Schema(title = "Access Token", description = "OAuth2 access token sent as Bearer auth for LinkedIn REST API calls")
    @NotNull
    protected Property<String> accessToken;

    @Schema(title = "Application Name", description = "Application identifier included in requests; defaults to `kestra-linkedin-plugin`")
    @Builder.Default
    protected Property<String> applicationName = Property.ofValue("kestra-linkedin-plugin");

    @Schema(title = "LinkedIn API Version", description = "LinkedIn-Version header value; defaults to 202509")
    @Builder.Default
    protected Property<String> apiVersion = Property.ofValue("202509");

    @Schema(title = "Base API URL", description = "LinkedIn REST base URL; defaults to `https://api.linkedin.com/rest`")
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
