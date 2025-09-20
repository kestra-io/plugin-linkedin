package io.kestra.plugin.linkedin;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.http.HttpRequestFactory;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
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

    @Schema(
        title = "Access Token",
        description = "The OAuth2 access token for LinkedIn API authentication"
    )
    @NotNull
    protected Property<String> accessToken;

    @Schema(
        title = "Application Name",
        description = "Name of the application making the request"
    )
    @Builder.Default
    protected Property<String> applicationName = Property.ofValue("kestra-linkedin-plugin");

    @Schema(
        title = "LinkedIn API Version",
        description = "LinkedIn API version to use"
    )
    @Builder.Default
    protected Property<String> apiVersion = Property.ofValue("202404");

    protected HttpRequestFactory createLinkedinHttpRequestFactory(RunContext runContext) throws Exception {
        String renderedAccessToken = runContext.render(this.accessToken).as(String.class).orElseThrow();

        Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod());
        credential.setAccessToken(renderedAccessToken);

        return new NetHttpTransport().createRequestFactory(credential);
    }

    protected String getLinkedinApiBaseUrl(RunContext runContext) throws Exception {
        String renderedApiVersion = runContext.render(this.apiVersion).as(String.class).orElse("202404");
        return "https://api.linkedin.com/rest";
    }
}