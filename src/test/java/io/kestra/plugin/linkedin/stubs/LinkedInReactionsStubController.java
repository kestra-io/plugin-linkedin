package io.kestra.plugin.linkedin.stubs;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.*;
import io.micronaut.http.annotation.*;

@Controller
public class LinkedInReactionsStubController {
    private static final AtomicReference<String> LAST_LINKEDIN_VERSION = new AtomicReference<>();

    public static String lastLinkedinVersion() {
        return LAST_LINKEDIN_VERSION.get();
    }

    public static void reset() {
        LAST_LINKEDIN_VERSION.set(null);
    }

    @Get("/reactions/{path:.*}")
    public HttpResponse<String> reactions(
        @PathVariable String path,
        @QueryValue @Nullable String q,
        @Header("LinkedIn-Version") @Nullable String linkedinVersion) {
        LAST_LINKEDIN_VERSION.set(linkedinVersion);

        if (!path.startsWith("(entity:") || !"entity".equals(q)) {
            return HttpResponse.status(HttpStatus.NOT_FOUND);
        }

        if (!path.endsWith(")")) {
            return HttpResponse.status(HttpStatus.NOT_FOUND);
        }
        String encodedUrn = path.substring("(entity:".length(), path.length() - 1);
        String urn = URLDecoder.decode(encodedUrn, StandardCharsets.UTF_8);

        String body = """
              {
                "elements": [
                  {
                    "id": "r1",
                    "reactionType": "LIKE",
                    "root": "%s",
                    "created": { "actor": "urn:li:person:abc", "time": 1700000000000 },
                    "lastModified": { "time": 1700000005000 }
                  },
                  {
                    "id": "r2",
                    "reactionType": "CELEBRATE",
                    "root": "%s",
                    "created": { "actor": "urn:li:person:def", "time": 1700001000000 },
                    "lastModified": { "time": 1700001005000 }
                  }
                ],
                "paging": { "total": 2 }
              }
            """.formatted(urn, urn);

        return HttpResponse.ok(body).contentType(MediaType.APPLICATION_JSON_TYPE);
    }
}
