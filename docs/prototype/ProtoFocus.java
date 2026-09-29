import java.util.*;

/**
 * Prototype of the proposed focus design (standalone, not library code).
 *
 *  - one FocusRoot holds a direct reference to the focused leaf
 *  - focus children are discovered on demand from the component tree (nothing stored)
 *  - the child decides: enter(dir) / advance(dir); null = "skip me" / "I'm done"
 *  - container kinds: PANEL (transparent), GROUP, WIDGET (one stop, remembers), TRAP
 *  - nav keys configurable per container
 *  - hand-off: every node on the old path below the split point gets
 *    unFocus(newFocusable) where newFocusable = new node at the split level
 *    (same semantics as today's FocusManager.UTIL.setNewFocusable)
 */
public class ProtoFocus {

    enum Dir { FORWARD, BACKWARD }
    enum Kind { PANEL, GROUP, WIDGET, TRAP }

    static int treeWalks = 0;                       // counts on-demand discovery walks
    static List<String> handoffLog = new ArrayList<>();

    // ------------------------------------------------------------------ tree
    static abstract class Node {
        final String name; Container parent; boolean enabled = true, visible = true;
        Node(String name) { this.name = name; }
        public String toString() { return name; }
        void unFocus(Node newFocusable) {}
    }

    static class Leaf extends Node {
        java.util.function.Consumer<Node> onUnFocus;
        Leaf(String n) { super(n); }
        boolean consumes(String key) { return key.length() == 1; }   // printable chars only
        void unFocus(Node newFocusable) { if (onUnFocus != null) onUnFocus.accept(newFocusable); }
    }

    static class Container extends Node {
        final Kind kind; final List<Node> children = new ArrayList<>();
        final Set<String> nextKeys = new HashSet<>(), prevKeys = new HashSet<>();
        boolean wrap;           // root / trap
        Node remembered;        // WIDGET only
        Container(String n, Kind k, Node... kids) {
            super(n); kind = k;
            for (Node c : kids) add(c);
            if (k == Kind.GROUP || k == Kind.TRAP) { nextKeys.add("TAB"); prevKeys.add("STAB"); }
            if (k == Kind.WIDGET) { nextKeys.add("DOWN"); prevKeys.add("UP"); }
            if (k == Kind.TRAP) wrap = true;
        }
        Container add(Node c) { c.parent = this; children.add(c); return this; }
        Dir dirFor(String key) {
            if (nextKeys.contains(key)) return Dir.FORWARD;
            if (prevKeys.contains(key)) return Dir.BACKWARD;
            return null;
        }
    }

    static Leaf leaf(String n) { return new Leaf(n); }
    static Container panel(String n, Node... k)  { return new Container(n, Kind.PANEL, k); }
    static Container group(String n, Node... k)  { return new Container(n, Kind.GROUP, k); }
    static Container widget(String n, Node... k) { return new Container(n, Kind.WIDGET, k); }
    static Container trap(String n, Node... k)   { return new Container(n, Kind.TRAP, k); }

    // --------------------------------------------------------- discovery
    /** focus children of c, computed now: transparent PANELs are flattened, hidden nodes dropped */
    static List<Node> focusChildren(Container c) {
        treeWalks++;
        List<Node> out = new ArrayList<>();
        collect(c, out);
        return out;
    }
    private static void collect(Container c, List<Node> out) {
        for (Node n : c.children) {
            if (!n.visible) continue;
            if (n instanceof Container && ((Container) n).kind == Kind.PANEL) collect((Container) n, out);
            else out.add(n);
        }
    }

    static boolean isAncestorOrSelf(Node maybeAncestor, Node n) {
        for (Node x = n; x != null; x = x.parent) if (x == maybeAncestor) return true;
        return false;
    }

    // ---------------------------------------------------------- protocol
    /** focus arrives from outside moving in dir: which leaf do I land on? null = skip me */
    static Leaf enter(Node n, Dir dir) {
        if (!n.enabled || !n.visible) return null;
        if (n instanceof Leaf) return (Leaf) n;
        Container c = (Container) n;
        List<Node> kids = focusChildren(c);
        if (c.kind == Kind.WIDGET && c.remembered != null && kids.contains(c.remembered)) {
            Leaf t = enter(c.remembered, dir);           // resume remembered, direction ignored
            if (t != null) return t;
        }
        // grouping behaviour (and widget fallback): first child forwards, last backwards
        // widget fallback is always "first" per constraint
        Dir order = c.kind == Kind.WIDGET ? Dir.FORWARD : dir;
        for (int i = 0; i < kids.size(); i++) {
            Node k = kids.get(order == Dir.FORWARD ? i : kids.size() - 1 - i);
            Leaf t = enter(k, dir);
            if (t != null) return t;
        }
        return null;
    }

    /** nav key pressed while focus is inside c: next leaf within c, or null = I'm done */
    static Leaf advance(Container c, Leaf focused, Dir dir) {
        List<Node> kids = focusChildren(c);
        int i = -1;
        for (int k = 0; k < kids.size(); k++) if (isAncestorOrSelf(kids.get(k), focused)) { i = k; break; }
        int step = dir == Dir.FORWARD ? 1 : -1;
        // up to kids.size() steps: when wrapping, the final step may land back on the
        // child we started in -- re-entering it from its first/last leaf is legitimate
        // (a root whose only child is a group must not dead-end)
        for (int n = 0, j = i; n < kids.size(); n++) {
            j += step;
            if (j < 0 || j >= kids.size()) {
                if (!c.wrap) return null;
                j = (j + kids.size()) % kids.size();
            }
            Leaf t = enter(kids.get(j), dir);
            if (t != null && t != focused) return t;
        }
        return null;
    }

    // ---------------------------------------------------------- focus root
    static class FocusRoot {
        final Container root; Leaf focused;
        FocusRoot(Container root) { this.root = root; root.wrap = true; }

        void press(String key) {
            if (focused == null) {                        // nothing focused yet: nav keys enter
                Dir d = root.dirFor(key);
                if (d != null) moveFocus(enter(root, d));
                return;
            }
            if (focused.consumes(key)) return;            // ordinary key: no tree walk at all
            for (Container c = focused.parent; c != null; c = c.parent) {   // bubble up
                if (c.kind == Kind.PANEL) continue;
                Dir d = c.dirFor(key);
                if (d == null) continue;
                Leaf t = advance(c, focused, d);
                if (t != null) { moveFocus(t); return; }
                if (c.kind == Kind.WIDGET) return;        // arrows at widget edge: stay, don't bubble
            }
        }

        void click(Leaf target) { if (enter(target, Dir.FORWARD) != null) moveFocus(target); }

        void moveFocus(Leaf target) {
            if (target == null || target == focused) return;
            if (focused != null) {
                List<Node> oldPath = path(focused), newPath = path(target);
                int k = 0;
                while (k < oldPath.size() && k < newPath.size() && oldPath.get(k) == newPath.get(k)) k++;
                Node newAtSplit = newPath.get(k);          // the new sibling at the split level
                for (int i = k; i < oldPath.size(); i++) oldPath.get(i).unFocus(newAtSplit);
            }
            // widgets on the new path remember which child now holds focus
            for (Node x = target; x.parent != null; x = x.parent)
                if (x.parent.kind == Kind.WIDGET) x.parent.remembered = x;
            focused = target;
        }

        static List<Node> path(Node n) {
            LinkedList<Node> p = new LinkedList<>();
            for (Node x = n; x != null; x = x.parent) p.addFirst(x);
            return p;
        }
    }

    // ------------------------------------------------------------- tests
    static int pass = 0, fail = 0;

    static String run(FocusRoot fr, String... keys) {
        StringBuilder sb = new StringBuilder();
        for (String k : keys) { fr.press(k); sb.append(fr.focused).append(' '); }
        return sb.toString().trim();
    }
    static String[] rep(String key, int n) { String[] a = new String[n]; Arrays.fill(a, key); return a; }

    static void check(String name, String actual, String expected) {
        boolean ok = actual.equals(expected);
        if (ok) pass++; else fail++;
        System.out.printf("%s %-58s %s%n", ok ? "PASS" : "FAIL", name, actual);
        if (!ok) System.out.printf("     %-58s %s  <-- expected%n", "", expected);
    }

    public static void main(String[] args) {
        // 1. grouping container: the NestedFocusBugTest layout
        { FocusRoot fr = new FocusRoot(group("root", leaf("A"), group("g", leaf("B"), leaf("C")), leaf("D")));
          check("1a group  A,[B,C],D  TAB x9", run(fr, rep("TAB", 9)), "A B C D A B C D A");
          check("1b group  then SHIFT-TAB x8", run(fr, rep("STAB", 8)), "D C B A D C B A"); }

        // 2. widget: one tab stop, remembers, arrows inside
        { FocusRoot fr = new FocusRoot(group("root", leaf("A"), widget("list", leaf("B"), leaf("C")), leaf("D")));
          check("2a widget A,{B,C},D  TAB x6", run(fr, rep("TAB", 6)), "A B D A B D");
          check("2b widget TAB,TAB,DOWN (move to C inside)", run(fr, "TAB", "TAB", "DOWN"), "A B C");
          check("2c widget TAB x4: re-enters at remembered C", run(fr, rep("TAB", 4)), "D A C D");
          check("2d widget SHIFT-TAB x3: remembered C again", run(fr, rep("STAB", 3)), "C A D");
          check("2e widget DOWN at last item stays put", run(fr, "STAB", "DOWN", "DOWN"), "C C C"); }

        // 3. single nested group as root's only child: no dead end
        { FocusRoot fr = new FocusRoot(group("root", group("outer", leaf("A"), group("g", leaf("B"), leaf("C")), leaf("D"))));
          check("3  outer group only child  TAB x9", run(fr, rep("TAB", 9)), "A B C D A B C D A"); }

        // 4. transparent panels are flattened (plain Composite with no focus policy)
        { FocusRoot fr = new FocusRoot(group("root", leaf("A"), panel("p", leaf("B"), panel("pp", leaf("C"))), leaf("D")));
          check("4  plain panels flattened  TAB x5", run(fr, rep("TAB", 5)), "A B C D A"); }

        // 5. disabled / hidden / empty are skipped
        { Leaf c = leaf("C"); Container g = group("g", leaf("B"), c); Leaf d = leaf("D");
          Container empty = group("empty");
          FocusRoot fr = new FocusRoot(group("root", leaf("A"), g, empty, d));
          c.enabled = false;
          check("5a disabled C skipped", run(fr, rep("TAB", 4)), "A B D A");
          g.enabled = false;
          check("5b whole group disabled skipped", run(fr, rep("TAB", 3)), "D A D");
          g.enabled = true; c.enabled = true; d.visible = false;
          // focused D is hidden: next TAB restarts from the top (moving focus off a hidden/disabled
          // focused component immediately is a separate, open question)
          check("5c hidden D skipped, empty group skipped", run(fr, rep("TAB", 4)), "A B C A"); }

        // 6. widget remembered child becomes disabled -> fall back to first
        { Leaf c = leaf("C");
          FocusRoot fr = new FocusRoot(group("root", leaf("A"), widget("list", leaf("B"), c), leaf("D")));
          run(fr, "TAB", "TAB", "DOWN", "TAB");   // A B C D  (remembers C)
          c.enabled = false;
          check("6  remembered C disabled -> falls back to B", run(fr, "TAB", "TAB"), "A B"); }

        // 7. trap (modal): TAB cycles inside, never leaves
        { FocusRoot fr = new FocusRoot(group("root", leaf("A"), trap("dialog", leaf("OK"), leaf("CANCEL")), leaf("D")));
          run(fr, "TAB", "TAB");                    // A OK
          check("7  trap cycles inside  TAB x5", run(fr, rep("TAB", 5)), "CANCEL OK CANCEL OK CANCEL"); }

        // 8. configurable nav keys: a group navigated with RIGHT/LEFT, TAB passes over it
        { Container bar = group("bar", leaf("B1"), leaf("B2"), leaf("B3"));
          bar.nextKeys.clear(); bar.prevKeys.clear(); bar.nextKeys.add("RIGHT"); bar.prevKeys.add("LEFT");
          FocusRoot fr = new FocusRoot(group("root", leaf("A"), bar, leaf("D")));
          check("8  custom nav keys", run(fr, "TAB", "TAB", "RIGHT", "RIGHT", "RIGHT", "TAB", "STAB"),
                "A B1 B2 B3 B3 D B3"); }

        // 9. dynamic add: no rebuild call needed
        { Container g = group("g", leaf("B"), leaf("C"));
          FocusRoot fr = new FocusRoot(group("root", leaf("A"), g, leaf("D")));
          run(fr, "TAB", "TAB");                    // A B
          g.add(leaf("E"));
          check("9  E added to group at runtime, TAB x6", run(fr, rep("TAB", 6)), "C E D A B C"); }

        // 10. click then TAB continues from clicked position; click into widget sets remembered
        { Leaf c = leaf("C"); Leaf d = leaf("D");
          FocusRoot fr = new FocusRoot(group("root", leaf("A"), widget("list", leaf("B"), c), d));
          fr.click(c);
          check("10a click C (in widget) then TAB", run(fr, "TAB"), "D");
          check("10b SHIFT-TAB re-enters widget at clicked C", run(fr, "STAB"), "C"); }

        // 11. hand-off: dropdown-like widget keeps popup open while focus moves inside it
        { Leaf text = leaf("ddText"), list = leaf("ddList");
          Container dd = widget("dropdown", text, list);
          text.onUnFocus = nf -> handoffLog.add("ddText.unFocus(new=" + nf + ") -> " + (isAncestorOrSelf(dd, nf) ? "keep open" : "close"));
          list.onUnFocus = nf -> handoffLog.add("ddList.unFocus(new=" + nf + ") -> " + (isAncestorOrSelf(dd, nf) ? "keep open" : "close"));
          FocusRoot fr = new FocusRoot(group("root", leaf("A"), dd, leaf("D")));
          run(fr, "TAB", "TAB", "DOWN", "TAB");      // A ddText ddList D
          check("11 hand-off log", String.join(" | ", handoffLog),
                "ddText.unFocus(new=ddList) -> keep open | ddList.unFocus(new=D) -> close"); }

        // 12. cost: ordinary keys never walk the tree
        { FocusRoot fr = new FocusRoot(group("root", leaf("A"), group("g", leaf("B"), leaf("C")), leaf("D")));
          fr.press("TAB");
          int before = treeWalks;
          for (int i = 0; i < 1000; i++) fr.press("x");
          check("12 tree walks for 1000 ordinary keypresses", String.valueOf(treeWalks - before), "0"); }

        System.out.printf("%n%d passed, %d failed%n", pass, fail);
        if (fail > 0) System.exit(1);
    }
}
