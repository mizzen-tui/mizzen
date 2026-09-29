# CLAUDE.md

Pure-Java terminal UI library (Java 1.8, Maven, no third-party deps).
Architecture notes, including known warts: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). Read it
before touching events or focus. The focus system (nested FocusManagers) has a known design
flaw; see its §4.

## Git commits

- Do NOT add a `Co-Authored-By` line (or any other AI attribution) to commit messages.
- Keep commit messages short: a subject line plus at most ~4 lines of body.
- Size the message by how much the *design* changed, not by how many files changed.
  A mechanical or repetitive change across many files (refactor, rename, API
  adaptation) gets one line saying so. Expand only when the commit changes
  architecture or behaviour in ways a reader needs explained.
