package dtb.terminalIO.components;

import java.util.Collection;
import java.util.Collections;

import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.CheckBoxInput;
import dtb.terminalIO.components.input.HasValue;
import dtb.terminalIO.components.input.tree.TreeView;
import dtb.terminalIO.components.input.tree.TreeViewModel.CachingTreeviewModel;
import dtb.terminalIO.components.input.tree.TreeViewModel.HasChildrenTreeViewModel;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.AbstractTimedRequestedController;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.StringUtils;

public class ComponentInspector extends AbstractComponent<ComponentInspector>
        implements CanResize, ParentContainer, FocusManager, GetComponentAtOffset {

    static enum VIEW_TYPE {
        COMPONENT_VIEW, FOCUSABLE_VIEW
    }

    ScrollableComponent scrollingArea;
    Component<?> componentToInspect;
    FocusManager focusManagerToInspect;

    VIEW_TYPE currentViewType = VIEW_TYPE.COMPONENT_VIEW;

    Focusable currentFocus = null;

    public ComponentInspector(Color color, 
                              Dimension d, 
                              Component<?> componentToInspect,
                              FocusManager focusManagerToInspect) {
        super(color);

        this.componentToInspect = componentToInspect;
        this.focusManagerToInspect = focusManagerToInspect;
        this.currentViewType = VIEW_TYPE.COMPONENT_VIEW;
        __setView(d);
    }

    private void toggleViewType() {
        currentViewType = currentViewType == VIEW_TYPE.COMPONENT_VIEW ? VIEW_TYPE.FOCUSABLE_VIEW
                                                                      : VIEW_TYPE.COMPONENT_VIEW;
    }

    private void __setView(Dimension d) {
        this.clearBeforeDraw();
        if (currentViewType == VIEW_TYPE.COMPONENT_VIEW) {
            TreeView<Component<?>> tvComponents = new TreeView<Component<?>>(Colors.IBlu, new Dimension.Simple(90, 60),
                    (node) -> nodeName(node));
            tvComponents.setModel(new CachingTreeviewModel<Component<?>>(new HasChildrenTreeViewModel<Component<?>>(componentToInspect)));

            if (scrollingArea != null) {
                this.scrollingArea.setParentContainer(null);
            }
            
            scrollingArea = new ScrollableComponent(Colors.BIGre, d, tvComponents);
            scrollingArea.setParentContainer(this);
        } else {

            TreeView<Focusable> tvFocusable = new TreeView<Focusable>(Colors.IBlu, new Dimension.Simple(90, 60),
                    (node) -> node.hasFocus()
                            ? new Colors.FGBGColors(node==currentFocus?Colors.BGre    : Colors.BBlu, 
                                                    node==currentFocus?Colors.On_IYel : Colors.On_Yel).asString() + nodeName(node)
                                    + Colors.RCol.asString()
                            : nodeName(node)) {

                public void draw(Color color, IScreenLineSegmentsBuffer segments) {
                    currentFocus = FocusManager.UTIL.findCurrentFocus(focusManagerToInspect);
                    super.draw(color, segments);
                }
            };

            tvFocusable.setModel(new CachingTreeviewModel<Focusable>(new HasChildrenTreeViewModel<Focusable>(focusManagerToInspect,
                                                                         new FocusManagerHasChildrenAdapter())));

            if (scrollingArea != null) {
                this.scrollingArea.setParentContainer(null);
            }
            scrollingArea = new ScrollableComponent(Colors.BIGre, d, tvFocusable);
            scrollingArea.setParentContainer(this);
        }

        //scrollingArea.appendEventHandler(toggleViewEventHandler());

        //scrollingArea.appendEventHandler(refreshEventHandler());

        this.markAsDirty();

    }

    private String nodeName(Object node) {
        String suffix=null;
        if (node instanceof CheckBoxInput) {
            suffix = null;
        }
        if (node instanceof HasName<?>) {
            suffix = ((HasName<?>)node).getName();
        }
        if (suffix == null && node instanceof HasTitleAndBorder<?>) {
            String title = ((HasTitleAndBorder<?>)node).getTitle();
            if (title != null) {
               suffix = title;
               suffix = StringUtils.truncate(suffix, 10);
            }
        }
        if (suffix == null && node instanceof Button) {
            suffix = ((Button)node).getButtonText();
        }
        if (suffix == null && node instanceof HasValue<?>) {
            Object value = ((HasValue<?>)node).getValue();
            if (value != null) {
               suffix = String.valueOf(value);
               suffix = StringUtils.truncate(suffix, 10);
            }
        }
        if ( suffix != null) {
            suffix = "["+suffix+"]";
        }
        String output = node.getClass().getSimpleName() + (suffix!=null?suffix:"");
        if (output == null || output.trim().length()==0) {
            output = node.toString();
        }
        return output;
    }
    
    

    AbstractTimedRequestedController delayedSetView = null;

    private void requestSetView(EventBus eventBus) {
        if (delayedSetView == null) {
            delayedSetView = new AbstractTimedRequestedController(()->{
                                    Dimension currentSize = ComponentInspector.this.scrollingArea;
                                    __setView(currentSize);
                                    eventBus.raiseEvent(new RequestUpdateCanvasEvent(this));
                                }) {
                                    @Override
                                    protected boolean isRequestDifferentFromPrevious() {
                                        return true;
                                    }
                                };
            delayedSetView.setTimerDelayMillis(500);
            delayedSetView.setMaxStarvationTime(delayedSetView.getTimerDelayMillis()*3);
        }
        if (currentViewType == VIEW_TYPE.COMPONENT_VIEW) {
            return;
        }
        delayedSetView.onRequest(eventBus);
    }


    private KeyInputEventHandler __toggleViewEventHandler;
    
    public KeyInputEventHandler toggleViewEventHandler() {
        if (__toggleViewEventHandler != null) {
            return __toggleViewEventHandler;
        }
        __toggleViewEventHandler = new KeyInputEventHandler(Keys.CTRL_T_KEYSTROKE, (te) -> {
            toggleViewType();
            requestSetView(te.getEventBus());
        });
        return __toggleViewEventHandler;
    }

    private KeyInputEventHandler __refreshEventHandler;
    public KeyInputEventHandler refreshEventHandler() {
        if (__refreshEventHandler != null) {
            return __refreshEventHandler;
        }
        __refreshEventHandler = new KeyInputEventHandler(Keys.CTRL_R_KEYSTROKE, (te) -> {
            Focusable curFocus = FocusManager.UTIL.findCurrentFocus(focusManagerToInspect);
            if (curFocus != currentFocus) {
                currentFocus = curFocus;
                requestSetView(te.getEventBus());
            }
        });
        return __refreshEventHandler;
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
        if (this.getParentContainer() != null) {
            this.getParentContainer().childResized(this);
        }
    }

    @Override
    public void resize(Dimension d) {
        if (d.isSameDimension(this)) {
            return;
        }
        Dimension previousDimension = new Dimension.Simple(this);
        this.scrollingArea.resize(d);
        if (this.getParentContainer()!=null  && ! previousDimension.isSameDimension(this)) {
            this.getParentContainer().childResized(this);
        }
        markAsDirty();
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.merge(this.scrollingArea.getSegmentsBuffer());
    }

    @Override
    protected Rectangle getRectangleForClearAndFill() {
        if (this.scrollingArea == null) {
            return Rectangle.ZERO;
        }
        return this.scrollingArea.getRectangleForClearAndFill();
    }

    @Override
    public Component<?> getComponentAtOffset(Position offset) {
        return this.scrollingArea.getComponentAtOffset(offset);
    }

    public void setTitle(String title) {
        scrollingArea.setTitle(title);
        
    }



}
