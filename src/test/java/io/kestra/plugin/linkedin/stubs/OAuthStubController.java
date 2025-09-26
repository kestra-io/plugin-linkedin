package io.kestra.plugin.linkedin.stubs;

import io.micronaut.http.*;
import io.micronaut.http.annotation.*;

import java.util.Map;

@Controller
public class OAuthStubController {
    @Post(uri = "/oauth/v2/accessToken", consumes = MediaType.APPLICATION_FORM_URLENCODED)
    public Map<String, Object> token() {
        return Map.of(
                "access_token", "mock-token",
                "expires_in", 3600,
                "token_type", "Bearer");
    }
}
