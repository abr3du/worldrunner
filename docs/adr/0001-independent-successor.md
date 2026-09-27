---
status: accepted; narrowed for the prototype by ADR 0003
---

# Worldrunner is an independent successor to kmspiel, not a client of it

Worldrunner has its own backend, its own Accounts, and its own game data. It uses no kmspiel accounts, data, branding, or interfaces. We chose this over building an official kmspiel client or a syncing companion because we have no agreement with the kmspiel owner, and both of those options would block all work on backend discovery. The alternative would be scraping or reverse engineering, which we rule out. This supersedes the "blocked on backend discovery" status of ADR-001 in `architecture-decisions.md`: option 3 from that list is now the plan.

## Consequences

- Existing kmspiel Runners start fresh. No migration of history, teams, or standings.
- Game rules are ours to define. They are inspired by kmspiel, but we do not need to match it exactly.
- We build and operate the backend. Authentication uses Google sign-in plus email magic link, so we never store passwords.
- Name, branding, and content must be original.
