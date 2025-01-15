export function setsEqual<T>(a: Set<T>, b: Set<T>) {
  if (a == null || b == null) {
    return false;
  }
  return a.size === b.size && [...a].every((value) => b.has(value));
}
