# Kestra Linkedin Plugin

## What

- Provides plugin components under `io.kestra.plugin.linkedin`.
- Includes classes such as `OAuth2`, `CommentTrigger`, `GetPostAnalytics`.

## Why

- What user problem does this solve? Teams need to authenticate with LinkedIn and fetch post analytics or comment triggers from orchestrated workflows instead of relying on manual console work, ad hoc scripts, or disconnected schedulers.
- Why would a team adopt this plugin in a workflow? It keeps LinkedIn steps in the same Kestra flow as upstream preparation, approvals, retries, notifications, and downstream systems.
- What operational/business outcome does it enable? It reduces manual handoffs and fragmented tooling while improving reliability, traceability, and delivery speed for processes that depend on LinkedIn.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `linkedin`

Infrastructure dependencies (Docker Compose services):

- `app`

### Key Plugin Classes

- `io.kestra.plugin.linkedin.CommentTrigger`
- `io.kestra.plugin.linkedin.GetPostAnalytics`
- `io.kestra.plugin.linkedin.OAuth2`

### Project Structure

```
plugin-linkedin/
├── src/main/java/io/kestra/plugin/linkedin/
├── src/test/java/io/kestra/plugin/linkedin/
├── build.gradle
└── README.md
```

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
