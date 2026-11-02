# Card workflow rules (avoid failed starts and wasted tokens)

1. One branch = one live worktree. A follow-up on an open PR uses a new branch `agent/<slug>-fix`
   (based on the PR branch) or the lead first removes the finished card's worktree.
2. Concurrency cap: max 2 cards running/ready (ROADMAP.md). Sub-cards are created blocked; the lead unblocks them.
3. After creating a card, the lead checks it started (Diagnostics) within a minute; spawn failures cost no tokens
   but 2 failures = give up.
4. Java and gh come from Windows PATH; restart Hermes after any install so agents inherit it.
5. PR text follows PR_STYLE.md, and its claims must match what the tests actually assert.
