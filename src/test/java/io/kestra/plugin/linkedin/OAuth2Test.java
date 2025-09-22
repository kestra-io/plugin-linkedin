package io.kestra.plugin.linkedin;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@KestraTest
class OAuth2Test {

    @Test
    void shouldBuildTask() {
        OAuth2 task = OAuth2.builder()
                .clientId(Property.ofValue("test-client-id"))
                .clientSecret(Property.ofValue("test-client-secret"))
                .refreshToken(Property.ofValue("test-refresh-token"))
                .build();

        assertThat(task.getClientId(), is(notNullValue()));
        assertThat(task.getClientSecret(), is(notNullValue()));
        assertThat(task.getRefreshToken(), is(notNullValue()));
        assertThat(task.getTokenUrl(), is(notNullValue()));
    }

    @Test
    void shouldTestOutputClass() {
        Instant now = Instant.now();
        Long expiresIn = 3600L;

        OAuth2.Output output = OAuth2.Output.builder()
                .accessToken("test-access-token-123")
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .scope("r_liteprofile r_emailaddress w_member_social")
                .expiresAt(now.plusSeconds(expiresIn))
                .build();

        assertThat(output.getAccessToken(), is("test-access-token-123"));
        assertThat(output.getTokenType(), is("Bearer"));
        assertThat(output.getExpiresIn(), is(3600L));
        assertThat(output.getScope(), is("r_liteprofile r_emailaddress w_member_social"));
        assertThat(output.getExpiresAt(), is(notNullValue()));
        assertThat(output.getExpiresAt(), is(now.plusSeconds(expiresIn)));
    }

    @Test
    void shouldTestOutputWithNullValues() {
        OAuth2.Output output = OAuth2.Output.builder()
                .accessToken("test-access-token-123")
                .tokenType("Bearer")
                .expiresIn(null)
                .scope(null)
                .expiresAt(null)
                .build();

        assertThat(output.getAccessToken(), is("test-access-token-123"));
        assertThat(output.getTokenType(), is("Bearer"));
        assertThat(output.getExpiresIn(), is(nullValue()));
        assertThat(output.getScope(), is(nullValue()));
        assertThat(output.getExpiresAt(), is(nullValue()));
    }

    @Test
    void shouldTestRequiredFields() {
        OAuth2 task = OAuth2.builder()
                .clientId(Property.ofValue("test-client-id"))
                .clientSecret(Property.ofValue("test-client-secret"))
                .refreshToken(Property.ofValue("test-refresh-token"))
                .build();

        assertThat(task.getClientId(), is(notNullValue()));
        assertThat(task.getClientSecret(), is(notNullValue()));
        assertThat(task.getRefreshToken(), is(notNullValue()));
    }

    @Test
    void shouldTestMinimalOutput() {
        OAuth2.Output output = OAuth2.Output.builder()
                .accessToken("minimal-token")
                .build();

        assertThat(output.getAccessToken(), is("minimal-token"));
        assertThat(output.getTokenType(), is(nullValue()));
        assertThat(output.getExpiresIn(), is(nullValue()));
        assertThat(output.getScope(), is(nullValue()));
        assertThat(output.getExpiresAt(), is(nullValue()));
    }
}