package dtb.terminalIO.events.focus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dtb.terminalIO.components.HasName;
import dtb.terminalIO.util.Logging;

/**
 * TEMPORARY (focus redesign step 7a): compares each focus manager's kept child list with the one
 * worked out from its component (FocusManager.UTIL.derivedChildFocusables) and logs every
 * difference, so we know exactly where they disagree before switching to the worked-out lists.
 * Changes nothing. Off unless started with -Ddtb.focus.shadow=true; runs after every event the
 * root focus manager handles; each distinct difference is logged once, marked "SHADOW".
 */
final class FocusShadowCheck {

    static final boolean ENABLED = Boolean.getBoolean("dtb.focus.shadow");

    private static final Set<String> reported = new HashSet<String>();

    private FocusShadowCheck() {
    }

    static void check(FocusManager root) {
        try {
            checkTree(root, new HashSet<Object>());
        } catch (RuntimeException e) {
            report("SHADOW check failed: " + e);
        }
    }

    private static void checkTree(FocusManager focusManager, Set<Object> seen) {
        if (!seen.add(focusManager)) {
            report("SHADOW CYCLE at " + name(focusManager));
            return;
        }
        List<Focusable> kept = new ArrayList<Focusable>(focusManager.getChildFocusables());
        List<Focusable> derived = FocusManager.UTIL.derivedChildFocusables(focusManager);
        if (derived == null) {
            report("SHADOW NO-COMPONENT " + name(focusManager) + " kept=" + names(kept));
        } else {
            compare(focusManager, kept, derived);
        }
        for (Focusable child : kept) {
            checkParent(focusManager, child);
            if (child instanceof FocusManager) {
                checkTree((FocusManager) child, seen);
            }
        }
    }

    /**
     * The parent worked out by climbing the component tree from child's component to the nearest
     * component that is a focus manager - compared with child's stored parent link (and with the
     * focus manager whose list holds it).
     */
    private static void checkParent(FocusManager listedIn, Focusable child) {
        Object component = componentOf(child);
        if (component == null) {
            report("SHADOW PARENT NO-COMPONENT-FOR " + name(child) + " in " + name(listedIn));
            return;
        }
        FocusManager climbed = climbToFocusManager(component);
        FocusManager stored = child.getParentFocusManager();
        boolean matchesStored = climbed == null ? stored == null : stored != null && FocusManager.UTIL.isSameNode(climbed, stored);
        boolean matchesList = climbed != null && FocusManager.UTIL.isSameNode(climbed, listedIn);
        if (!matchesStored || !matchesList) {
            report("SHADOW PARENT DIFF " + name(child) + " | climbed=" + name(climbed) + " stored=" + name(stored)
                   + " listedIn=" + name(listedIn));
        }
    }

    /** the component behind a focusable: itself, a wrapper's inner manager's wrapper, or a focus behaviour's component */
    static Object componentOf(Focusable focusable) {
        if (focusable instanceof dtb.terminalIO.components.Component) {
            return focusable;
        }
        if (focusable instanceof DefaultFocusManager) {
            Object actual = ((DefaultFocusManager) focusable).meOrActual();
            return actual instanceof dtb.terminalIO.components.Component ? actual : null;
        }
        for (Class<?> c = focusable.getClass(); c != null; c = c.getSuperclass()) {   // TEMPORARY: reflection
            try {
                java.lang.reflect.Field f = c.getDeclaredField("component");
                f.setAccessible(true);
                Object o = f.get(focusable);
                if (o instanceof dtb.terminalIO.components.Component) {
                    return o;
                }
            } catch (NoSuchFieldException e) {
                // try the superclass
            } catch (IllegalAccessException e) {
                return null;
            }
        }
        return null;
    }

    /** climb component's parents; the first that is (or provides) a focus manager; null at the top */
    static FocusManager climbToFocusManager(Object component) {
        Object up = ((dtb.terminalIO.components.Component<?>) component).getParentContainer();
        while (up != null) {
            if (ToFocusable.UTIL.isFocusable(up)) {
                Focusable f = ToFocusable.UTIL.toFocusable(up);
                if (f instanceof FocusManager) {
                    return (FocusManager) f;
                }
                report("SHADOW PARENT LEAF-ANCESTOR " + name(up) + " above " + name(component));
            }
            if (!(up instanceof dtb.terminalIO.components.Component)) {
                return null;
            }
            up = ((dtb.terminalIO.components.Component<?>) up).getParentContainer();
        }
        return null;
    }

    private static void compare(FocusManager focusManager, List<Focusable> kept, List<Focusable> derived) {
        List<Focusable> onlyKept = new ArrayList<Focusable>();
        for (Focusable k : kept) {
            if (indexOfSameNode(derived, k) < 0) {
                onlyKept.add(k);
            }
        }
        List<Focusable> onlyDerived = new ArrayList<Focusable>();
        for (Focusable d : derived) {
            if (indexOfSameNode(kept, d) < 0) {
                onlyDerived.add(d);
            }
        }
        boolean sameOrder = true;
        if (onlyKept.isEmpty() && onlyDerived.isEmpty()) {
            for (int i = 0; i < kept.size(); i++) {
                if (!FocusManager.UTIL.isSameNode(kept.get(i), derived.get(i))) {
                    sameOrder = false;
                }
            }
        }
        if (onlyKept.isEmpty() && onlyDerived.isEmpty() && sameOrder) {
            if (Boolean.getBoolean("dtb.focus.shadow.ok")) {
                report("SHADOW OK at " + name(focusManager) + " " + names(kept));
            }
            return;
        }
        report("SHADOW DIFF at " + name(focusManager)
               + (onlyKept.isEmpty() ? "" : " | only-kept=" + names(onlyKept))
               + (onlyDerived.isEmpty() ? "" : " | only-derived=" + names(onlyDerived))
               + (sameOrder ? "" : " | ORDER kept=" + names(kept) + " derived=" + names(derived)));
    }

    private static int indexOfSameNode(List<Focusable> list, Focusable f) {
        for (int i = 0; i < list.size(); i++) {
            if (FocusManager.UTIL.isSameNode(list.get(i), f)) {
                return i;
            }
        }
        return -1;
    }

    private static void report(String line) {
        if (reported.add(line)) {
            Logging.info(line);
            System.err.println(line);
        }
    }

    private static String names(List<Focusable> focusables) {
        List<String> names = new ArrayList<String>();
        for (Focusable f : focusables) {
            names.add(name(f));
        }
        return names.toString();
    }

    private static String name(Object o) {
        if (o == null) {
            return "null";
        }
        try {
            return String.valueOf(HasName.UTIL.getName(o)) + "@" + o.getClass().getSimpleName();
        } catch (RuntimeException e) {
            return o.getClass().getSimpleName();
        }
    }
}
