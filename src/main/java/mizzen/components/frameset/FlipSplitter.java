package mizzen.components.frameset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import mizzen.components.AbstractComponentWrappper;
import mizzen.components.Component;
import mizzen.components.composite.Composite;
import mizzen.components.input.ChipGroup;
import mizzen.components.input.DisplayableValue;
import mizzen.components.input.DisplayableValue.SimpleDisplayValue;
import mizzen.components.input.FormInput;
import mizzen.components.input.RadioGroup;
import mizzen.geom.Align;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.VAlign;
import mizzen.geom.position.PositionRelativeFactory;

/**
 * Two panes that flip between stacked (the second below the first) and beside (the second to its right). A control
 * floats at its top right, over whatever is there - its own border, or the panes' - and does the flipping.
 * <pre>
 *     FlipSplitter split = new FlipSplitter(chart, figures).share(60).setTitle(" Visitor stats ");
 *
 *     ┌ Visitor stats ──────────────────── (•) Below  ( ) Right─┐       stacked: the divider joins the border
 *     │ chart                                                    │
 *     ├───────────────────────────⣿⣿────────────────────────────┤
 *     │ figures                                                  │
 *     └──────────────────────────────────────────────────────────┘
 * </pre>
 * The control is any value component - a FormInput of Placement: FlipSplitter sets its value as it flips, and flips when
 * the user changes it. radio() and chips() make the usual two, their text yours or the defaults:
 * <pre>
 *     split.control(split.radio());                          (•) Below  ( ) Right
 *     split.control(split.chips());                          [═][║]      the divider's direction
 *     split.control(split.chips((p) -> p == Placement.BELOW ? "Stacked" : "Side by side"));
 *     split.control(split.radio(), split.chips());           the first that fits (the default)
 *     split.control(myOwn);                                  any FormInput&lt;Placement&gt; component of yours
 * </pre>
 * A control fits when it takes at most half the splitter's width; none fits: the last. It is never cut.
 * <p/>
 * No border by default: the panes bring their own, or none. setBorder(true) (or a title) draws one around both, joined
 * to the divider - one set of lines, not a box around boxes.
 * <p/>
 * The panes are the same components after a flip: what they show, where they are scrolled, stays. A flip builds a new
 * splitter around them (a splitter can't give a pane back). Each placement keeps its own divider: dragged while
 * stacked, flipped beside and back, it is where it was left.
 * <p/>
 * offerSecondOnly(true) adds a third choice: the second pane alone (before making the controls - they show the choices
 * there are then). secondShown(false) shows the first pane alone, and the control goes (there is nothing to flip).
 */
public class FlipSplitter extends AbstractComponentWrappper<Composite> {

    /** where the second pane goes */
    public enum Placement {
        BELOW("below"), RIGHT("right"), SECOND_ONLY("only");

        private final String word;

        Placement(String word) {
            this.word = word;
        }

        /** "below", "right", "only" - for a message ("Figures " + p.word()) */
        public String word() {
            return word;
        }
    }

    /** radio()'s text: Below, Right, Only */
    public static String words(Placement p) {
        return Character.toUpperCase(p.word().charAt(0)) + p.word().substring(1);
    }

    /**
     * chips()'s text: ═ stacked, ║ beside, █ alone - the divider's direction. Box drawing, in every terminal font
     * (PuTTY draws from one font only: it had no ⊟ ◫).
     */
    public static String pictures(Placement p) {
        return p == Placement.BELOW ? "═" : p == Placement.RIGHT ? "║" : "█";
    }

    // fromAlign's spacer for the control: it ends two columns in from the right edge - on a border, the corner and one
    // line show past it ("[═][║]─┐")
    private static final int FROM_THE_RIGHT = 1;

    private final Component<?> first;
    private final Component<?> second;
    private Placement placement = Placement.BELOW;
    private boolean secondOnlyOffered = false;
    private boolean secondShown = true;
    private boolean bordered = false;
    private boolean collapse = false;                    // panes' borders shared with the divider: collapseBorders
    private String title;
    private final Map<Placement, Integer> shares = new EnumMap<Placement, Integer>(Placement.class);   // the first pane's %, as left
    private int share = 50;
    private boolean sized = false;                       // given a size: until then the splitter's proportions are its panes', not a divider's
    private Component<?> arrangement;
    // the control is placed against the arrangement: at the top right of what shows
    private final Rectangle.Delegate anchor = new Rectangle.Delegate(Rectangle.ZERO);   // the arrangement, once made
    private final List<Consumer<Placement>> flipWatchers = new ArrayList<Consumer<Placement>>();

    private List<FormInput<Placement>> controls;         // in the order preferred
    private boolean controlsGiven = false;               // by the caller (control(..)); else the default, made again as needed
    private Component<?> shownControl;                    // the one at the top right now; null: none

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
        this.controls = defaultControls();
        rebuild();
    }

    private List<FormInput<Placement>> defaultControls() {
        return Arrays.asList(radio(), chips());
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

    /**
     * true: bordered panes share their edges with the divider, and with its border if it has one - one line between
     * them, the grip on it (see Composite.collapseBorders). False (the default): each pane's box whole.
     */
    public FlipSplitter collapseBorders(boolean collapse) {
        if (this.collapse != collapse) {
            this.collapse = collapse;
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

    /** true: a third choice, the second pane alone. Before control(..): the controls show the choices there are when made. */
    public FlipSplitter offerSecondOnly(boolean offered) {
        if (controlsGiven && offered != secondOnlyOffered) {
            throw new IllegalStateException("FlipSplitter: offerSecondOnly(" + offered + ") after control(..) - its controls "
                                            + "show the choices there were when they were made; call it first");
        }
        this.secondOnlyOffered = offered;
        if (!offered && Placement.SECOND_ONLY.equals(placement)) {
            this.placement = Placement.BELOW;
        }
        this.controls = defaultControls();
        rebuild();
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

    // ---- the control ---------------------------------------------------------------------------------------------

    /** radio buttons, side by side: (•) Below  ( ) Right (words(..)); a RadioGroup */
    public RadioGroup<Placement> radio() {
        return radio(FlipSplitter::words);
    }

    /** radio buttons with text of your own for each choice */
    public RadioGroup<Placement> radio(Function<Placement, String> text) {
        RadioGroup<Placement> radio = RadioGroup.horizontal(choices(text));
        radio.setName("flipRadio");
        return radio;
    }

    /** chips in the button colour: [═][║] (pictures(..)); a ChipGroup */
    public ChipGroup<Placement> chips() {
        return chips(FlipSplitter::pictures);
    }

    /** chips with text of your own for each choice: a word, or a few characters - a little picture */
    public ChipGroup<Placement> chips(Function<Placement, String> text) {
        ChipGroup<Placement> chips = ChipGroup.of(choices(text));
        chips.setName("flipChips");
        return chips;
    }

    @SuppressWarnings("unchecked")
    private DisplayableValue<Placement>[] choices(Function<Placement, String> text) {
        List<DisplayableValue<Placement>> choices = new ArrayList<DisplayableValue<Placement>>();
        for (Placement p : offered()) {
            choices.add(new SimpleDisplayValue<Placement>(text.apply(p), p));
        }
        return choices.toArray(new DisplayableValue[0]);
    }

    /**
     * The control at its top right: control, or the first of these that fits (at most half the splitter's width) - none
     * fits, the last. Each a component that is a FormInput of Placement: radio(), chips(), or your own.
     */
    @SafeVarargs
    public final FlipSplitter control(FormInput<Placement> control, FormInput<Placement>... fallbacks) {
        List<FormInput<Placement>> given = new ArrayList<FormInput<Placement>>();
        given.add(control);
        given.addAll(Arrays.asList(fallbacks));
        for (FormInput<Placement> c : given) {
            if (!(c instanceof Component)) {
                throw new IllegalArgumentException("FlipSplitter.control: " + c + " is a FormInput but not a component to show");
            }
        }
        this.controls = given;
        this.controlsGiven = true;
        showControl();
        return this;
    }

    /** the control showing at its top right now (null: none - the second pane put away) */
    public Component<?> getControl() {
        return shownControl;
    }

    @Override
    public void resize(Dimension d) {
        sized = d.getHeight() > 0 && d.getWidth() > 0;
        super.resize(d);
        showControl();
    }

    // the control that fits, at the top right, telling the placement; none with one pane
    private void showControl() {
        Component<?> fits = null;
        if (secondShown) {
            int room = getWidth() / 2;
            for (FormInput<Placement> c : controls) {
                fits = (Component<?>) c;
                if (fits.getWidth() <= room) {
                    break;
                }
            }
        }
        Composite area = getWrappedComponent();
        if (fits != shownControl) {
            if (shownControl != null) {
                area.remove(shownControl);
            }
            shownControl = fits;
            if (fits != null) {
                FormInput<Placement> input = asInput(fits);
                input.setValue(placement);
                if (!listenedTo.contains(input)) {
                    listenedTo.add(input);
                    input.onChange((te) -> {
                        if (input.getValue() != null) {
                            userPicks(input.getValue());
                        }
                    });
                }
                area.add(PositionRelativeFactory.fromAlign(VAlign.TOP, 0, Align.RIGHT, FROM_THE_RIGHT, anchor, fits), fits);
            }
        }
        if (shownControl != null) {
            asInput(shownControl).setValue(placement);
            area.keepOnTop(shownControl);                           // over the arrangement, whatever focus raises in it
        }
    }

    private final List<FormInput<Placement>> listenedTo = new ArrayList<FormInput<Placement>>();

    @SuppressWarnings("unchecked")
    private FormInput<Placement> asInput(Component<?> control) {
        return (FormInput<Placement>) control;
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
        area.applyResizeRules();
        showControl();
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
                split.collapseBorders(collapse);
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
                split.collapseBorders(collapse);
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
        box.collapseBorders(collapse);
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

}
