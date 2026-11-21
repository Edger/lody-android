// The dispatcher calls an adapter as `(...nativeArgs, signal)`, so an omitted optional argument
// leaves the AbortSignal in its position. Accept only the machine's comparison scopes, never an
// arbitrary string from a misplaced argument.
const DIFF_SCOPES = new Set(['branch', 'uncommitted']);
export const withDiffScope = (params, scope) =>
  DIFF_SCOPES.has(scope) ? { ...params, scope } : params;
