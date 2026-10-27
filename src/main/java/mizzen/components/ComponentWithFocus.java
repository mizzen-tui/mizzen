package mizzen.components;

import java.util.function.Consumer;

import mizzen.components.composite.Composite;
import mizzen.components.input.ParentFocusBehaviour;
import mizzen.controlcodes.color.Color;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.AsKeyboardHandler;
import mizzen.events.keyboard.KeyboardHandler;
import mizzen.geom.Dimension;

/**
 * The base for a component made of other components that take focus - a button bar, a form, a
 * panel of controls. It holds the panel that lays them out (P: a Composite, HorizontalPanel ...) and
 * their focus: by default each of them is a TAB stop, nothing to set. To change how focus moves,
 * use focus() - for example a panel of arrow buttons that is one TAB stop:
 * <pre>
 *     public class NavButtonsPanel extends ComponentWithFocus&lt;Composite, NavButtonsPanel&gt; {
 *         public NavButtonsPanel(..) {
 *             super(color, new Composite(color));
 *             getPanel().add(..);                         // the buttons
 *             focus().singleTabStop()
 *                    .arrowsMoveInside(ArrowKeys.UP_DOWN)
 *                    .enterAt(Entry.FIRST_OR_LAST);
 *         }
 *     }
 * </pre>
 * Border, title, colours and hilite are the panel's. (A single TAB stop - a button, a check box -
 * is not built on this: it holds a LeafFocusBehaviour. A layout with no focus of its own is just a
 * Composite.)
 * <p/>
 * T is the component's own class, so setters chain: new MyPanel().setBorder(true).setTitle("..").
 */
public abstract class ComponentWithFocus<P extends AbstractComponent<?>, T extends ComponentWithFocus<P, T>>
                     extends AbstractComponentWrappper<P>
                     implements ToFocusable, AsKeyboardHandler, HasTitleAndBorder<T> {

    private final ParentFocusBehaviour focus = ParentFocusBehaviour.of(this);

    /** panel: holds its components; it must have a border and title (HasTitleAndBorder) - a Composite does */
    protected ComponentWithFocus(Color color, P panel) {
        super(color, panel);
        if (!(panel instanceof HasTitleAndBorder)) {
            throw new IllegalArgumentException(getClass().getSimpleName() + ": its panel (" + panel.getClass().getSimpleName()
                                               + ") has no border and title (HasTitleAndBorder) - use a Composite");
        }
    }

    private HasTitleAndBorder<?> look() {
        return (HasTitleAndBorder<?>) getPanel();
    }

    /** how focus moves in this component - see ParentFocusBehaviour */
    public ParentFocusBehaviour focus() {
        return focus;
    }

    /** the panel holding its components */
    protected P getPanel() {
        return getWrappedComponent();
    }

    @SuppressWarnings("unchecked")
    private T self() {
        return (T) this;
    }

    @Override
    public final Focusable asFocusable() {
        return focus.asFocusable();
    }

    @Override
    public final KeyboardHandler asKeyboardHandler() {
        return focus.asKeyboardHandler();
    }

    // ---- the panel's look ---------------------------------------------------------------------

    @Override
    public boolean hasBorder() {
        return look().hasBorder();
    }

    @Override
    public T setBorder(boolean hasBorder) {
        look().setBorder(hasBorder);
        return self();
    }

    @Override
    public T setBorderColor(Color borderColors) {
        look().setBorderColor(borderColors);
        return self();
    }

    @Override
    public T setTitle(Color titleColors, String title) {
        look().setTitle(titleColors, title);
        return self();
    }

    @Override
    public T setTitle(String title) {
        look().setTitle(title);
        return self();
    }

    @Override
    public String getTitle() {
        return look().getTitle();
    }

    @Override
    public void hilite() {
        look().hilite();
    }

    @Override
    public void unHilite() {
        look().unHilite();
    }

    @Override
    public T setEmptyChar(char emptyChar) {
        super.setEmptyChar(emptyChar);
        if (getPanel() != null) {          // null while AbstractComponent's constructor runs
            getPanel().setEmptyChar(emptyChar);
        }
        return self();
    }

    @Override
    public T setEmptyColor(Color emptyColor) {
        super.setEmptyColor(emptyColor);
        if (getPanel() != null) {          // null while AbstractComponent's constructor runs
            getPanel().setEmptyColor(emptyColor);
        }
        return self();
    }

    @Override
    public T setName(String name) {
        super.setName(name);
        return self();
    }

    /** when the panel is resized (only for a Composite panel) */
    public T setOnResize(Consumer<Dimension> onResize) {
        if (!(getPanel() instanceof Composite)) {
            throw new IllegalStateException(HasName.UTIL.getName(this) + ": setOnResize needs a Composite panel");
        }
        ((Composite) getPanel()).setOnResize(onResize);
        return self();
    }
}
