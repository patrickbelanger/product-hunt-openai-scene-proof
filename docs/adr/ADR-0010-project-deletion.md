# ADR-0010: Explicit ordinary-project deletion with recoverable media cleanup

Status: Accepted within Patrick's explicit PR9 lifecycle authorization
Date: 2026-09-14

## Context and decision

PR4/PR5/PR8 history is immutable within a retained project. Patrick explicitly
requires irreversible whole-project deletion in PR9, including owned history and
media. This supersedes retention only when the entire ordinary project is deleted;
it does not allow rewriting individual findings, decisions or source evidence.

DELETE requires the exact project name. The UI defaults focus to Keep project and
requires typing the name. Demo copies, including retired copies, refuse deletion;
Reset demo remains the existing replacement lifecycle. Active analysis is refused,
not canceled. The service takes the existing media gate and analysis admission lock,
then locks the project and commits its UUID-only deletion record plus project-owned
relational cascade. Immutable-history delete triggers permit only deleted-parent
cleanup; updates and direct history deletion remain rejected.

Physical media cleanup follows database commit through MediaStorage. Targets are
generated project UUIDs under the configured canonical root, never client paths.
The adapter validates the tree without following links, refuses symlinks/nonregular
entries, removes the owned tree and retains a zero-byte UUID deletion marker to
prevent stale readers/workers recreating directories. Packaged assets are unaffected.

## Failure and alternatives

Filesystem and PostgreSQL cannot commit atomically. A failed cleanup returns an
explicit pending-cleanup error; the UUID-only database record allows retrying the
same DELETE after project rows are gone. A successful replay returns 204, an unknown
UUID returns 404. The UI stays on its confirmation/error surface until success and
then navigates to the library. There is no automatic cleanup worker in PR9.

Deleting files first risks retained database history pointing at removed evidence
after rollback. Soft deletion alone fails the requested physical cleanup. A broker,
general storage collector or broad authorization platform exceeds PR9.

## Consequences and delivery

Ordinary retained projects keep their original authority/history guarantees.
Tiny UUID tombstones persist; no film content is kept in them. Pending cleanup after
process death requires retry using the known UUID. Trusted local storage ownership
remains an assumption; cross-process filesystem mutation/TOCTOU review belongs in
[PR10 preparation](../PR10-SECURITY-PLAN.md). PR9 tests must exercise complete
dependent state, cleanup/replay/failure and untouched neighboring assets.
September 18 remains unchanged; no provider behavior or PR10 implementation added.
