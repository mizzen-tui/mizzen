package dtb.terminalIO.components.tab;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.components.BlockChars;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.HasHotKey;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

public class TabPanel extends Composite {

    List<TabAction>              tabActions                = new ArrayList<TabAction>();
    List<ConditionalEventHandler> registeredEventHandlers  = new ArrayList<ConditionalEventHandler>();

    TabAction                    active_tabAction = null;

    TabContext                   tabContext;

    private EventBus             eventBus;

    private BorderedComponent    workAreaBorder;
    private TabWorkAreaComponent workArea;
    
    Color textActiveTabColor = FGBGColors.DEFAULT.bold();
    
    private String prevNextHelp = "";


    class TabWorkAreaComponent extends BoundedComponent<TabWorkAreaComponent> {

        TabWorkAreaComponent(Rectangle r) {
            super(FGBGColors.NO_COLOR_SPECIFIED, r, TabPanel.this);
        }

        TabWorkAreaComponent(dtb.terminalIO.geom.Position position, Dimension dimension) {
            this(new Rectangle.Simple(position, dimension));
        }

        TabWorkAreaComponent(Dimension dimension) {
            this(new Rectangle.Simple(Position.ZERO, dimension));
        }

        @Override
        protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
            if (TabPanel.this.active_tabAction != null) {
                TabPanel.this.active_tabAction.drawOnActive(TabPanel.this.getTabContext());
            }
        }
    }

    public TabPanel(Color color, EventBus eventBus, Dimension bounds) {
        super(color);
        this.eventBus = eventBus;
        workArea = new TabWorkAreaComponent(Position.ZERO, bounds);
        workAreaBorder = new BorderedComponent(color, workArea);
        this.add(new Position.Simple(2,0),workAreaBorder);
    }

    public TabPanel(Color color, Color textActiveTabColor, EventBus eventBus, Dimension bounds) {
        super(color);
        this.eventBus = eventBus;
        workArea = new TabWorkAreaComponent(Position.ZERO, bounds);
        workAreaBorder = new BorderedComponent(color, workArea, this);
        this.add(new Position.Simple(2,0),workAreaBorder);
        this.textActiveTabColor = textActiveTabColor;
    }

    public void resize(Dimension size) {
	super.resize(size);
	workArea.resize(size);
	
	TabAction ta = getActiveTabAction();
	if (ta != null) {
	    ta.resize(getTabContext());
	}
    }
    
    
    TabAction findTabActionByHotKey(KeyStroke hotKey) {
        for (TabAction ta : tabActions) {
            if (hotKey.equals(ta.getHotKey())) {
                return ta;
            }
        }
        return null;
    }

    TabAction findTabAction(TabAction ta) {
        return findTabActionByHotKey(ta.getHotKey());
    }

    TabAction getActiveTabAction() {
        return active_tabAction;
    }

    int offsetRow = 0;
    int offsetCol = 1;
    
    private void addPrevNextEventHandlerIfNeeded() {
        if (registeredEventHandlers.size()>0) {
            // assume added because list not empty
            return;
        }
        
        BlockChars blockChars = workAreaBorder.getBlockChars();
        String prevArrow = " <- ";
        String nextArrow = " -> ";

        
        KeyInputEventHandler hotKeyEventHandler = new KeyInputEventHandler(Keys.SHIFT_TAB_KEYSTROKE, new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                selectPrevAsActive();
            }
        });

        getTabContext().getEventBus().addEventHandler(hotKeyEventHandler);

        this.prevNextHelp = blockChars.getVERTICAL_LEFT()+" " + Keys.SHIFT_TAB_KEYSTROKE.getName() +prevArrow;
        
        hotKeyEventHandler = new KeyInputEventHandler(Keys.TAB_KEYSTROKE, new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                selectNextAsActive();
            }
        });
        getTabContext().getEventBus().addEventHandler(hotKeyEventHandler);
        
        this.prevNextHelp +=  blockChars.getVERTICAL_RIGHT()+ ""
                             +blockChars.getHORIZONTAL_LINE()+""
                             +blockChars.getHORIZONTAL_LINE()+""
                             +blockChars.getHORIZONTAL_LINE()+""
                             +blockChars.getVERTICAL_LEFT()+""
        	             +" "+ Keys.TAB_KEYSTROKE.getName() + nextArrow + blockChars.getVERTICAL_RIGHT();
        
        int prevNextHelpLen = this.prevNextHelp.length();  
        
        TextComponent tcHelp = new TextComponent(workAreaBorder.getColor(),prevNextHelp);
        
        this.add(new Position.Simple(2,0).offsetAdd(workAreaBorder).offsetAdd(workAreaBorder.getBottomMostRow(),workAreaBorder.getRightMostCol()-prevNextHelpLen-2),tcHelp);

    }

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
            tabAction.setTextActiveTabColor( this.textActiveTabColor );
        }
        
        addPrevNextEventHandlerIfNeeded();
        
        tabActions.add(tabAction);

        this.add(offsetRow, offsetCol, tabAction.getTabComponent());

        offsetCol += tabAction.getTabComponent().getWidth();

        KeyInputEventHandler hotKeyEventHandler = new KeyInputEventHandler(tabAction.getHotKey(), new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                // System.err.println("IN tab handler!");
                selectAsActive(tabAction);
            }
        });
        getTabContext().getEventBus().addEventHandler(hotKeyEventHandler);
        
        // eventHandlers so that we can later remove on clean up
        registeredEventHandlers.add(hotKeyEventHandler);
        
        // todo: Do I need a way to remove this eventHandler per tabAction ? -- deal with this
        // if tab becomes disabled, or removed, or tab-panel is destroyed

    }

    public void clearForRedraw() {
        clear(); // doing a clear on tabPanel -- forces a redraw -- I know.. awkward
        workArea.clear();
    }

    TabContext getTabContext() {
        return new TabContext() {

            @Override
            public EventBus getEventBus() {
                return TabPanel.this.eventBus;
            }

            @Override
            public Color getColor() {
                return TabPanel.this.getColor();
            }

            @Override
            public Rectangle getBounds() {
                return TabPanel.this.workArea;
            }

            public IScreenLineSegmentsBuffer getSegmentsBuffer() {
                return new IScreenLineSegmentsBuffer.DelegateWithDirtyFlag(TabPanel.this.workArea.getSegmentsBuffer()) {
                    @Override
                    public void markAsDirty() {
                        TabPanel.this.workArea.markAsDirty();
                    }
                };
            }

            @Override
            public void clearForRedraw() {
                TabPanel.this.clearForRedraw();

            }

            @Override
            public Rectangle getDrawingArea() {
                return TabPanel.this.workArea;
            }

        };
    }

    public void deactivateActive() {
        if (active_tabAction == null) {
            return;
        }

        active_tabAction.deativate(getTabContext());

        active_tabAction = null;

    }

    public void selectAsActive(TabAction tabAction) {
        TabAction ta = findTabAction(tabAction);
        if (ta == null || ta != tabAction) {
            throw new IllegalStateException("Error in selectAsActive():  ta == null || ta != tabAction");
        }
        deactivateActive();
        active_tabAction = ta;

        active_tabAction.activate(getTabContext());
        clear(); // doing a clear on tabPanel -- forces a redraw -- I know.. awkward
        workArea.clear();
    }

    public void selectAsActive(KeyStroke hotKey) {
        selectAsActive(findTabActionByHotKey(hotKey));
    }

    public void selectNextAsActive() {
        selectAsActive(getNextTab(active_tabAction));
    }
    
    public void selectPrevAsActive() {
        selectAsActive(getPrevTab(active_tabAction));
    }
    
    protected TabAction getNextTab(TabAction tabAction) {
        if (tabAction == null) {
            return tabActions.size()>0? tabActions.get(0) : null;
        }
        int idx = tabActions.indexOf(tabAction);
        return idx >= tabActions.size()-1 ? tabActions.get(0) : tabActions.get(idx+1); 
    }

    protected TabAction getPrevTab(TabAction tabAction) {
        if (tabAction == null) {
            return tabActions.size()>0? tabActions.get(tabActions.size()-1) : null;
        }
        int idx = tabActions.indexOf(tabAction);
        return idx <= 0 ? tabActions.get(tabActions.size()-1) : tabActions.get(idx-1); 
    }
    
    

    public static interface TabContext  extends IScreenLineSegmentsBuffer.Provider {

        EventBus getEventBus();

        Color getColor();

        Rectangle getBounds();

        void clearForRedraw();

        Rectangle getDrawingArea();
        
        IScreenLineSegmentsBuffer getSegmentsBuffer();


    }

    public static abstract class TabAction implements HasHotKey {

        KeyStroke    hotKey;
        String       tabText;
        TabComponent tabComponent;

        public TabAction(KeyStroke hotKey, String tabText) {
            this.hotKey = hotKey;
            this.tabText = tabText;
            this.tabComponent = new TabComponent(FGBGColors.DEFAULT, tabText, false);
        }
        

        abstract public void resize(TabContext tabContext);


	public void activate(TabContext tabContext) {
            getTabComponent().setActive(true);
        }

        TabComponent getTabComponent() {
            return tabComponent;
        }

        public void deativate(TabContext tabContext) {
            getTabComponent().setActive(false);
            drawOnUnActive(tabContext);
        }

        public KeyStroke getHotKey() {
            return hotKey;
        }

        public Color getTextActiveTabColor() {
	    return tabComponent.getTextActiveTabColor();
	}

	public void setTextActiveTabColor(Color textActiveTabColor) {
	    this.tabComponent.setTextActiveTabColor(textActiveTabColor);
	}

	abstract public void drawOnActive(TabContext tabContext);

        abstract public void drawOnUnActive(TabContext tabContext);

    }

}
