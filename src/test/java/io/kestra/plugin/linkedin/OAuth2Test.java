package io.kestra.plugin.linkedin;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.micronaut.runtime.server.EmbeddedServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KestraTest
class OAuth2Test {
    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    private EmbeddedServer server;

    @BeforeEach
    void ensureServer() {
        if (!server.isRunning())
            server.start();
    }

    @Test
    void testOAuth2FlowSuccess() throws Exception {
        String tokenEndpoint = server.getURI().toString() + "/oauth/v2/accessToken";

        RunContext runContext = runContextFactory.of(Map.of());

        OAuth2 task = OAuth2.builder()
                .clientId(Property.ofValue("test-client-id"))
                .clientSecret(Property.ofValue("test-client-secret"))
                .refreshToken(Property.ofValue("test-refresh-token"))
                .tokenUrl(Property.ofValue(tokenEndpoint))
                .build();

        OAuth2.Output out = task.run(runContext);
        assertThat(out, notNullValue());
        assertThat(out.getAccessToken(), equalTo("mock-token"));
        assertThat(out.getTokenType(), anyOf(nullValue(), equalToIgnoringCase("Bearer")));
        assertThat(out.getExpiresIn(), anyOf(nullValue(), greaterThan(0L)));
    }

    @Test
    void testOAuth2FlowMissingClientId() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
                "clientSecret", "test-client-secret",
                "refreshToken", "test-refresh-token"));

        OAuth2 task = OAuth2.builder()
                .clientId(Property.ofValue("{{ clientId }}"))
                .clientSecret(Property.ofValue("{{ clientSecret }}"))
                .refreshToken(Property.ofValue("{{ refreshToken }}"))
                .build();

        assertThrows(Exception.class, () -> task.run(runContext));
    }

    @Test
    void testOAuth2FlowMissingClientSecret() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
                "clientId", "test-client-id",
                "refreshToken", "test-refresh-token"));

        OAuth2 task = OAuth2.builder()
                .clientId(Property.ofValue("{{ clientId }}"))
                .clientSecret(Property.ofValue("{{ clientSecret }}"))
                .refreshToken(Property.ofValue("{{ refreshToken }}"))
                .build();

        assertThrows(Exception.class, () -> task.run(runContext));
    }

    @Test
    void testOAuth2FlowMissingRefreshToken() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
                "clientId", "test-client-id",
                "clientSecret", "test-client-secret"));

        OAuth2 task = OAuth2.builder()
                .clientId(Property.ofValue("{{ clientId }}"))
                .clientSecret(Property.ofValue("{{ clientSecret }}"))
                .refreshToken(Property.ofValue("{{ refreshToken }}"))
                .build();

        assertThrows(Exception.class, () -> task.run(runContext));
    }

    @Test
    void testTaskBuilderDefaults() throws IllegalVariableEvaluationException {
        RunContext runContext = runContextFactory.of(Map.of());
        OAuth2 task = OAuth2.builder()
                .clientId(Property.ofValue("test-client-id"))
                .clientSecret(Property.ofValue("test-client-secret"))
                .refreshToken(Property.ofValue("test-refresh-token"))
                .build();

        String tokenUrl = runContext.render(task.getTokenUrl()).as(String.class).orElse(null);
        assertThat(tokenUrl, equalTo("https://www.linkedin.com/oauth/v2/accessToken"));
    }
}