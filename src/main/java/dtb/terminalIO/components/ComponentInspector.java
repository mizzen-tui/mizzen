package dtb.terminalIO.components;

import java.util.Collection;
import java.util.Collections;

import dtb.terminalIO.components.input.tree.TreeView;
import dtb.terminalIO.components.input.tree.TreeViewModel.HasChildrenTreeViewModel;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

public class ComponentInspector extends AbstractComponent<ComponentInspector>
        implements CanResize, ParentContainer, FocusManager {

    static enum VIEW_TYPE {
        COMPONENT_VIEW, FOCUSABLE_VIEW
    }

    ScrollableComponent scrollingArea;
    Component<?> componentToInspect;
    FocusManager focusManagerToInspect;

    VIEW_TYPE currentViewType = VIEW_TYPE.COMPONENT_VIEW;

    public ComponentInspector(Color color, Dimension d, Component<?> componentToInspect,
            FocusManager focusManagerToInspect) {
        super(color);

        this.componentToInspect = componentToInspect;
        this.focusManagerToInspect = focusManagerToInspect;
        this.currentViewType = VIEW_TYPE.COMPONENT_VIEW;
        setView(d);
    }

    private void toggleViewType() {
        currentViewType = currentViewType == VIEW_TYPE.COMPONENT_VIEW ? VIEW_TYPE.FOCUSABLE_VIEW
                : VIEW_TYPE.COMPONENT_VIEW;
    }

    private void setView(Dimension d) {
        if (currentViewType == VIEW_TYPE.COMPONENT_VIEW) {
            TreeView<Component<?>> tvComponents = new TreeView<Component<?>>(Colors.IBlu, new Dimension.Simple(90, 60),
                    (node) -> node.getClass().getSimpleName());
            tvComponents.setModel(new HasChildrenTreeViewModel<Component<?>>(componentToInspect));

            scrollingArea = new ScrollableComponent(Colors.BIGre, d, new BorderedComponent(tvComponents));
            scrollingArea.setParentContainer(this);
        } else {

            TreeView<Focusable> tvFocusable = new TreeView<Focusable>(Colors.IBlu, new Dimension.Simple(90, 60),
                    (node) -> node.hasFocus()
                            ? new FGBGColors(Colors.BIBlu, Colors.On_Yel).asString() + node.getClass().getSimpleName()
                                    + ControlCode.RCol.asString()
                            : node.getClass().getName());

            tvFocusable.setModel(new HasChildrenTreeViewModel<Focusable>(focusManagerToInspect,
                    new FocusManagerHasChildrenAdapter()));

            scrollingArea = new ScrollableComponent(Colors.BIGre, d, new BorderedComponent(tvFocusable));
            scrollingArea.setParentContainer(this);
        }

        scrollingArea.appendEventHandler(toggleViewEventHandler());

        scrollingArea.appendEventHandler(refreshEventHandler());

        this.markAsDirty();

    }

    public KeyInputEventHandler refreshEventHandler() {
        return new KeyInputEventHandler(Keys.CTRL_R_KEYSTROKE, (te) -> {
            ComponentInspector.this.scrollingArea.getComponentBeingViewed().markAsDirty();
            te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(this));
        });
    }

    public KeyInputEventHandler toggleViewEventHandler() {
        return new KeyInputEventHandler(Keys.CTRL_T_KEYSTROKE, (te) -> {
            Dimension currentSize = ComponentInspector.this.scrollingArea;
            toggleViewType();
            setView(currentSize);
            te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(this));
        });
    }

    @Override
    public Collection<? extends Component<?>> getChildren() {
        return Collections.singletonList(this.scrollingArea);
    }

    @Override
    public boolean hasFocus() {
        return this.scrollingArea.hasFocus();
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        this.scrollingArea.setFocus(focus, eventBus);
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if (this.scrollingArea.canHandle(te) == null) {
            return null;
        }
        return this;
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        this.scrollingArea.handle(te);

    }

    @Override
    public int getCol() {
        return this.scrollingArea.getCol();
    }

    @Override
    public int getRow() {
        return this.scrollingArea.getRow();
    }

    @Override
    public int getWidth() {
        return this.scrollingArea.getWidth();
    }

    @Override
    public int getHeight() {
        return this.scrollingArea.getHeight();
    }

    @Override
    public void moveTo(int row, int col) {
        this.scrollingArea.moveTo(row, col);
    }

    @Override
    public void setChildAsCurrentFocus(Focusable focusableChild, EventBus eventBus) {
        if (focusableChild != this.scrollingArea) {
            throw new IllegalStateException("expected scrollingArea as child!");
        }
        // dont need to do anything as scrollingArea is always current focusable
    }

    @Override
    public Focusable getCurrentChildFocus() {
        return this.scrollingArea;
    }

    @Override
    public Collection<Focusable> getChildFocusables() {
        return Collections.singleton(this.scrollingArea);
    }

    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        if (child == scrollingArea) {
            return Position.ZERO;
        }
        return null;
    }

    @Override
    public void childResized(Component<?> child) {
        this.markAsDirty();
    }

    @Override
    public void resize(Dimension d) {
        this.scrollingArea.resize(d);
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.merge(this.scrollingArea.getSegmentsBuffer());
    }

    @Override
    protected Rectangle getRectangleForClearAndFill() {
        return this.scrollingArea.getRectangleForClearAndFill();
    }

}
