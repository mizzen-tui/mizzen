package dtb.terminalIO.components;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.CheckBoxInput;
import dtb.terminalIO.components.input.HasValue;
import dtb.terminalIO.components.input.tree.TreeView;
import dtb.terminalIO.components.input.tree.TreeViewModel.CachingTreeviewModel;
import dtb.terminalIO.components.input.tree.TreeViewModel.FilteredTreeViewModel;
import dtb.terminalIO.components.input.tree.TreeViewModel.HasChildrenTreeViewModel;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.components.scroll.SearchFunctionForScrollableComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.TimedRequestedController;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.OnIdleEventEventHandler.RepeatingInterval;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.FocusManager.FocusManagerHasChildrenAdapter;
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
        implements CanResize, ParentContainer, GetComponentAtOffset {

    static enum VIEW_TYPE {
        COMPONENT_VIEW, FOCUSABLE_VIEW
    }

    ScrollableComponent scrollingArea;
    Component<?> componentToInspect;
    Supplier<FocusManager> focusManagerToInspect;

    VIEW_TYPE currentViewType = VIEW_TYPE.COMPONENT_VIEW;

    Focusable currentFocus = null;

    public ComponentInspector(Color color, 
                              Dimension d, 
                              Component<?> componentToInspect,
                              FocusManager focusManagerToInspect) {
        super(color);

        this.componentToInspect = componentToInspect;
        this.focusManagerToInspect = ()-> focusManagerToInspect;
        this.currentViewType = VIEW_TYPE.COMPONENT_VIEW;
        __setView(d);
    }


    public void setFocusManagerToInspect(FocusManager focusManagerToInspect) {
        this.focusManagerToInspect = ()->focusManagerToInspect;
    }

    public void setFocusManagerToInspect(Supplier<FocusManager> focusManagerToInspect) {
        this.focusManagerToInspect = focusManagerToInspect;
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
            tvComponents.setModel(new FilteredTreeViewModel<Component<?>>((c)->c != ComponentInspector.this, new HasChildrenTreeViewModel<Component<?>>(componentToInspect)));
            tvComponents.expandAll();     // the inspector shows the whole tree

            if (scrollingArea != null) {
                this.scrollingArea.setParentContainer(null);
                this.scrollingArea.asFocusable().setParentFocusManager(null);
            }

            scrollingArea = new ScrollableComponent(Colors.BIGre, d, tvComponents);
            SearchFunctionForScrollableComponent.addFinderToScrollable(scrollingArea.asSearchableView());
            scrollingArea.setParentContainer(this);
            scrollingArea.setTitle("Components : " + currentViewType);
            
        } else {

            TreeView<Focusable> tvFocusable = new TreeView<Focusable>(Colors.IBlu, new Dimension.Simple(90, 60),
                    (node) -> node.hasFocus()
                            ? new Colors.FGBGColors(node==currentFocus?Colors.BGre    : Colors.BBlu, 
                                                    node==currentFocus?Colors.On_IYel : Colors.On_Yel).asString() + nodeName(node)
                                    + Colors.RCol.asString()
                            : nodeName(node)) {

                public void draw(Color color, IScreenLineSegmentsBuffer segments) {
                    currentFocus = FocusManager.UTIL.findCurrentFocus(focusManagerToInspect.get());
                    super.draw(color, segments);
                }
            };

            tvFocusable.setModel(new CachingTreeviewModel<Focusable>(new HasChildrenTreeViewModel<Focusable>(focusManagerToInspect.get(),
                                                                         new FocusManagerHasChildrenAdapter())));
            tvFocusable.expandAll();      // the inspector shows the whole tree

            if (scrollingArea != null) {
                this.scrollingArea.setParentContainer(null);
                this.scrollingArea.asFocusable().setParentFocusManager(null);
            }
            scrollingArea = new ScrollableComponent(Colors.BIGre, d, tvFocusable);
            SearchFunctionForScrollableComponent.addFinderToScrollable(scrollingArea.asSearchableView());
            scrollingArea.setParentContainer(this);
            scrollingArea.setTitle("Components : " + currentViewType);
        }
        
        //scrollingArea.appendEventHandler(toggleViewEventHandler());

        //scrollingArea.appendEventHandler(refreshEventHandler());

        this.markAsDirty();

    }

    public void setFillOnClear(boolean fillOnClear) {
        super.setFillOnClear(fillOnClear);
        if (this.scrollingArea != null) {
            this.scrollingArea.setFillOnClear(fillOnClear);
        }
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

    TimedRequestedController delayedSetView = null;

    private void requestSetView(EventBus eventBus) {
        requestSetView(eventBus, true);
    }
    private void requestSetView(EventBus eventBus, boolean updateOnViewTypeChanged) {
        if (delayedSetView == null) {
            delayedSetView = new TimedRequestedController(()->{
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
        if (updateOnViewTypeChanged && currentViewType == VIEW_TYPE.COMPONENT_VIEW) {
            return;
        }
        delayedSetView.onRequest(eventBus);
    }
    public void refreshView(EventBus eventBus) {
        requestSetView(eventBus, false);
    }


    private KeyInputEventHandler __toggleViewEventHandler;

    public KeyInputEventHandler toggleViewEventHandler() {
        if (__toggleViewEventHandler != null) {
            return __toggleViewEventHandler;
        }
        __toggleViewEventHandler = new KeyInputEventHandler(Keys.CTRL_T_KEYSTROKE, (te) -> {
            toggleViewType();
            scrollingArea.setTitle("Components : " + currentViewType);
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
            Focusable curFocus = FocusManager.UTIL.findCurrentFocus(focusManagerToInspect.get());
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

    public static class InspectorButton extends AbstractComponentWrappper<Button> {

        WindowFrame inspector;
        TUIApp      tApp;

        public InspectorButton(Color color, TUIApp tApp) {
            super(color, new Button(color, BlockChars.DEFAULT.getBOTTOM_LEFT()+"."));
            this.tApp = tApp;
            this.getWrappedComponent().onChange((te)-> handleClicked(te) );
        }

        public InspectorButton setBorder(boolean hasBorder) {
            this.getWrappedComponent().setBorder(hasBorder);
            return this;
        }

        protected void handleClicked(TerminalEvent<?> te) {
            if (inspector == null) {
                inspector = resizableInspector(tApp);
                return;
            }
        }
    }

    public static final WindowFrame resizableInspector(TUIApp tApp) {

        
        ComponentInspector inspector = new ComponentInspector(Colors.BIGre, 
                                                              new Dimension.Simple(25, 60),
                                                              (Component<?>) tApp.getMainApplicationPanel(), 
                                                              tApp.getApplicationFocusManager());

        WindowFrame wfInspector = new WindowFrame(inspector.getColor(), "Components", inspector);

        tApp.getEventBus().appendEventHandler(inspector.toggleViewEventHandler());
        tApp.getEventBus().appendEventHandler(inspector.refreshEventHandler());
        // every 1/4 second we update ComponentInspector with current component hierarchy and focus data
        tApp.getEventBus().appendIdleEventHandler(new RepeatingInterval(2000, (te) -> {
            inspector.refreshEventHandler().handle(te);
        }));

        //mainFocusManager.addComponentAsFocusables(wfInspector);
        Position inspectorPos = ((Component<?>)tApp.getMainApplicationPanel())
                                                    .getPreferredRectangleForLayout()
                                                    .centered(wfInspector);
        tApp.getMainApplicationPanel().add(inspectorPos, wfInspector);

        return wfInspector;
    }

}
