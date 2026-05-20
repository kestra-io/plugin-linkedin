# How to use the LinkedIn plugin

Fetch LinkedIn post analytics and monitor comments from Kestra flows.

## Authentication

Tasks use a LinkedIn OAuth2 `accessToken` (Bearer token). Use the `OAuth2` task to exchange a `refreshToken` for a fresh access token — set `clientId`, `clientSecret`, and `refreshToken` (all required). The `tokenUrl` defaults to `https://www.linkedin.com/oauth/v2/accessToken`. The output `accessToken` can then be passed to other tasks. Store secrets in [secrets](https://kestra.io/docs/concepts/secret) and apply connection properties globally with [plugin defaults](https://kestra.io/docs/workflow-components/plugin-defaults).

## Tasks

`OAuth2` exchanges a refresh token for a new access token — set `clientId`, `clientSecret`, and `refreshToken` (all required). The output includes `accessToken`, `tokenType`, `expiresIn`, `scope`, and `expiresAt`.

`GetPostAnalytics` fetches reaction data for one or more LinkedIn posts — set `accessToken` (required) and `activityUrns` (required, list of LinkedIn activity URNs). The output includes `posts` (per-post reaction breakdown), `totalPosts`, and `totalReactions`.

## Triggers

`CommentTrigger` polls LinkedIn for new comments on a set of posts — set `accessToken` (required) and `postUrns` (required, list of LinkedIn post URNs to monitor). The polling `interval` defaults to 30 minutes. The trigger output includes `postUrn`, `commentId`, `commentUrn`, `commentText`, `actorUrn`, `createdTime`, `newCommentsCount`, and `allNewComments`.
