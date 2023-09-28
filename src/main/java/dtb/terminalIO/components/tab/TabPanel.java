package dtb.terminalIO.components.tab;

import dtb.terminalIO.components.AbstractComponentWrappper;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.CompositeWithFocusManager;
import dtb.terminalIO.components.GetComponentAtOffset;
import dtb.terminalIO.components.Hiliter;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.SimpleBorder;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Align;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.VAlign;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionRelativeFactory;

public class TabPanel extends AbstractComponentWrappper<CompositeWithFocusManager> 
                      implements CanResize, ParentContainer, GetComponentAtOffset, Hiliter {

    private ButtonBar tabsButtonBar;
    private Button previousButton;
    private Button nextButton;
    private ButtonBar prevNextButtonBar;
    private TabButtonComponent initial;
    private TabButtonComponent current;
    Component<?> currentComponentInTab;
    private SimpleBorder border;
    private Position workAreaPosition = new Position.Simple(3,1);

    Color textActiveTabColor = Colors.DEFAULT_COLOR.bold();

    public TabPanel(Color color, Dimension bounds) {
        super(color, new CompositeWithFocusManager(color));
        getWrappedComponent().setBounds(bounds);

        border = new SimpleBorder(getColor(), new Dimension.Simple(bounds.getHeight()-2, bounds.getWidth()));
        getWrappedComponent().add(workAreaPosition.addRows(-1).addCols(-1), border);

        tabsButtonBar = new ButtonBar().setCycling(true).setNoBorderChars();

        getWrappedComponent().add(Position.ZERO.addCols(1), tabsButtonBar);

        // Prev / Next
        prevNextButtonBar = new ButtonBar( getColor() ).setHorizontalSpacer(2);

        previousButton = new Button(getColor(), "<-");
        previousButton.setHotKey(previousTabHotKey);
        previousButton.setDisplayHotKey(false);
        previousButton.onChange((te)-> this.setPreviousAsCurrentFocus(te.getEventBus()));

        prevNextButtonBar.addComponent(previousButton);

        nextButton = new Button(getColor(),"->");
        nextButton.setHotKey(nextTabHotKey);
        nextButton.setDisplayHotKey(false);
        nextButton.onChange((te)-> this.setNextAsCurrentFocus(te.getEventBus()));

        prevNextButtonBar.addComponent(nextButton);

        Position navControlsPosition = PositionRelativeFactory.fromAlign(VAlign.BOTTOM, Align.RIGHT,  this, this.prevNextButtonBar);
        getWrappedComponent().add(navControlsPosition, prevNextButtonBar);
        
        FocusManager.UTIL.verifyPathsToFocusables(getWrappedComponent(), getWrappedComponent());

    }

    public TabPanel(Color color, Color textActiveTabColor, Dimension bounds) {
        this(color, bounds);
        this.textActiveTabColor = textActiveTabColor;
    }

    public void resize(Dimension size) {
        if (this.isSameDimension(size)) {
            return ;
        }
        
        super.resize(size);
        this.border.resize(new Dimension.Simple(size.getHeight()-2, size.getWidth()));
        if (current != null) {
            current.tabAction.resize(getTabContext());
        }
    }

    public TabPanel setPrevKeyStroke(KeyStroke prevKeyStroke) {
        this.previousTabHotKey = prevKeyStroke;
        this.tabsButtonBar.setPrevKeyStroke(prevKeyStroke);
        return this;
    }

    public TabPanel setNextKeyStroke(KeyStroke nextKeyStroke) {
        this.nextTabHotKey = nextKeyStroke;
        this.tabsButtonBar.setNextKeyStroke(nextKeyStroke);
        return this;
    }

    KeyStroke previousTabHotKey = Keys.SHIFT_TAB_KEYSTROKE;
    KeyStroke nextTabHotKey = Keys.TAB_KEYSTROKE;


    int offsetRow = 0;
    int offsetCol = 1;

    boolean helpFooterBuilt = true;

    public void addTab(final String tabText, KeyStroke hotKey, final TabAction<?> tabAction) {

        if (hotKey == null)
            throw new IllegalArgumentException("TabAction must specify a hotKey");

//        if (findTabActionByHotKey(hotKey) != null)
//            throw new IllegalArgumentException("TabAction must specify a unique hotKey");

        String tabTxt  = tabText;
        if (!tabTxt.contains(hotKey.getName())) {
            tabTxt = hotKey.getName() + " - " + tabTxt;
        }

        // todo:  hotkey not handled ( I think )
        TabButtonComponent tabButton = new TabButtonComponent(TabPanel.this.getColor(), tabTxt, false, tabAction, hotKey);
        if (initial == null) {
            initial = tabButton;
        }

        tabButton.onChange((te)->{

            if ( current ==  tabButton ) {
                return ;
            }

            if (current != null) {
                current.tabAction.deactivate(te.getEventBus(), getTabContext());
                current.setActive(false);
                TabPanel.this.getWrappedComponent().remove( currentComponentInTab );
                currentComponentInTab = null;
            }

            current = tabButton;
            currentComponentInTab = tabAction.getComponentForTab(getTabContext());
            if (currentComponentInTab == null) {
                throw new NullPointerException("tabAction.getComponentForTab(ctx) returned null for tabAction : '"+tabButton.getTabText()+"'");
            }

            TabPanel.this.getWrappedComponent().add(workAreaPosition, currentComponentInTab);

            te.getEventBus().asynch((te2) -> {
                // activate the panel on the next event loop
                current.tabAction.activate(te2.getEventBus(), getTabContext());
                current.setActive(true);
                te2.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(currentComponentInTab));
            });
            
            FocusManager.UTIL.verifyPathsToFocusables(getWrappedComponent(), getWrappedComponent());
            
        });

        tabsButtonBar.addComponent(tabButton);

    }

    TabContext getTabContext() {
        return new TabContext() {

            @Override
            public Color getColor() {
                return TabPanel.this.getColor();
            }

            @Override
            public Rectangle getBounds() {
                // Fragile .. 
                return new Rectangle.Simple(Position.ZERO, new Dimension.Simple(TabPanel.this.getHeight()-4, TabPanel.this.getWidth()-2));
            }

            @Override
            public Focusable toFocusable(Component<?> componentForTab) {
                Focusable focusable = ToFocusable.UTIL.toFocusable(componentForTab);
                if (focusable != null) {
                    return focusable;
                }
                // see if container knows the associated focusable
                if (!getWrappedComponent().containsChild(componentForTab)) {
                    return null;
                }
                focusable = getWrappedComponent().getFocusableForChild(componentForTab);

                return focusable;
            }

        };
    }

    public boolean displayCurrentTab(EventBus eventBus) {
        if (this.current == null  && initial != null) {
            initial.onClick(eventBus);
        }
        if (this.tabsButtonBar.getCurrentFocus() != null && this.tabsButtonBar.getCurrentFocus() instanceof TabButtonComponent) {
            ((TabButtonComponent)tabsButtonBar.getCurrentFocus()).onClick(eventBus);
            return true;
        }
        return false;
    }

    
    public boolean setPreviousAsCurrentFocus(EventBus eventBus) {
        boolean previousSet = this.tabsButtonBar.setPreviousAsCurrentFocus(eventBus);
        
        if (previousSet  && this.tabsButtonBar.getCurrentFocus() instanceof TabButtonComponent) {
            ((TabButtonComponent)tabsButtonBar.getCurrentFocus()).onClick(eventBus);
        }
        return previousSet;
    }

    public boolean setNextAsCurrentFocus(EventBus eventBus) {
        boolean nextSet = this.tabsButtonBar.setNextAsCurrentFocus(eventBus);
        if (nextSet  && this.tabsButtonBar.getCurrentFocus() instanceof TabButtonComponent) {
            ((TabButtonComponent)tabsButtonBar.getCurrentFocus()).onClick(eventBus);
        }
        return nextSet;
    }
    
    public static interface TabContext {

        Color getColor();

        Rectangle getBounds();

        Focusable toFocusable(Component<?> componentForTab);

    }

    private Hiliter hiliter = new Hiliter() {

        @Override
        public void hilite() {
            TabPanel.this.border.hilite();
            TabPanel.this.markAsDirty();
        }

        @Override
        public void unHilite() {
            TabPanel.this.border.unHilite();
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
