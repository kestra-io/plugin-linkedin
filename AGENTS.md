# Kestra Linkedin Plugin

## What

- Provides plugin components under `io.kestra.plugin.linkedin`.
- Includes classes such as `OAuth2`, `CommentTrigger`, `GetPostAnalytics`.

## Why

- This plugin integrates Kestra with LinkedIn.
- It provides tasks that authenticate with LinkedIn and fetch post analytics or comment triggers.

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
