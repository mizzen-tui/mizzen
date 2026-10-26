package dtb.terminalIO.components.tab;

import java.util.Collection;

import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ComponentWithFocus;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.GetComponentAtOffset;
import dtb.terminalIO.components.Hiliter;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.SimpleBorder;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.input.ArrowKeys;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.AsKeyboardHandler;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.geom.Align;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.VAlign;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionRelativeFactory;

public class TabPanel extends ComponentWithFocus<Composite, TabPanel> 
                      implements CanResize, ParentContainer, GetComponentAtOffset, Hiliter {

    private ButtonBar tabsButtonBar;
    private Button previousButton;
    private Button nextButton;
    private HorizontalPanel prevNextButtonBar;
    private TabButtonComponent initial;
    private TabButtonComponent current;
    Component<?> currentComponentInTab;
    private SimpleBorder border;
    private Position workAreaPosition = new Position.Simple(3,1);

    Color textActiveTabColor = Colors.DEFAULT_COLOR.bold();

    // Optional shortcuts to switch to the previous / next tab from anywhere inside this TabPanel
    // (see setPrevKeyStroke / setNextKeyStroke). The tab's content always gets the key first;
    // these only fire if the content does not handle it. None by default.
    // NOTE : never use TAB / SHIFT_TAB here - that circumvents normal FocusManager navigation.
    KeyStroke previousTabHotKey = null;
    KeyStroke nextTabHotKey = null;

    
    public TabPanel(Color color, Dimension bounds) {
        super(color, new Composite(color));
        getWrappedComponent().setBounds(bounds);

        border = new SimpleBorder(getColor(), new Dimension.Simple(bounds.getHeight()-2, bounds.getWidth()));
        getWrappedComponent().add(workAreaPosition.addRows(-1).addCols(-1), border);

        // tab strip: arrows move between tabs (wrapping) and show them, focus stays on the strip;
        // TAB leaves the strip into the tab's content. Using TAB here made the strip a focus trap.
        // ONE_STOP: the strip is one TAB stop that remembers the active tab
        tabsButtonBar = new ButtonBar().setNoBorderChars();
        tabsButtonBar.focus().singleTabStop()
                             .arrowsMoveInside(ArrowKeys.LEFT_RIGHT)
                             .wrapAround();

        getWrappedComponent().add(Position.ZERO.addCols(1), tabsButtonBar);

        // Prev / Next
        prevNextButtonBar = new HorizontalPanel( getColor() ).setHorizontalSpacer(2);

        previousButton = new Button(getColor(), "<-");
        //previousButton.setHotKey(previousTabHotKey);  <-- see comment above why I am disabling hot key
        previousButton.setDisplayHotKey(false);
        previousButton.onChange((te)-> this.setPreviousAsCurrentFocus(te.getEventBus()));

        prevNextButtonBar.add(previousButton);

        nextButton = new Button(getColor(),"->");
        //nextButton.setHotKey(nextTabHotKey);  <-- see comment above why I am disabling hot key
        nextButton.setDisplayHotKey(false);
        nextButton.onChange((te)-> this.setNextAsCurrentFocus(te.getEventBus()));

        prevNextButtonBar.add(nextButton);

        Position navControlsPosition = PositionRelativeFactory.fromAlign(VAlign.BOTTOM, Align.RIGHT,  this, this.prevNextButtonBar);
        getWrappedComponent().add(navControlsPosition, prevNextButtonBar);
        
        // prev / next tab shortcuts: last in this panel's handler chain, so they only fire
        // when neither the tab strip nor the tab's content handled the key
        focus().handleKeys(AsKeyboardHandler.UTIL.toKeyboardHandler(
            new KeyInputEventHandler(
                (te) -> {
                    KeyEvent ke = (KeyEvent) te;
                    return (previousTabHotKey != null && ke.isKeyStroke(previousTabHotKey))
                        || (nextTabHotKey != null && ke.isKeyStroke(nextTabHotKey));
                },
                (te) -> {
                    if (previousTabHotKey != null && ((KeyEvent) te).isKeyStroke(previousTabHotKey)) {
                        setPreviousAsCurrentFocus(te.getEventBus());
                    } else {
                        setNextAsCurrentFocus(te.getEventBus());
                    }
                })));

        FocusManager.UTIL.verifyPathsToFocusables((FocusManager) asFocusable(), getWrappedComponent());

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
        Dimension sizeAfterSuperResize = new Dimension.Simple(this);   // not size: the parent's rules may have resized this again
        this.border.resize(new Dimension.Simple(sizeAfterSuperResize.getHeight()-2, sizeAfterSuperResize.getWidth()));
        if (current != null) {
            current.tabAction.resize(getTabContext());
        }
    }

    /**
     * Shortcut to switch to the previous tab from anywhere inside this TabPanel
     * (only fires if the tab's content does not handle the key). null = none.
     */
    public TabPanel setPrevKeyStroke(KeyStroke prevKeyStroke) {
        this.previousTabHotKey = prevKeyStroke;
        return this;
    }

    /**
     * Shortcut to switch to the next tab from anywhere inside this TabPanel
     * (only fires if the tab's content does not handle the key). null = none.
     */
    public TabPanel setNextKeyStroke(KeyStroke nextKeyStroke) {
        this.nextTabHotKey = nextKeyStroke;
        return this;
    }


    int offsetRow = 0;
    int offsetCol = 1;

    boolean helpFooterBuilt = true;

    public TabPanel addTab(final String tabText, KeyStroke hotKey, final TabAction<?> tabAction) {

        // if (hotKey == null)
        //     throw new IllegalArgumentException("TabAction must specify a hotKey");

        String tabTxt  = tabText;
        if (hotKey != null) {
            if (!tabTxt.contains(hotKey.getName())) {
                tabTxt = hotKey.getName() + " - " + tabTxt;
            }
        }

        // todo:  hotkey not handled ( I think )
        TabButtonComponent tabButton = new TabButtonComponent(TabPanel.this.getColor(), tabTxt, false, tabAction, hotKey);
        if (initial == null) {
            initial = tabButton;
        }

        tabButton.onChange((te)->{

            // explicitly chosen (click / hot key / shortcut) -> focus goes into the tab's content;
            // reached by navigating the tab strip -> focus stays on the strip
            boolean focusContent = tabButton.consumeFocusContentRequest();

            if ( current ==  tabButton ) {
                if (focusContent) {
                    te.getEventBus().asynch((te2) -> requestFocusOnCurrentTabContent(te2.getEventBus()));
                }
                return;
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

            // insert the content before the prev/next buttons so TAB order is:
            // tab strip -> tab content -> prev/next buttons
            TabPanel.this.getWrappedComponent().insert(getWrappedComponent().indexOf(prevNextButtonBar),
                                                       workAreaPosition, currentComponentInTab);

            te.getEventBus().asynch((te2) -> {
                // activate the panel on the next event loop
                current.tabAction.activate(te2.getEventBus(), getTabContext());
                current.setActive(true);
                if (focusContent) {
                    requestFocusOnCurrentTabContent(te2.getEventBus());
                }
                te2.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(currentComponentInTab));
            });

            FocusManager.UTIL.verifyPathsToFocusables((FocusManager) asFocusable(), getWrappedComponent());

        });

        tabsButtonBar.addComponent(tabButton);

        return this;

    }

    public TabPanel addTab(final String tabText, KeyStroke hotKey, final Component<?> component) {
        return addTab(tabText, hotKey, new TabAction<Component<?>>() {
                @Override
                protected Component<?> getComponent(TabContext tabContext) {
                    return component;
                }            
            });
    }
    public TabPanel addTab(final String tabText, final Component<?> component) {
        return addTab(tabText,null, component);
    }


    private void requestFocusOnCurrentTabContent(EventBus eventBus) {
        if (current == null) {
            return;
        }
        Focusable focusable = current.tabAction.getFocusableForActivation(getTabContext());
        if (focusable != null) {
            eventBus.raiseEvent( new RequestFocusEvent( focusable ));
        }
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
                // the first focusable inside it
                Collection<Focusable> inside = FocusManager.UTIL.focusablesFromComponent(componentForTab);
                return inside.isEmpty() ? null : inside.iterator().next();
            }

        };
    }

    public boolean displayCurrentTab(EventBus eventBus) {
        if (this.current == null  && initial != null) {
            initial.onClick(eventBus);
        }
        if (this.tabsButtonBar.focus().currentChild() != null && this.tabsButtonBar.focus().currentChild() instanceof TabButtonComponent) {
            ((TabButtonComponent)tabsButtonBar.focus().currentChild()).onClick(eventBus);
            return true;
        }
        return false;
    }

    
    public boolean setPreviousAsCurrentFocus(EventBus eventBus) {
        boolean previousSet = this.tabsButtonBar.focus().focusPrevious(eventBus);
        
        if (previousSet  && this.tabsButtonBar.focus().currentChild() instanceof TabButtonComponent) {
            ((TabButtonComponent)tabsButtonBar.focus().currentChild()).onClick(eventBus);
        }
        return previousSet;
    }

    public boolean setNextAsCurrentFocus(EventBus eventBus) {
        boolean nextSet = this.tabsButtonBar.focus().focusNext(eventBus);
        if (nextSet  && this.tabsButtonBar.focus().currentChild() instanceof TabButtonComponent) {
            ((TabButtonComponent)tabsButtonBar.focus().currentChild()).onClick(eventBus);
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
