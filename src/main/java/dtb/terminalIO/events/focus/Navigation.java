package dtb.terminalIO.events.focus;

/**
 * Answer to {@link Focusable#advance(Focusable, FocusDirection)}: a nav key was pressed while
 * focus is inside a focus manager - where does focus go?
 */
public final class Navigation {

    public enum Kind {
        /** focus moves to {@link Navigation#getTarget()} */
        MOVED,
        /** nothing more to visit here in that direction; the parent decides */
        DONE,
        /** key handled, focus stays where it is (e.g. arrow at the end of a list) */
        STAY
    }

    public static final Navigation DONE = new Navigation(Kind.DONE, null);
    public static final Navigation STAY = new Navigation(Kind.STAY, null);

    public static Navigation movedTo(Focusable target) {
        if (target == null) {
            throw new NullPointerException("Navigation.movedTo(): target is null");
        }
        return new Navigation(Kind.MOVED, target);
    }

    private final Kind kind;
    private final Focusable target;

    private Navigation(Kind kind, Focusable target) {
        this.kind = kind;
        this.target = target;
    }

    public Kind getKind() {
        return kind;
    }

    /** the focusable to move to; null unless MOVED */
    public Focusable getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return kind == Kind.MOVED ? "MOVED(" + target + ")" : kind.toString();
    }
}
