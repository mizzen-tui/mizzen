package dtb.terminalIO.components.tab;

import java.util.Collection;
import java.util.Collections;

import dtb.terminalIO.components.AbstractComponent;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.CanResize;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.GetComponentAtOffset;
import dtb.terminalIO.components.Hiliter;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Align;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.VAlign;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionRelativeFactory;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

public class TabPanel extends AbstractComponent<TabPanel> 
                      implements CanResize, FocusManager, ParentContainer, GetComponentAtOffset, Hiliter {

    private DefaultFocusManager focusManager;

    private Composite mainPanel;
    private BorderedComponent workAreaBorder;
    private Composite workArea;
    private Button previousButton;
    private Button nextButton;
    private ButtonBar navControls;

    Color textActiveTabColor = Colors.DEFAULT_COLOR.bold();

    private ChainedEventHandler chainedEventHandler;

    public TabPanel(Color color, Dimension bounds) {
        super(color);
        mainPanel = new Composite(color).setBounds(bounds);
        workArea = new Composite().setBounds(new Dimension.Simple(bounds.getHeight()-4, bounds.getWidth()-2));
        workAreaBorder = new BorderedComponent(color, workArea);
        this.mainPanel.add(new Position.Simple(2, 0), workAreaBorder);
        this.mainPanel.setParentContainer(this);
        
    }

    public TabPanel(Color color, Color textActiveTabColor, Dimension bounds) {
        this(color, bounds);
        this.textActiveTabColor = textActiveTabColor;
    }
    
    public TabPanel setBorder(boolean hasBorder) {
        this.mainPanel.setBorder(hasBorder);
        this.markAsDirty();
        return this;
    }

    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        if (child == this.mainPanel) {
            return Position.ZERO;
        }
        return null;
    }
    
    @Override
    public void childResized(Component<?> child) {
        this.markAsDirty();
    }

    public void resize(Dimension size) {
        this.mainPanel.resize(size);
        this.workAreaBorder.resize(new Dimension.Simple(size.getHeight()-2, size.getWidth()));
        this.workArea.resize(new Dimension.Simple(workAreaBorder.getHeight() - 2, workAreaBorder.getWidth() - 2));

        TabAction ta = getActiveTabAction();
        if (ta != null) {
            ta.resize(getTabContext());
        }

    }

    private void buildFocusManager() {
        
        KeyStroke previousTabHotKey = Keys.SHIFT_TAB_KEYSTROKE;
        KeyStroke nextTabHotKey = Keys.TAB_KEYSTROKE;
        
        chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy();
        ConditionalEventHandler chainedEventHandlerAsConditionalEventHandler = chainedEventHandler.asConditionalEventHandler();
        focusManager = new DefaultFocusManager(previousTabHotKey, nextTabHotKey) {
            
            public boolean setPreviousAsCurrentFocus(EventBus eventBus) {
                boolean result = super.setPreviousAsCurrentFocus(eventBus);
                if (result && navControls != null) {
                    navControls.setChildAsCurrentFocus(previousButton, eventBus);
                }
                return result;
            }

            public boolean setNextAsCurrentFocus(EventBus eventBus) {
                boolean result = super.setNextAsCurrentFocus(eventBus);
                if (result && navControls != null) {
                    navControls.setChildAsCurrentFocus(nextButton, eventBus);
                }
                return result;
            }
            
            
            @Override
            public void setChildAsCurrentFocus(Focusable focusable, EventBus eventBus) {
                // before?
                
                Focusable currentChildFocus = this.getCurrentChildFocus();
                if (currentChildFocus != null && currentChildFocus instanceof TabAction) {
                    TabAction curTabAction = (TabAction) currentChildFocus;
                    curTabAction.deactivate(eventBus, getTabContext());
                    TabPanel.this.workArea.remove(curTabAction.getComponent(getTabContext()));
                }
                /**/
                
                if (focusable != null && focusable instanceof TabAction) {
                    TabAction newTabAction =  (TabAction) focusable;
                    newTabAction.activate(eventBus, getTabContext()); 
                    Component<?> newActiveComponent = newTabAction.getComponent(getTabContext());
                    if (newActiveComponent == null) {
                        throw new NullPointerException("tabAction.getComponent() returned null - cannot render!");
                    }
                    workArea.add(newActiveComponent);
                }
                
                // some of this might be redundant - because of the activity above
                super.setChildAsCurrentFocus(focusable, eventBus);
            }
            
            @Override
            public EventHandler canHandle(TerminalEvent<?> te) {
                if (super.canHandle(te) != null) {
                    return this;
                }
                if (chainedEventHandlerAsConditionalEventHandler.canHandle(te) != null) {
                    return this;
                }
                return null;
            }
            
            @Override
            public void handle(TerminalEvent<?> te) {
                if (super.canHandle(te)  == this) {
                    super.handle(te);
                    return;
                }
                chainedEventHandlerAsConditionalEventHandler.handle(te);
            }
        };
        
        DefaultFocusManager.setDefaultNameFromParentObject(focusManager, this);
        
        if (navControls == null) {
            navControls = new ButtonBar( workAreaBorder.getBorderColor() ).setHorizontalSpacer(2);
            
            previousButton = new Button("<-");
            previousButton.setHotKey(previousTabHotKey);
            previousButton.setDisplayHotKey(true);
            previousButton.onChange((te)-> focusManager.setPreviousAsCurrentFocus( te.getEventBus() ));
            navControls.addComponent(previousButton);
            
            nextButton = new Button("->");
            nextButton.setHotKey(nextTabHotKey);
            nextButton.setDisplayHotKey(true);
            nextButton.onChange((te)-> focusManager.setNextAsCurrentFocus( te.getEventBus() ));
            navControls.addComponent(nextButton);
            
            // TODO:  change navButtons to a button bar
            //  because not attached to a focusManager - will I have problems processing a mouse?  .. I think so
            chainedEventHandler.appendEventHandler(navControls);
            
            Position navControlsPosition = PositionRelativeFactory.fromAlign(VAlign.BOTTOM, Align.RIGHT,  this.mainPanel, this.navControls);
            mainPanel.add(navControlsPosition, navControls);
        }
        

    }

    private EventHandlerContainer getEventHandlerContainer() {
        if (chainedEventHandler == null) {
            buildFocusManager();
        }
        return this.chainedEventHandler;
    }
    
    private DefaultFocusManager getFocusManager() {
        if (focusManager == null) {
            buildFocusManager();
        }
        return focusManager;
    }

    private void appendEventHandler(ConditionalEventHandler eventHandler) {
        getEventHandlerContainer().appendEventHandler(eventHandler);
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        if (focusManager == null) {
            return;
        }
        focusManager.handle(te);
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if (focusManager == null) {
            return null;
        }
        if (focusManager.canHandle(te) == null) {
            return null;
        }
        return this;
    }

    TabAction findTabActionByHotKey(KeyStroke hotKey) {
        for (Focusable focusable : getChildFocusables()) {
            if (focusable instanceof TabAction) {
                KeyStroke tabActionHotKey = ((TabAction) focusable).getHotKey();
                if ( hotKey.equals(tabActionHotKey)) {
                    return (TabAction) focusable;
                }
            }
        }
        return null;
    }

    TabAction findTabAction(TabAction ta) {
        return findTabActionByHotKey(ta.getHotKey());
    }

    public void selectAsActive(TerminalEvent<?> te, KeyStroke hotKey) {
        TabAction tabAction = findTabActionByHotKey(hotKey);
        if (tabAction != null) {
            getFocusManager().setChildAsCurrentFocus(tabAction, te.getEventBus());
        }
    }
    
    
    TabAction getActiveTabAction() {
        Focusable focusable = getFocusManager().getCurrentChildFocus();
        if (focusable != null && focusable instanceof TabAction) {
            return (TabAction) focusable;
        }
        return null;
    }

    int offsetRow = 0;
    int offsetCol = 1;

    boolean helpFooterBuilt = true;

    public void addTab(final TabAction tabAction) {

        KeyStroke hotKey = tabAction.getHotKey();

        if (tabAction.getHotKey() == null)
            throw new IllegalArgumentException("TabAction must specify a hotKey");

        if (findTabActionByHotKey(hotKey) != null)
            throw new IllegalArgumentException("TabAction must specify a unique hotKey");

        if (!tabAction.getTabComponent().getTabText().contains(hotKey.getName())) {
            tabAction.getTabComponent().setTabText(hotKey.getName() + " - " + tabAction.getTabComponent().getTabText());
        }

        if (this.textActiveTabColor != null && !this.textActiveTabColor.equals(tabAction.getTextActiveTabColor())) {
            // hmm... I wonder if this will cause grief down the road ..
            tabAction.setTextActiveTabColor(this.textActiveTabColor);
        }

        getFocusManager().addFocusable(tabAction);
        
        // why am I not using workArea ( is it needed? )
        this.mainPanel.add(offsetRow, offsetCol, tabAction.getTabComponent());

        offsetCol += tabAction.getTabComponent().getWidth();

        // appendEventHandler(new KeyInputEventHandler(tabAction.getHotKey(), (te) -> selectAsActive(te, tabAction)));
        appendEventHandler(new KeyInputEventHandler(tabAction.getHotKey(), (te) -> getFocusManager().setChildAsCurrentFocus(tabAction, te.getEventBus())));

    }

    TabContext getTabContext() {
        return new TabContext() {

            @Override
            public Color getColor() {
                return TabPanel.this.getColor();
            }

            @Override
            public Rectangle getBounds() {
                return TabPanel.this.workArea;
            }

        };
    }

    public static interface TabContext {

        Color getColor();

        Rectangle getBounds();

    }

    @Override
    public boolean hasFocus() {
        return focusManager.hasFocus();
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        focusManager.setFocus(focus, eventBus);
        if (focus) {
            this.hilite();
        } else {
            this.unHilite();
        }
    }

    @Override
    public int getCol() {
        return this.mainPanel.getCol();
    }

    @Override
    public int getRow() {
        return this.mainPanel.getRow();
    }

    @Override
    public int getWidth() {
        return this.mainPanel.getWidth();
    }

    @Override
    public int getHeight() {
        return this.mainPanel.getHeight();
    }

    @Override
    public void moveTo(int row, int col) {
        this.mainPanel.moveTo(row, col);
    }

    @Override
    public void setChildAsCurrentFocus(Focusable focusableChild, EventBus eventBus) {
        getFocusManager().setChildAsCurrentFocus(focusableChild, eventBus);
    }

    @Override
    public Focusable getCurrentChildFocus() {
        return getFocusManager().getCurrentChildFocus();
    }

    @Override
    public Collection<Focusable> getChildFocusables() {
        return getFocusManager().getChildFocusables();
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.merge(this.mainPanel.getSegmentsBuffer());
    }

    @Override
    protected Rectangle getRectangleForClearAndFill() {
        return mainPanel;
    }
    
    @Override
    public Collection<? extends Component<?>> getChildren() {
        return Collections.singleton(this.mainPanel);
    }

    @Override
    public Component<?> getComponentAtOffset(Position offset) {
        return this.mainPanel.getComponentAtOffset(offset);
    }

    public Hiliter getHiliter() {
        return hiliter;
    }
    
    public void setHiliter(Hiliter hiliter) {
        this.hiliter = hiliter;
    }
    
    private Hiliter hiliter = new Hiliter() {

        @Override
        public void hilite() {
            TabPanel.this.workAreaBorder.hilite();
            if (TabPanel.this.getActiveTabAction() != null) {
                TabAction active = TabPanel.this.getActiveTabAction();
                active.getTabComponent().hilite();
            }
            TabPanel.this.markAsDirty();
        }

        @Override
        public void unHilite() {
            TabPanel.this.workAreaBorder.unHilite();
            if (TabPanel.this.getActiveTabAction() != null) {
                TabAction active = TabPanel.this.getActiveTabAction();
                active.getTabComponent().unHilite();
            }
            TabPanel.this.markAsDirty();
        }

    };

    @Override
    public void hilite() {
        hiliter.hilite();
    }

    @Override
    public void unHilite() {
        hiliter.unHilite();
    }

}
