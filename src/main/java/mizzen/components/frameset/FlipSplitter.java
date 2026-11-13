package mizzen.components.frameset;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import mizzen.components.AbstractComponentWrappper;
import mizzen.components.BoundedComponent;
import mizzen.components.Component;
import mizzen.components.composite.Composite;
import mizzen.components.input.LeafFocusBehaviour;
import mizzen.components.theme.Theme;
import mizzen.controlcodes.color.Color;
import mizzen.events.EventBus;
import mizzen.events.RequestUpdateCanvasEvent;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.AsKeyboardHandler;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyboardHandler;
import mizzen.events.keyboard.Keys;
import mizzen.events.mouse.ApplicationMouseEvent;
import mizzen.events.mouse.Clickable;
import mizzen.events.mouse.MOUSE_ACTION;
import mizzen.geom.Align;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.VAlign;
import mizzen.geom.position.PositionRelativeFactory;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment;

/**
 * Two panes that flip between stacked (the second below the first) and beside (the second to its right). A small
 * control floats at its top right, over whatever is there - its own border, or the panes' - and does the flipping.
 * <pre>
 *     FlipSplitter split = new FlipSplitter(chart, figures).share(60).setTitle(" Visitor stats ");
 *
 *     ┌ Visitor stats ─────────────────────────────── ▀ ▌ ─┐       stacked: the divider joins the border
 *     │ chart                                               │
 *     ├────────────────────────⣿⣿──────────────────────────┤
 *     │ figures                                             │
 *     └─────────────────────────────────────────────────────┘
 * </pre>
 * No border by default: the panes bring their own, or none. setBorder(true) (or a title) draws one around both, joined
 * to the divider - one set of lines, not a box around boxes.
 * <p/>
 * The panes are the same components after a flip: what they show, where they are scrolled, stays. A flip builds a new
 * splitter around them (a splitter can't give a pane back). Each placement keeps its own divider: dragged while
 * stacked, flipped beside and back, it is where it was left.
 * <p/>
 * The control reads ▀ ▌ (stacked, beside), the chosen one standing out; controlStyle(..) gives words instead.
 * offerSecondOnly(true) adds a third choice, █: the second pane alone. secondShown(false) shows the first pane alone,
 * and the control goes (there is nothing to flip).
 */
public class FlipSplitter extends AbstractComponentWrappper<Composite> {

    /** where the second pane goes */
    public enum Placement {
        BELOW("below", '▀'), RIGHT("right", '▌'), SECOND_ONLY("only", '█');

        private final String word;
        private final char glyph;                    // the terse style's: a block element, in every terminal font (PuTTY's too)

        Placement(String word, char glyph) {
            this.word = word;
            this.glyph = glyph;
        }

        /** "below", "right", "only" */
        public String word() {
            return word;
        }

        /** ▀ stacked, ▌ beside, █ alone: the filled part is the pane on top, at the left, or the only one */
        public char glyph() {
            return glyph;
        }
    }

    /**
     * How the control reads: TERSE "▀ ▌" (the default; the chosen one stands out), PLAIN "(•) Below  ( ) Right", WORDY
     * "(•) Figures below  ( ) Figures right" (the noun is the second pane's name).
     */
    public enum Style { WORDY, PLAIN, TERSE }

    // fromAlign's spacer for the control: it ends two columns in from the right edge - on a border, the corner and one
    // line show past it ("▀ ▌─┐")
    private static final int FROM_THE_RIGHT = 1;

    private final Component<?> first;
    private final Component<?> second;
    private final Control control = new Control();
    private Placement placement = Placement.BELOW;
    private boolean secondOnlyOffered = false;
    private boolean secondShown = true;
    private boolean bordered = false;
    private String title;
    private final Map<Placement, Integer> shares = new EnumMap<Placement, Integer>(Placement.class);   // the first pane's %, as left
    private int share = 50;
    private boolean sized = false;                       // given a size: until then the splitter's proportions are its panes', not a divider's
    private Component<?> arrangement;
    // the control is placed against the arrangement: at the top right of what shows
    private final Rectangle.Delegate anchor = new Rectangle.Delegate(Rectangle.ZERO);   // the arrangement, once made
    private final List<Consumer<Placement>> flipWatchers = new ArrayList<Consumer<Placement>>();

    /** first above or left, second below or right of it; stacked at first, half each, no border */
    public FlipSplitter(Component<?> first, Component<?> second) {
        super(null, new Composite(Dimension.ZERO));                // as a splitter: sized by what holds it, no size till then -
                                                                   // worked out from its children, the floating control would count
        if (first == null || second == null) {
            throw new NullPointerException("FlipSplitter: two panes, not " + first + " and " + second);
        }
        this.first = first;
        this.second = second;
        setName("flipSplitter");
        Composite area = getWrappedComponent();
        area.add(PositionRelativeFactory.fromAlign(VAlign.TOP, 0, Align.RIGHT, FROM_THE_RIGHT, anchor, control), control);
        area.keepOnTop(control);                                   // over the arrangement, whatever focus raises in it
        rebuild();
    }

    /** the first pane's share, in percent, for both placements: the dividers go there, wherever they were dragged */
    public FlipSplitter share(int percent) {
        this.share = Math.max(1, Math.min(99, percent));
        shares.clear();
        arrangement = forget(arrangement);
        rebuild();
        return this;
    }

    /** true: one border around both panes, joined to the divider; false (the default): none */
    public FlipSplitter setBorder(boolean bordered) {
        if (this.bordered != bordered) {
            this.bordered = bordered;
            rebuild();
        }
        return this;
    }

    /** a title on its border (and so a border); null: none */
    public FlipSplitter setTitle(String title) {
        this.title = title;
        if (title != null) {
            this.bordered = true;
        }
        rebuild();
        return this;
    }

    /** where the second pane goes now */
    public Placement getPlacement() {
        return placement;
    }

    /** puts the second pane there; the watchers aren't told (they hear the user) */
    public FlipSplitter placement(Placement placement) {
        if (Placement.SECOND_ONLY.equals(placement) && !secondOnlyOffered) {
            throw new IllegalArgumentException("FlipSplitter: SECOND_ONLY isn't offered - offerSecondOnly(true) first");
        }
        if (this.placement != placement) {
            this.placement = placement;
            rebuild();
        }
        return this;
    }

    /** true: a third choice, the second pane alone */
    public FlipSplitter offerSecondOnly(boolean offered) {
        this.secondOnlyOffered = offered;
        if (!offered && Placement.SECOND_ONLY.equals(placement)) {
            placement(Placement.BELOW);
        }
        control.fit();
        return this;
    }

    /** false: the first pane alone, the second put away (kept as it is, to show again), and no control */
    public FlipSplitter secondShown(boolean shown) {
        if (this.secondShown != shown) {
            this.secondShown = shown;
            rebuild();
        }
        return this;
    }

    public boolean isSecondShown() {
        return secondShown;
    }

    /** told after the user flips it (the control: a click, a key), with the new placement - any number of them */
    public FlipSplitter onFlip(Consumer<Placement> watcher) {
        flipWatchers.add(watcher);
        return this;
    }

    /**
     * How the control reads (TERSE by default). It is never cut: given less than its style needs (more than half the
     * splitter's width), it reads tersely, and wider again the style asked for comes back. noun names the second pane in
     * the WORDY style ("Figures below").
     */
    public FlipSplitter controlStyle(Style style, String noun) {
        control.style = style;
        control.noun = noun;
        control.fit();
        return this;
    }

    /** the control at its top right: a TAB stop, for focus or a test */
    public Control getControl() {
        return control;
    }

    @Override
    public void resize(Dimension d) {
        sized = d.getHeight() > 0 && d.getWidth() > 0;
        super.resize(d);
        control.fit();
    }

    // ---- the arrangement -----------------------------------------------------------------------------------------

    // a new splitter (or bordered panel) around the same panes; the old one goes, its divider remembered
    private void rebuild() {
        if (arrangement != null) {
            rememberShare();
            arrangement = forget(arrangement);
        }
        detach(first);
        detach(second);
        Composite area = getWrappedComponent();
        arrangement = arrange();
        anchor.setRectangle(arrangement);
        area.add(arrangement);
        area.onResizeExpandDownAndRight(arrangement);
        area.keepOnTop(control);
        area.applyResizeRules();
        if (secondShown) {
            control.show();
        } else {
            control.hide();                                        // one pane: nothing to flip
        }
        control.fit();
        markAsDirty();
    }

    // the arrangement taken out (its divider not remembered)
    private Component<?> forget(Component<?> old) {
        if (old != null) {
            getWrappedComponent().setOnResizeRule(null);
            getWrappedComponent().remove(old);
        }
        return null;
    }

    private Component<?> arrange() {
        if (!secondShown) {
            return alone(first);
        }
        switch (placement) {
            case RIGHT: {
                HorizontalSplitter split = new HorizontalSplitter().addComponent(first).addComponent(second);
                split.setBorder(bordered);                         // a frameset's border: the divider joins it
                if (title != null) {
                    split.setTitle(title);
                }
                split.onResizeExpandRight(first, share(Placement.RIGHT));
                return split;
            }
            case SECOND_ONLY:
                return alone(second);
            default: {
                VerticalSplitter split = new VerticalSplitter().addComponent(first).addComponent(second);
                split.setBorder(bordered);
                if (title != null) {
                    split.setTitle(title);
                }
                split.onResizeExpandDown(first, share(Placement.BELOW));
                return split;
            }
        }
    }

    // one pane: as it is, or in the border (and title) the splitter would have
    private Component<?> alone(Component<?> pane) {
        if (!bordered) {
            return pane;
        }
        Composite box = new Composite();
        box.setBorder(true);
        if (title != null) {
            box.setTitle(title);
        }
        box.add(pane);
        box.onResizeExpandDownAndRight(pane);
        return box;
    }

    // the divider where it was left: the first pane's part, as a percent - rounded up: the rule rounds its size down,
    // so it gives back the same rows (exactly, under 100 of them)
    private void rememberShare() {
        if (!sized) {
            return;                                           // not laid out yet: no divider to keep
        }
        int inside = bordered ? 2 : 0;                        // the rule's share is of the inside: the border's lines aren't in it
        if (arrangement instanceof HorizontalSplitter && arrangement.getWidth() > inside) {
            shares.put(Placement.RIGHT, (int) Math.ceil(100.0 * first.getWidth() / (arrangement.getWidth() - inside)));
        } else if (arrangement instanceof VerticalSplitter && arrangement.getHeight() > inside) {
            shares.put(Placement.BELOW, (int) Math.ceil(100.0 * first.getHeight() / (arrangement.getHeight() - inside)));
        }
    }

    private int share(Placement placement) {
        Integer kept = shares.get(placement);
        return kept != null ? kept : share;
    }

    // out of the arrangement thrown away: a splitter can't give a pane back, so its parent is cleared
    private static void detach(Component<?> pane) {
        pane.setParentContainer(null);
    }

    // the user picked one: flip, tell the watchers
    private void userPicks(Placement picked) {
        if (picked == placement) {
            return;
        }
        placement(picked);
        for (Consumer<Placement> watcher : new ArrayList<Consumer<Placement>>(flipWatchers)) {
            watcher.accept(picked);
        }
    }

    private List<Placement> offered() {
        List<Placement> offered = new ArrayList<Placement>(3);
        offered.add(Placement.BELOW);
        offered.add(Placement.RIGHT);
        if (secondOnlyOffered) {
            offered.add(Placement.SECOND_ONLY);
        }
        return offered;
    }

    // ---- the control ---------------------------------------------------------------------------------------------

    /**
     * The choices, one row: a click picks one; focused (a TAB stop), LEFT / RIGHT move to the one before / after and
     * SPACE to the next, round again. Each flips the splitter at once.
     */
    public final class Control extends BoundedComponent<Control> implements ToFocusable, Clickable, AsKeyboardHandler {

        {
            defaultThemeColor(Theme::inputColor);           // given no colour: the theme's - an input
        }

        private Style style = Style.TERSE;
        private String noun;
        private Style shown = Style.TERSE;                   // the style drawn: style, or terser where room is short

        private final LeafFocusBehaviour focus = LeafFocusBehaviour.of(this)
                .look(LeafFocusBehaviour.Look.NONE)
                .whenFocusArrives(this::repaint)
                .whenFocusLeaves(this::repaint)
                .handleKeys(this::key);

        private Control() {
            super((Color) null, new Dimension.Simple(1, 3));
            setName("flipControl");
        }

        /** the style it is drawn in now: the one asked for, or terser while there's no room for it */
        public Style getShownStyle() {
            return shown;
        }

        // the widest style, up to the one asked for, that takes at most half the splitter's width
        private void fit() {
            int room = FlipSplitter.this.getWidth() / 2;
            Style fits = Style.TERSE;
            for (Style s : Style.values()) {
                if (s.ordinal() >= style.ordinal() && width(s) <= room) {
                    fits = s;
                    break;
                }
            }
            shown = fits;
            int width = width(shown);
            if (width != getWidth()) {
                resize(new Dimension.Simple(1, width));
            }
            markAsDirty();
        }

        private int width(Style s) {
            int width = 0;
            for (String label : labels(s)) {
                width += label.length();
            }
            return width + gap(s) * (offered().size() - 1);
        }

        private int gap(Style s) {
            return Style.TERSE.equals(s) ? 1 : 2;
        }

        // each choice as drawn: "(•) Below", "( ) Figures right", "▌"
        private List<String> labels(Style s) {
            List<String> labels = new ArrayList<String>(3);
            for (Placement p : offered()) {
                String mark = p == placement ? "(•) " : "( ) ";
                if (Style.TERSE.equals(s)) {
                    labels.add(String.valueOf(p.glyph()));
                } else if (Style.WORDY.equals(s) && noun != null) {
                    labels.add(mark + noun + " " + p.word());
                } else {
                    labels.add(mark + Character.toUpperCase(p.word().charAt(0)) + p.word().substring(1));
                }
            }
            return labels;
        }

        // the choice at a column; null: between them
        private Placement placementAt(int col) {
            List<String> labels = labels(shown);
            List<Placement> offered = offered();
            int start = 0;
            for (int i = 0; i < labels.size(); i++) {
                int end = start + labels.get(i).length();
                if (col >= start && col < end) {
                    return offered.get(i);
                }
                start = end + gap(shown);
            }
            return null;
        }

        private boolean key(KeyEvent ke) {
            List<Placement> offered = offered();
            int at = offered.indexOf(placement);
            if (ke.isKeyStroke(Keys.LEFT_KEYSTROKE)) {
                return picks(offered.get(Math.max(0, at - 1)), ke.getEventBus());
            } else if (ke.isKeyStroke(Keys.RIGHT_KEYSTROKE)) {
                return picks(offered.get(Math.min(offered.size() - 1, at + 1)), ke.getEventBus());
            } else if (ke.isKeyStroke(Keys.SPACE_KEYSTROKE)) {
                return picks(offered.get((at + 1) % offered.size()), ke.getEventBus());
            }
            return false;
        }

        // true: the key is used (at an end too: the choice stays rather than focus moving on)
        private boolean picks(Placement picked, EventBus bus) {
            userPicks(picked);
            repaint(bus);
            return true;
        }

        @Override
        public void onClick(ApplicationMouseEvent appMouseEvent) {
            if (!MOUSE_ACTION.DOWN_LEFT.equals(appMouseEvent.getMouseAction())) {
                return;
            }
            EventBus bus = appMouseEvent.getEventBus();
            focus.requestFocus(bus);
            Placement picked = placementAt(appMouseEvent.getMousePosition().getCol());
            if (picked != null) {
                picks(picked, bus);
            }
        }

        @Override
        public void onClick(EventBus eventBus) {
            focus.requestFocus(eventBus);
        }

        private void repaint(EventBus bus) {
            markAsDirty();
            RequestUpdateCanvasEvent.invoke(bus, this);
        }

        @Override
        protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
            Color chosen = focus.hasFocus() ? Color.onBackgroundOf(getTheme().accentColor().getForeGroundColor(), color).bold()
                                            : color.bold();
            Color other = Style.TERSE.equals(shown) ? Color.onBackgroundOf(getTheme().dimColor().getForeGroundColor(), color) : color;
            List<String> labels = labels(shown);
            List<Placement> offered = offered();
            int col = 0;
            for (int i = 0; i < labels.size(); i++) {
                String label = labels.get(i);
                if (i > 0) {
                    String gap = Style.TERSE.equals(shown) ? " " : "  ";
                    segments.merge(ScreenLineSegment.UTIL.segmentsOf(gap, color, new Rectangle.Simple(0, col, 1, gap.length())));
                    col += gap.length();
                }
                Color c = offered.get(i) == placement ? chosen : other;
                segments.merge(ScreenLineSegment.UTIL.segmentsOf(label, c, new Rectangle.Simple(0, col, 1, label.length())));
                col += label.length();
            }
        }

        @Override
        public Focusable asFocusable() {
            return focus.asFocusable();
        }

        @Override
        public KeyboardHandler asKeyboardHandler() {
            return focus.asKeyboardHandler();
        }
    }
}
