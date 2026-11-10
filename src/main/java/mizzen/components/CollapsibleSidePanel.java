
package mizzen.components;

import mizzen.components.composite.Composite;
import mizzen.components.input.Button;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.TerminalEvent;
import mizzen.geom.Align;
import mizzen.geom.CanResize;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.VAlign;
import mizzen.geom.position.PositionRelativeFactory;
import mizzen.util.RecursionGuard;

/**
 * A horizontally collapsible panel.
 *
 * Expanded: displays the supplied content.
 * Collapsed: displays a narrow placeholder and an expand button.
 *
 * The active child is resized when the parent explicitly resizes
 * this component, provided the child implements CanResize.
 *
 * Switching between expanded and collapsed uses the active child's
 * natural dimensions.
 */
public class CollapsibleSidePanel
        extends AbstractComponentWithContainer<Composite> {

    public enum State {
        EXPANDED,
        COLLAPSED
    }

    private final Component<?> content;
    private final BoundedComponent<?> contentWhenHidden;
    private final Button toggleButton;

    private final RecursionGuard resizeGuard = new RecursionGuard();
    private final RecursionGuard stateGuard = new RecursionGuard();

    private BorderedComponent borderedComponent;
    private Component<?> activeChild;
    private Rectangle.Delegate anchorToActiveChildDelegate = new Rectangle.Delegate(null);
    private State state = State.EXPANDED;

    public CollapsibleSidePanel(Component<?> content) {
        this(null, content);
    }

    public CollapsibleSidePanel(Color color, Component<?> content) {
        super(color, new Composite());

        if (content == null) {
            throw new NullPointerException("content");
        }

        this.content = content;

        toggleButton = new Button("<").setName("tggl-btn");

        Rectangle initialSize = new Rectangle.Simple(content.getPreferredRectangleForLayout());

        contentWhenHidden = new MultiCharComponent(
                Colors.DEFAULT_COLOR,
                '░',
                new Rectangle.Simple(
                        Math.max(1, initialSize.getHeight() - 2),
                        toggleButton.getWidth()))
                .setThemeColor(theme -> theme.dimColor())
                .setName("ctn-hidden");

        Composite panel = getWrappedComponent();

        // Install the initial content before positioning the button.
        showChild(content, state);

        panel.add(
                PositionRelativeFactory.fromAlign(
                        VAlign.TOP, 0,
                        Align.RIGHT, 0,
                        anchorToActiveChildDelegate, toggleButton),
                toggleButton);

        panel.keepOnTop(toggleButton);

        toggleButton.onChange(te -> setCollapsed(te, state != State.COLLAPSED));
    }

    public State getState() {
        return state;
    }

    public boolean isCollapsed() {
        return state == State.COLLAPSED;
    }

    public Button getToggleButton() {
        return toggleButton;
    }

    public CollapsibleSidePanel collapse() {
        return setCollapsed(null, true);
    }

    public CollapsibleSidePanel expand() {
        return setCollapsed(null, false);
    }

    private CollapsibleSidePanel setCollapsed(TerminalEvent<?> te, boolean collapsed) {
        State next = collapsed ? State.COLLAPSED : State.EXPANDED;

        if (state == next) {
            return this;
        }

        // resizeGuard too: while the child is swapped, the parent's rules resize this panel to its in-between
        // width - e.g. 29 for a moment, on the way from 30 to 5 - and resize() would pass it to the hidden content
        stateGuard.execute(() -> resizeGuard.execute(() -> {
            if (collapsed) {
                showChild(contentWhenHidden, next);
                toggleButton.setButtonText(">");
            } else {
                showChild(content, next);
                toggleButton.setButtonText("<");
            }

            state = next;
            markAsDirty();
        }));

        return this;
    }

    /**
     * Replaces the visible child and sizes this component to the
     * new child's natural dimensions.
     */
    private void showChild(Component<?> child, State next) {
        if (activeChild == child) {
            return;
        }

        Composite panel = getWrappedComponent();

        if (activeChild != null) {
            panel.setOnResizeRule(null); // remove resize rules - crucial todo this first
            panel.remove(activeChild);
        }
        panel.setOnResizeRule(null); // crucial to avoid recursion

        activeChild = bordered(child);
        anchorToActiveChildDelegate.setRectangle(activeChild); // note: anchorToActiveChildDelegate must always delegate
                                                               // to activeChild

        panel.insert(0, activeChild);
        if (next == State.EXPANDED) {
            RecursionGuard childResizeGuard = new RecursionGuard();
            panel.setOnResizeRule((size) -> {
                childResizeGuard.execute(() -> {
                    Dimension newChildSize = new Dimension.Simple(size.getHeight(), size.getWidth());
                    ((CanResize) activeChild).resize(newChildSize);
                });
            });
            // panel.applyResizeRules();
            // super.resize(desiredCollapsedSize);// applyResizeRules();
            super.resize(new Dimension.Simple(activeChild));
        } else { // COLLAPSED
            Dimension desiredCollapsedSize = new Dimension.Simple(panel.getInnerRectangle().getHeight(), 5);
            // borderedComponent.resize(desiredCollapsedSize);
            panel.setOnResizeRule((size) -> {
                Dimension newChildSize = new Dimension.Simple(size.getHeight(), 5);
                ((CanResize) activeChild).resize(newChildSize);
            });
            panel.applyResizeRules();
            //super.resize(desiredCollapsedSize);// applyResizeRules();
        }

        // Internal size change: bypass this class's resize handling.
        // super.resize(new Dimension.Simple(activeChild));

        // TODO hoping this would trigger the parent components to run
        //  their resize rules.
        if (this.getParentContainer() != null) {
            this.getParentContainer().childResized(this);
        }

        markAsDirty();
    }

    /**
     * Uses the child's border when available; otherwise wraps it.
     */
    private Component<?> bordered(Component<?> child) {
        if (child instanceof HasTitleAndBorder) {
            HasTitleAndBorder<?> bordered = (HasTitleAndBorder<?>) child;

            if (!bordered.hasBorder()) {
                bordered.setBorder(true);
            }
            return child;
        }

        if (borderedComponent == null) {
            borderedComponent = new BorderedComponent(child);// .setBorderColor(Colors.IRed);
        } else {
            borderedComponent.setComponent(child);
        }

        return borderedComponent;
    }

    /**
     * Handles an externally requested size.
     *
     * The active child receives the requested dimensions if it
     * supports CanResize and is not already that size.
     */
    @Override
    public void resize(Dimension dimension) {

        if (this.isSameDimension(dimension)) {
            return;
        }
        resizeGuard.execute(() -> {
            int height = dimension.getHeight();

            // Collapsed placeholder: height changes, width stays fixed.
            resizeIfNeeded(contentWhenHidden,
                    new Dimension.Simple(
                            Math.max(1, height - 2),
                            contentWhenHidden.getWidth()));

            if (borderedComponent != null && this.getState() == State.COLLAPSED) {
                resizeIfNeeded(borderedComponent,
                        new Dimension.Simple(
                                Math.max(1, height),
                                contentWhenHidden.getWidth() + 2));
            }

            // Expanded content: never narrower than collapsed representation.
            int minimumWidth = contentWhenHidden.getWidth() + 2;

            if (this.state == State.EXPANDED) {
                resizeIfNeeded(content,
                        new Dimension.Simple(
                                height,
                                Math.max(dimension.getWidth(), minimumWidth)));
            }

            if (this.getParentContainer() != null) {
                this.getParentContainer().childResized(this);
            }

        });
    }

    private void resizeIfNeeded(Component<?> child, Dimension dimension) {
        if (child instanceof CanResize && !sameSize(child, dimension)) {
            ((CanResize) child).resize(dimension);
        }
    }

    private static boolean sameSize(Component<?> component, Dimension d) {
        return component.getWidth() == d.getWidth()
                && component.getHeight() == d.getHeight();
    }
}
