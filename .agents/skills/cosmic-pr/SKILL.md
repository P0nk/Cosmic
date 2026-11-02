---
name: cosmic-pr
description: Branch, commit and open a pull request on the user's Cosmic fork for a Kanban card, then hand the card to review. Use at the end of every coding card.
version: 1.0.0
platforms: [linux]
metadata:
  hermes:
    tags: [cosmic, git, github, pr]
    related_skills: [cosmic-build-test]
---
# Cosmic PR workflow

## When to Use
Finishing any card that changed files in the Cosmic repo.

## Procedure
1. Confirm you are in the card's worktree and on `agent/<card-id>-<slug>` (`git branch --show-current`).
   If not, `git switch -c agent/<card-id>-<slug>`.
2. `git fetch origin && git rebase origin/master`. Resolve conflicts only in files your lane owns; otherwise block.
3. Run `cosmic-build-test`. Do not open a PR on a red build.
4. Commit with a conventional message: `<lane>: <what> (<card-id>)`, e.g. `content: fix Ellinia 3rd job quest NPC (t_8f2a)`.
5. `git push -u origin HEAD`
6. `gh pr create --base master --title "<same as commit>" --body-file <filled .github/pull_request_template.md>`
   Add label = lane name (`gh pr edit --add-label cosmic-content`).
7. `kanban_request_review` with the PR URL and a 3-line summary. The reviewer profile picks it up.
8. If the reviewer calls `kanban_request_changes`, push fixes to the same branch — never open a second PR.

## Pitfalls
- Never `git push --force` to `master`; `--force-with-lease` on your own agent branch only.
- Never merge your own PR. The human merges (CI must be green).
- Keep PRs < ~400 changed lines (excluding generated data). Split otherwise.
