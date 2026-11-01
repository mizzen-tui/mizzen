#!/bin/bash
# TextSnapshot under cron-like conditions: no terminal, empty environment, read-only folder, closed input.
# Runs the demo (one of each component), then checks the result. Run from the project's top, after mvn package.
#   src/test/scripts/snapshot-acceptance.sh [java]    default: the java on the PATH
set -u
JAR="$PWD/target/mizzen-0.1.0.jar"
JAVA="$(readlink -f "$(command -v "${1:-java}")")"
OUT="$(mktemp -d)"; REPORT="$OUT/report.txt"; FAILED=0
pass() { echo "  ok    $1"; }
fail() { echo "  FAIL  $1"; FAILED=1; }
"$JAVA" -version 2>&1 | head -1
cd /
START=$(date +%s)
setsid -w env -i "$JAVA" -cp "$JAR" mizzen.examples.SnapshotReportDemo "$REPORT" </dev/null >"$OUT/out.txt" 2>"$OUT/err.txt"
CODE=$?
ELAPSED=$(( $(date +%s) - START ))
[ "$CODE" -eq 0 ] && [ "$ELAPSED" -lt 30 ] && pass "exit 0, by itself, in ${ELAPSED}s (R5)" || fail "exit $CODE after ${ELAPSED}s (R5)"
[ ! -s "$OUT/err.txt" ] && pass "stderr empty (R3)" || { fail "stderr: $(head -3 "$OUT/err.txt")"; }
[ ! -s "$OUT/out.txt" ] && pass "stdout empty (R3)" || fail "stdout not empty (R3)"
python3 -I - "$REPORT" <<'PY' || FAILED=1
import re, sys
data = open(sys.argv[1], 'rb').read()
sections, bad = re.split(rb'^=== ', data, flags=re.M)[1:], 0
def check(ok, what):
    global bad
    print(("  ok    " if ok else "  FAIL  ") + what)
    bad += 0 if ok else 1
plain = [s for s in sections if not s.startswith(b'colours')]
check(all(b'\x1b' not in s for s in plain), "no 0x1B in the default output (R8)")
sizes_ok = True
for s in plain:
    head, _, body = s.partition(b'\n')
    h, w = map(int, head.rsplit(b' ', 1)[1].split(b'x'))
    lines = body.decode('utf-8').rstrip('\n').split('\n') if body.strip(b'\n') else []
    if len(lines) != h or any(len(l) != w for l in lines):
        sizes_ok = False; print("        wrong size:", head)
check(sizes_ok, "every section exactly height x width (R10)")
check(any(0x2800 < ord(c) <= 0x28FF for c in data.decode('utf-8')), "braille survives with no locale (R9)")
ascii_ok = all(max(s.partition(b'\n')[2] or b' ') < 128 for s in plain if s.startswith(b'ascii'))
check(ascii_ok, "ascii sections only 7-bit (R9)")
colours = [s for s in sections if s.startswith(b'colours')][0]
check(b'\x1b' in colours and b'\x1b' not in re.sub(rb'\x1b\[[0-9;]*m', b'', colours), "colour section: colour codes only (R8)")
sys.exit(1 if bad else 0)
PY
cp "$REPORT" "$OUT/first.txt"
setsid -w env -i "$JAVA" -cp "$JAR" mizzen.examples.SnapshotReportDemo "$REPORT" </dev/null >/dev/null 2>&1
cmp -s "$REPORT" "$OUT/first.txt" && pass "two runs identical (R10, R11)" || fail "two runs differ (R10, R11)"
if command -v strace >/dev/null; then
    setsid -w env -i strace -f -qq -o "$OUT/strace.txt" -e trace=execve,openat "$JAVA" -XX:-UsePerfData -cp "$JAR" mizzen.examples.SnapshotReportDemo "$REPORT" </dev/null >/dev/null 2>&1
    EXECS=$(grep -c 'execve(' "$OUT/strace.txt")
    OTHER=$(grep 'execve(' "$OUT/strace.txt" | grep -v "$(dirname "$JAVA")" | grep -v ENOENT | wc -l)
    [ "$OTHER" -eq 0 ] && pass "no child processes: only java started ($EXECS exec) (R2)" || fail "child processes: $(grep execve "$OUT/strace.txt" | grep -v "$JAVA" | head -3) (R2)"
    grep -q '/dev/tty' "$OUT/strace.txt" && fail "/dev/tty touched (R2)" || pass "no /dev/tty (R2)"
    CREATED=$(grep -E 'openat.*O_(CREAT|WRONLY|RDWR)' "$OUT/strace.txt" | grep -v ENOENT | grep -v EACCES | grep -v "$REPORT" | grep -v '/proc/\|/sys/\|/dev/null' | wc -l)
    [ "$CREATED" -eq 0 ] && pass "no files written but the report; the JVM's own perf file turned off for this run (R3)" || { fail "files written (R3):"; grep -E 'openat.*O_(CREAT|WRONLY|RDWR)' "$OUT/strace.txt" | grep -v ENOENT | grep -v EACCES | grep -v "$REPORT" | grep -v '/proc/\|/sys/\|/dev/null' | head -5; }
else
    echo "  skip  strace not installed (R2, R3)"
fi
echo "Report: $REPORT"
echo "Real cron, once by hand: * * * * * $JAVA -cp $JAR mizzen.examples.SnapshotReportDemo /tmp/mizzen-cron-report.txt  then: diff /tmp/mizzen-cron-report.txt $REPORT"
exit $FAILED
