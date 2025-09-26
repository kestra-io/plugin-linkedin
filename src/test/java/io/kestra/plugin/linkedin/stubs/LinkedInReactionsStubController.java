package io.kestra.plugin.linkedin.stubs;

import io.micronaut.http.*;
import io.micronaut.http.annotation.*;
import io.micronaut.core.annotation.Nullable;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Controller
public class LinkedInReactionsStubController {
    @Get("/reactions/{path:.*}")
    public HttpResponse<String> reactions(
            @PathVariable String path,
            @QueryValue @Nullable String q) {
        if (!path.startsWith("(entity:") || !"entity".equals(q)) {
            return HttpResponse.status(HttpStatus.NOT_FOUND);
        }

        if (!path.endsWith(")")) {
            return HttpResponse.status(HttpStatus.NOT_FOUND);
        }
        String encodedUrn = path.substring("(entity:".length(),path.length()-1);
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
