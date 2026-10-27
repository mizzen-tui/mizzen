package mizzen.components.scroll;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import mizzen.components.ComponentWithFocus;
import mizzen.components.BorderedComponent;
import mizzen.components.ButtonBar;
import mizzen.components.Component;
import mizzen.components.HasName;
import mizzen.components.TextComponent;
import mizzen.components.input.Button;
import mizzen.components.composite.Composite;
import mizzen.components.scroll.SearchFunctionForScrollableComponent.AsSearchableView;
import mizzen.components.scroll.SearchFunctionForScrollableComponent.SearchableView;
import mizzen.controlcodes.color.Color;
import mizzen.events.ChainedEventHandler;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventBus;
import mizzen.events.mouse.ApplicationMouseEvent;
import mizzen.events.mouse.Clickable;
import mizzen.events.EventHandler;
import mizzen.events.RequestUpdateCanvasEvent;
import mizzen.events.TerminalEvent;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.AsKeyboardHandler;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyInputEventHandler;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.Keys;
import mizzen.geom.CanResize;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.util.Logging;

/**
 * A view onto a component larger than itself, with scrollbars, a footer of scroll buttons (for the
 * mouse) and scroll keys (UP, DOWN, PgUp, PgDn, LEFT, RIGHT). When the viewed component can take
 * focus, focus goes inside it and the scroll keys work for whatever it doesn't use; when it can't (a
 * TextComponent), the pane itself is the TAB stop - its border hilites and its keys scroll it.
 */
public class ScrollableComponent  extends ComponentWithFocus<Composite, ScrollableComponent>
                                  implements CanScrollToView, ViewPortNavigable, HasViewPortWindow, AsSearchableView, Clickable {

    // its own handlers: the scroll keys, footer keys for the content, scroll-to-view and view-port-changed
    private final ChainedEventHandler ownHandlers = ChainedEventHandler.useChainOfResponsibilityStrategy(this);

    WindowViewComponent viewer;
    BorderedComponent borderedComponent;
    ScrollBarVertical vScrollBar;
    ScrollBarHorizontal hScrollBar;

    Component<?> componentBeingViewed;
    ButtonBar footerControls; 
    private CanResize onResizeService = CanResize.NOOP;

    private ViewPortNavigable navigator;


    public ScrollableComponent(Color color, Dimension d, Component<?> componentBeingViewed)  {
        super(color, new Composite());
        
    //    this.setBorder(true);

        navigator = new DefaultViewPortNavigableImpl();

        this.componentBeingViewed = componentBeingViewed;

        viewer = new WindowViewComponent(color,
                                         new Dimension.Simple(d.getHeight() - 2, d.getWidth() - 2),
                                         this.componentBeingViewed);

        borderedComponent = new BorderedComponent(color, viewer);
        this.getContainer().add(borderedComponent);
        this.getContainer().onResizeExpandDownAndRight(borderedComponent);

        buildAndPopulateFooterPanel();
        // the panel re-measures when it is resized or when a part on the box's border changes size:
        // either way the bottom line is laid out again - before the next draw, not in the middle of the
        // size-change notices (their outer steps would put back the sizes they had worked out)
        this.getContainer().appendOnResizeRule((size) -> {
            footerLayoutOwed = true;
            markAsDirty();
        });

        vScrollBar = new ScrollBarVertical(color,
                                           borderedComponent.getPostion_TopRight().offsetAdd(1, 0),
                                           borderedComponent.getHeight() - 2).enableKeyEvents(false)
                                           .setTabStop(false);   // mouse controls, like the footer: not a TAB stop

        HasName.UTIL.decorateWithParentsNamePrefix(vScrollBar, this);

        vScrollBar.setOffset(Position.ZERO);

        vScrollBar.setViewedSize(new Rectangle.Delegate(componentBeingViewed), new Rectangle.Delegate(viewer));

        vScrollBar.onChange((te) -> emitViewOffsetChangedOnOperation(te, () ->      // told like a key or footer scroll
                this.setViewOffset(vScrollBar.getValue().getRow(), this.getOffsetIntoSource().getCol())));

        hScrollBar = new ScrollBarHorizontal(color,
                                             borderedComponent.getPostion_BottomLeft().offsetAdd(0, 1),
                                             borderedComponent.getWidth() - 3 - footerControls.getWidth()).enableKeyEvents(false)
                                           .setTabStop(false);   // mouse controls, like the footer: not a TAB stop

        HasName.UTIL.decorateWithParentsNamePrefix(hScrollBar, this);

        hScrollBar.setOffset(Position.ZERO);

        hScrollBar.setViewedSize(new Rectangle.Delegate(componentBeingViewed), new Rectangle.Delegate(viewer));

        hScrollBar.onChange((te) -> emitViewOffsetChangedOnOperation(te, () ->
                this.setViewOffset(this.getOffsetIntoSource().getRow(), hScrollBar.getValue().getCol())));

        this.getContainer().add(hScrollBar);
        this.getContainer().add(vScrollBar);

        ownHandlers.appendEventHandler(new  WindowViewComponent.ScrollToViewHandler(()->this.viewer, this));

        ownHandlers.appendEventHandler(new ConditionalEventHandler() {

            @Override
            public void handle(TerminalEvent<?> te) {
                // TODO Auto-generated method stub
                if (footerControls.asFocusable().hasFocus()) {
                    Logging.info("FOOTER focus evt = " + te);
                    Focusable focusableChild = ToFocusable.UTIL.toFocusable(ScrollableComponent.this.componentBeingViewed);
                    if (focusableChild == null) {
                        return;
                    }
                    if (focusableChild.canHandle(te)!=null) {
                        EventBus evtBus = te.getEventBus();
                        focusableChild.handle(te.then(()->{
                            evtBus.raiseEvent( new RequestFocusEvent(focusableChild) );
                        }));
                    } 
                }
            }

            @Override
            public EventHandler canHandle(TerminalEvent<?> te) {
                if (footerControls.asFocusable().hasFocus() && footerControls.asFocusable().canHandle(te)==null ) {
                    // footer cannot handle it
                    if ( ToFocusable.UTIL.isFocusable(ScrollableComponent.this.componentBeingViewed) ) {
                        Focusable focusableChild = ToFocusable.UTIL.toFocusable(ScrollableComponent.this.componentBeingViewed);
                        if (focusableChild != null) {
                            if (focusableChild.canHandle(te)!=null) {
                                return this;
                            }
                        }
                    }
                }
                return null;
            }});

        // scrolled: the component shown hears of it if it wants to (a text input moves its caret), then
        // the app's handler (onViewPortChanged). One handler for both - the chain runs only the first
        // that claims the event - set up once, so changing what is shown keeps the app's handler
        ownHandlers.appendEventHandler(HasViewPortWindow.UTIL.wrapHandler(this, (te) -> {
            if (this.componentBeingViewed instanceof HasViewPortWindow.OnChange) {
                ((HasViewPortWindow.OnChange) this.componentBeingViewed).onViewPortWindowChange(this, te);
            }
            if (viewPortChanged != null) {
                viewPortChanged.handle(te);
            }
        }));

        // focus: inside the viewed component if it can take it, else on the pane itself; keys and other
        // events go to its own handlers
        ConditionalEventHandler own = ownHandlers.asConditionalEventHandler();
        focus().takeFocusItselfWhenNoChildCan()
               .handleKeys(AsKeyboardHandler.UTIL.toKeyboardHandler(own))
               .handleEvents(new ConditionalEventHandler.Impl(
                       (te) -> !(te instanceof KeyEvent) && own.canHandle(te) != null,
                       (te) -> own.handle(te)));
    }


    private Composite getContainer() {
        return getPanel();
    }

    public ScrollableComponent setTitle(String title) {
        this.borderedComponent.setTitle(title);
        return this;
    }

    public ScrollableComponent setTitle(Color titleColors, String title) {
        this.borderedComponent.setTitle(titleColors, title);
        return this;
    }
    

    public Component<?> getComponentBeingViewed() {
        return this.componentBeingViewed;
    }

    public synchronized void setComponentBeingViewed(Component<?> componentBeingViewed) {
        this.componentBeingViewed = componentBeingViewed;
        viewer.setViewSource(componentBeingViewed);
        vScrollBar.setViewedSize(new Rectangle.Delegate(componentBeingViewed), new Rectangle.Delegate(viewer));
        hScrollBar.setViewedSize(new Rectangle.Delegate(componentBeingViewed), new Rectangle.Delegate(viewer));
    }

    public boolean setViewOffset(int r, int c) {
        if (r < 0 || c < 0)
            return false; // too small
        if (r > (componentBeingViewed.getBottomMostRow() + 1)  || 
            c > (componentBeingViewed.getRightMostCol()  + 1)  )
            return false; // too large

        Position offset = new Position.Simple(r, c);
        boolean changedOffset = viewer.setViewOffset(offset);
        if (changedOffset) {
            //viewer.markAsDirty();
            markAsDirty();
            this.vScrollBar.setOffset(offset);
            this.hScrollBar.setOffset(offset);
        }
        return changedOffset;
    }

    /**
     * A fallback, for the one case the mouse handling cannot cover: a click in the view that does
     * not land on the component shown. Most clicks never come here - the mouse handling finds the
     * component under the mouse and delivers the click to it directly. This only makes up for the
     * clicks where nothing of the component is under the mouse.
     * <p/>
     * When that happens: what is shown is only as big as its content - a text input is as wide as
     * its longest line and as tall as its lines - so a click past the end of a short line, or below
     * the last line, lands on this scrollable, not on what it shows:
     * <pre>
     *   +----------------------------+   the scrollable's border
     *   |Rush - boat.................|
     *   |in the yard.....X...........|   X: a click here
     *   |............................|
     *   +--| ^ | v | PgUp | PgDn |---+
     *
     *   the text input is only the letters' block (11 wide, 2 high);
     *   the dots are the scrollable's own empty view
     * </pre>
     * The mouse handling only knows where the scrollable was clicked (X: row 2, col 17 of it). Which
     * spot of the content that is, only the scrollable knows: one in from its border, plus how far
     * it is scrolled (X: line 1, col 16 of the text). So it works that out and passes the click on,
     * as a click on the content at that spot - and a text input puts its caret as near as it can:
     * the end of that line.
     * <p/>
     * (Making the text input as big as the view would also catch these clicks, but only for that one
     * component; this works for anything shown in a scrollable.)
     */
    @Override
    public void onClick(ApplicationMouseEvent click) {
        if (!(componentBeingViewed instanceof Clickable) || click.getMousePosition() == null) {
            return;
        }
        Position me = Component.UTIL.getCanvasOffsetFor(this);
        Position view = Component.UTIL.getCanvasOffsetFor(viewer);
        if (me == null || view == null) {
            return;
        }
        Position inView = me.offsetAdd(click.getMousePosition()).offsetAdd(view.negate());
        if (inView.getRow() < 0 || inView.getCol() < 0 || inView.getRow() >= viewer.getHeight() || inView.getCol() >= viewer.getWidth()) {
            return;                                        // on the border or the footer, not the view
        }
        ((Clickable) componentBeingViewed).onClick(click.at(inView.offsetAdd(getOffsetIntoSource())));
    }

    @Override
    public void onClick(EventBus eventBus) {
        // only clicks with a position mean anything here: onClick(ApplicationMouseEvent)
    }

    public Position getOffsetIntoSource() {
        return viewer.getOffsetIntoSource();
    }

    public Rectangle getViewPortAsRectangle() {
        Position offset = this.getOffsetIntoSource();
        return new Rectangle.Simple(offset.getRow(), offset.getCol(), viewer.getHeight(), viewer.getWidth());
    }


    public boolean scrollToView(Rectangle targetToView) {

        Position offset = viewer.calculateScrollToViewOffset(targetToView);

        if (offset != null) {
            // Logging.forceLogInfo("scrollToView " + String.valueOf(offset) );
            return setViewOffset(offset.getRow(), offset.getCol());
        } else {
            return false;
        }
    }


    public void resize(Dimension d) {
        super.resize(d);
        borderedComponent.resize(d);

        vScrollBar.setRectangle(new Rectangle.Simple(borderedComponent.getPostion_TopRight().offsetAdd(1, 0),
                                                     new Dimension.Simple(borderedComponent.getHeight() - 2, 1)));
        vScrollBar.markAsDirty();

//        positionFooterText();
        handleResizeDependants(d);
        layOutFooter();                                  // after the resize rules: they may change footer parts (search)

        markAsDirty();
        this.componentBeingViewed.markAsDirty();
    }
    
    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        if (footerLayoutOwed) {
            footerLayoutOwed = false;
            layOutFooter();
        }
        super.draw(color, segments);
    }

    public void markAsDirty() {
        super.markAsDirty();
        if (vScrollBar != null) {
            vScrollBar.markAsDirty();
            hScrollBar.markAsDirty();
            // following is a hack because need to always ensure on top - but it is expensive
            // need to find a better (less called) place to do this
            getContainer().bringToFront(vScrollBar);
            getContainer().bringToFront(hScrollBar);
        }
    }
    
    
    private void handleResizeDependants(Dimension size) {
        this.onResizeService.resize(size);
        if (this.getParentContainer()!=null ) {
            this.getParentContainer().childResized(this);
        }
    }
    
    public ScrollableComponent appendOnResizeRule(CanResize resizableRule) {
        if (this.onResizeService == null) {
            setOnResizeRule(resizableRule);
            return this;
        }
        this.onResizeService = new CanResize.Chain(this.onResizeService, resizableRule);
        return this;
    }
    
    public ScrollableComponent setOnResizeRule(CanResize resizableRule) {
        this.onResizeService = resizableRule == null ? CanResize.NOOP : resizableRule;
        return this;
    }
    
    
    boolean hasFooterControls = true;
    
    private void refreshFooter() {
        if (!hasFooterControls) {
            return;
        }
        layOutFooter();                                  // a footer part grew or shrank (e.g. search)
    }

    private final List<Button> scrollButtons = new ArrayList<Button>();       // in SCROLL_ACTIONS order
    private final List<Component<?>> addedFooterParts = new ArrayList<Component<?>>();   // by addFooterComponent (e.g. search)
    /** dropped in pairs when the bottom line is short of room: « », then PgUp PgDn, then ^ v */
    private static final int[][] SCROLL_BUTTONS_DROP_ORDER = { { 4, 5 }, { 2, 3 }, { 0, 1 } };
    private boolean layingOutFooter;
    private boolean footerLayoutOwed;                    // a footer part changed size: lay the line out before drawing

    /**
     * The bottom line: the footer parts (status, text, scroll buttons, other components such as
     * search) and the horizontal scrollbar in what is left. Short of room, in this order until it
     * fits: the scrollbar gives up its reserved room; scroll buttons are dropped (« », then PgUp PgDn,
     * then ^ v - their keys still work); the status shrinks to its smallest; an open search field
     * narrows - search stays, the user asked for it. If it still doesn't fit, the border clips it,
     * keeping its right end. With room again, everything comes back.
     */
    private void layOutFooter() {
        if (footerControls == null || layingOutFooter) {
            return;
        }
        layingOutFooter = true;
        try {
            int room = borderedComponent.getWidth() - 2;
            showAllScrollButtons();
            fitFooterStatus(room, MIN_HSCROLLBAR_WIDTH);
            if (footerControls.getWidth() > room) {
                fitFooterStatus(room, 0);
            }
            for (int[] pair : SCROLL_BUTTONS_DROP_ORDER) {
                if (footerControls.getWidth() <= room) {
                    break;
                }
                for (int i : pair) {
                    hideScrollButton(i);
                }
                fitFooterStatus(room, 0);
            }
            if (footerControls.getWidth() > room) {
                for (Component<?> part : addedFooterParts) {
                    if (part instanceof SearchFunctionForScrollableComponent) {
                        ((SearchFunctionForScrollableComponent) part).narrowSearchField(footerControls.getWidth() - room);
                    }
                }
            }
            footerControls.markAsDirty();
            // one row and no footer: that row is the title's line - no scrollbar on it
            int hScrollWidth = borderedComponent.getHeight() < 2 && !footerShown ? 0 : Math.max(0, room - footerControls.getWidth());
            hScrollBar.setRectangle(new Rectangle.Simple(borderedComponent.getPostion_BottomLeft().offsetAdd(0, 1),
                                                         new Dimension.Simple(1, hScrollWidth)));
            hScrollBar.markAsDirty();
        } finally {
            layingOutFooter = false;
        }
    }

    private void hideScrollButton(int i) {
        Button button = scrollButtons.get(i);
        if (footerControls.indexOf(button) >= 0) {
            footerControls.remove(button);
        }
    }

    /** each dropped scroll button back in its place: after the shown button before it, or after the status and text */
    private void showAllScrollButtons() {
        for (int i = 0; i < scrollButtons.size(); i++) {
            Button button = scrollButtons.get(i);
            if (footerControls.indexOf(button) >= 0) {
                continue;
            }
            int at = footerStatus != null ? 1 : 0;      // after the status
            for (int before = i - 1; before >= 0; before--) {
                int index = footerControls.indexOf(scrollButtons.get(before));
                if (index >= 0) {
                    at = index + 1;
                    break;
                }
            }
            footerControls.insertComponent(at, button);
        }
    }
    
    /** no footer at all: no scroll buttons, status or other parts on the bottom line (the scroll keys still work) */
    public ScrollableComponent removeFooter() {
        borderedComponent.removeFooterComponent();
        footerShown = false;
        return this;
    }

    /** @deprecated it removes the whole footer, not one component: use removeFooter() */
    @Deprecated
    public ScrollableComponent removeFooterComponent() {
        return removeFooter();
    }

    /** the footer parts can't be set once the footer is gone: say so rather than set them where they never show */
    private void assertFooterShown(String what) {
        if (!footerShown) {
            throw new IllegalStateException(HasName.UTIL.getName(this) + ": cannot " + what + " - the footer was removed (removeFooter)");
        }
    }

    private boolean footerShown = true;

    private void buildAndPopulateFooterPanel() {
        Color borderColor = borderedComponent.getColor();
        if (footerControls != null) {
            return ;
        }
        footerControls = new ButtonBar(borderColor, HasName.UTIL.getName(this)+".buttonBar");
        // the footer buttons are for the mouse - not TAB stops, no hot keys (scroll keys are the pane's
        // own keys, as for any scrollable area: they reach the pane from the focused component inside it,
        // or with focus on the pane itself, and don't move focus)
        footerControls.focus().skippedByTab();
        for (ScrollAction action : SCROLL_ACTIONS) {
            scrollButtons.add(footerControls.addButton(action.label, null, (te) -> emitViewOffsetChangedOnOperation(te, () -> action.scroll.accept(this))));
        }

        this.borderedComponent.setFooter(footerControls);

        // the same actions' keys, while focus is in (or on) the pane and nothing inside used them
        KeyStroke[] keys = new KeyStroke[SCROLL_ACTIONS.length];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = SCROLL_ACTIONS[i].key;
        }
        ownHandlers.appendEventHandler(new KeyInputEventHandler(keys, (te) -> footerShown, (te) -> {
            for (ScrollAction action : SCROLL_ACTIONS) {
                if (((KeyEvent) te).isKeyStroke(action.key)) {
                    emitViewOffsetChangedOnOperation(te, () -> action.scroll.accept(this));
                    return;
                }
            }
        }));
    }

    /**
     * The scroll actions: each is a footer button (for the mouse) and a key (for the keyboard) -
     * add one here and both appear.
     */
    private static final ScrollAction[] SCROLL_ACTIONS = {
        new ScrollAction("^",    Keys.UP_KEYSTROKE,    (pane) -> pane.up(1)),
        new ScrollAction("v",    Keys.DOWN_KEYSTROKE,  (pane) -> pane.down(1)),
        new ScrollAction("PgUp", Keys.PGUP_KEYSTROKE,  ScrollableComponent::pageup),
        new ScrollAction("PgDn", Keys.PGDN_KEYSTROKE,  ScrollableComponent::pagedown),
        new ScrollAction("«",    Keys.LEFT_KEYSTROKE,  (pane) -> pane.left(1)),
        new ScrollAction("»",    Keys.RIGHT_KEYSTROKE, (pane) -> pane.right(1)),
    };

    private static final class ScrollAction {
        final String label;
        final KeyStroke key;
        final Consumer<ScrollableComponent> scroll;

        ScrollAction(String label, KeyStroke key, Consumer<ScrollableComponent> scroll) {
            this.label = label;
            this.key = key;
            this.scroll = scroll;
        }
    }

    private void emitViewOffsetChangedOnOperation(TerminalEvent<?> event, Runnable operation) {
        Position prevOffset = this.getOffsetIntoSource();
        operation.run();
        Position currentOffset = this.getOffsetIntoSource();
        if (currentOffset == prevOffset || currentOffset.isSameLocation(prevOffset)) {
            return;
        }
        event.getEventBus().raiseEvent( new HasViewPortWindow.ViewPortWindowChangedEvent(this) );
        event.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(this) );     // show it (a key press repaints nothing else)
    }

    private EventHandler viewPortChanged;               // the app's, run after the shown component's own (see the constructor)

    /**
     * Runs on scrolling - by the keys, the footer buttons or the scroll bars - whatever component is
     * shown, now or later. One handler: a second call replaces it, null removes it. E.g. two panes kept
     * at the same line:
     * <pre>
     *     left.onViewPortChanged((te) -> right.setViewOffset(left.getOffsetIntoSource().getRow(), 0));
     * </pre>
     */
    public void onViewPortChanged(EventHandler consumeChange) {
        this.viewPortChanged = consumeChange;
    }

    /** the pane's border is its BorderedComponent's (the container around it has none) - hilite that one */
    @Override
    public void hilite() {
        borderedComponent.hilite();
    }

    @Override
    public void unHilite() {
        borderedComponent.unHilite();
    }

    @Override
    public String getTitle() {
        return borderedComponent.getTitle();
    }

    /**
     * A scroll pane always shows its border (its scrollbars and footer sit on it): true is fine, false
     * is refused - before, it silently switched off an invisible box around the pane instead.
     */
    @Override
    public ScrollableComponent setBorder(boolean hasBorder) {
        if (!hasBorder) {
            throw new IllegalStateException(HasName.UTIL.getName(this) + ": a ScrollableComponent always shows its border");
        }
        return this;
    }

    @Override
    public boolean hasBorder() {
        return this.borderedComponent.hasBorder();
    }

    /** the visible border's colour (the bordered view's, also used for the scrollbars and footer) */
    @Override
    public ScrollableComponent setBorderColor(Color borderColor) {
        this.borderedComponent.setBorderColor(borderColor);
        if (footerControls != null) {
            footerControls.setColor(borderColor);
        }
        vScrollBar.setColor(borderColor);
        hScrollBar.setColor(borderColor);
        return this;
    }

    public Color getBorderColor() {
        return this.borderedComponent.getBorderColor();
    }

    /** colour of the content: the viewed component and the view around it */
    public void setContentColor(Color contentColor) {
        super.setColor(contentColor);
        this.borderedComponent.setColor(contentColor);
        this.componentBeingViewed.setColor(contentColor);
        this.viewer.setColor(contentColor);
    }

    private TextComponent footerText;                    // the status, when set by setFooterText
    private Component<?> footerStatus;
    private boolean footerStatusFitted;                  // a component status is fitted to the room; a text keeps its width

    /** the horizontal scrollbar keeps at least this much of the bottom line when a status is fitted */
    private static final int MIN_HSCROLLBAR_WIDTH = 10;

    /**
     * A one-row status at the start of the footer, before the scroll buttons - e.g. a progress meter:
     * <pre>
     *   +-------------------------------------------------------------+
     *   |  ...content...                                              |
     *   +-----hscroll-----Copying ######......  42% | ^ | v | PgUp ...+
     * </pre>
     * If it can resize, it is kept as wide as the bottom line allows - the width less the scroll
     * buttons and room for the horizontal scrollbar - as the pane is resized. Calling it again
     * replaces the status (and a footer text: they share the slot); null removes it. Throws if the
     * footer was removed.
     */
    public ScrollableComponent setFooterStatus(Component<?> status) {
        assertFooterShown("set a footer status");
        footerText = null;
        footerStatusFitted = true;
        return putFooterStatus(status);
    }

    /** the status slot: the one component before the scroll buttons */
    private ScrollableComponent putFooterStatus(Component<?> status) {
        if (status != null && status.getHeight() != 1) {
            throw new IllegalArgumentException("A footer status is one row high, not " + status.getHeight()
                    + " (" + HasName.UTIL.getName(status) + ")");
        }
        changeFooter(() -> {
            if (footerStatus != null) {
                footerControls.remove(footerStatus);
            }
            footerStatus = status;
            if (status != null) {
                footerControls.insertComponent(0, status);
            }
        });
        resize((Dimension) this);                        // fit it, and re-fit the horizontal scrollbar
        return this;
    }

    /**
     * A change to the footer bar made here: while it happens the bar is half changed (a button gone,
     * its separator not yet), so the layout - which a size change in the bar would start - waits
     * until it is done, then runs once.
     */
    private void changeFooter(Runnable change) {
        boolean outermost = !layingOutFooter;
        layingOutFooter = true;
        try {
            change.run();
        } finally {
            if (outermost) {
                layingOutFooter = false;
            }
        }
        if (outermost) {
            layOutFooter();
        }
    }

    public Component<?> getFooterStatus() {
        return footerStatus;
    }

    /** the status as wide as the line allows after the other footer parts and what the scrollbar keeps */
    private void fitFooterStatus(int room, int keepForScrollbar) {
        if (!footerStatusFitted || !(footerStatus instanceof CanResize)) {
            return;
        }
        int others = footerControls.getWidth() - footerStatus.getWidth();
        int width = room - others - keepForScrollbar;
        if (width > 0 && width != footerStatus.getWidth()) {
            ((CanResize) footerStatus).resize(new Dimension.Simple(1, width));   // it may stop at its own smallest
        }
    }

    /**
     * A short text as the footer status, before the scroll buttons, e.g. "count: 42" - it keeps its
     * own width. Calling it again changes the text; it shares the slot with setFooterStatus (the
     * last one set shows). Throws if the footer was removed.
     */
    public ScrollableComponent setFooterText(String text) {
        assertFooterShown("set a footer text");
        if (footerText != null && footerStatus == footerText) {
            changeFooter(() -> footerText.setTextAndResize(text));   // then laid out again: its width changed
            return this;
        }
        footerText = new TextComponent(getBorderColor(), text);
        footerStatusFitted = false;
        return putFooterStatus(footerText);
    }

    /** a component after the scroll buttons, e.g. search; throws if the footer was removed */
    public void addFooterComponent(Component<?> componentForButtonBar) {
        assertFooterShown("add a footer component");
        changeFooter(() -> {                             // then laid out: room is made for it
            footerControls.addComponent(componentForButtonBar);
            addedFooterParts.add(componentForButtonBar);
        });
    }

    public void up(int iLines) {
        this.navigator.up(iLines);
    }

    public void down(int iLines) {
        this.navigator.down(iLines);
    }

    public void left(int iCols) {
        this.navigator.left(iCols);
    }

    public void right(int iCols) {
        this.navigator.right(iCols);
    }

    public void pageup() {
        int pageHeight = viewer.getHeight();
        int delta = pageHeight;
        if (ScrollableComponent.this.getOffsetIntoSource().getRow() < delta) {
            delta = ScrollableComponent.this.getOffsetIntoSource().getRow();
        }
        up(delta);
    }

    public void pagedown() {
        int pageHeight = viewer.getHeight();
        int delta = pageHeight;
        if (ScrollableComponent.this.getOffsetIntoSource().getRow() + delta > componentBeingViewed.getBottomMostRow() + 1) {
            delta = (componentBeingViewed.getBottomMostRow() + 1) - ScrollableComponent.this.getOffsetIntoSource().getRow();
        }
        down(delta);
    }

    class DefaultViewPortNavigableImpl implements ViewPortNavigable {

        public void up(int iLines) {
            ScrollableComponent.this.setViewOffset(ScrollableComponent.this.getOffsetIntoSource().getRow() - iLines, ScrollableComponent.this.getOffsetIntoSource().getCol());
        }

        public void down(int iLines) {
            ScrollableComponent.this.setViewOffset(ScrollableComponent.this.getOffsetIntoSource().getRow() + iLines, ScrollableComponent.this.getOffsetIntoSource().getCol());
        }

        public void left(int iCols) {
            ScrollableComponent.this.setViewOffset(ScrollableComponent.this.getOffsetIntoSource().getRow(), ScrollableComponent.this.getOffsetIntoSource().getCol() - iCols);
        }

        public void right(int iCols) {
            ScrollableComponent.this.setViewOffset(ScrollableComponent.this.getOffsetIntoSource().getRow(), ScrollableComponent.this.getOffsetIntoSource().getCol() + iCols);
        }

    }
    
    @Override
    public SearchableView asSearchableView() {
        return new SearchableView() {

            @Override
            public int getHeight() {
                return ScrollableComponent.this.getHeight();
            }

            @Override
            public void refreshFooter() {
                ScrollableComponent.this.refreshFooter();
            }

            @Override
            public int getWidth() {
                return ScrollableComponent.this.getWidth();
            }

            @Override
            public boolean scrollToView(Rectangle targetToView) {
                return ScrollableComponent.this.scrollToView(targetToView);
            }

            @Override
            public void setTitle(String message) {
                ScrollableComponent.this.setTitle(message);
            }

            @Override
            public Component<?> getComponentBeingViewed() {
                return ScrollableComponent.this.getComponentBeingViewed();
            }

            @Override
            public void setComponentBeingViewed(Component<?> component) {
                ScrollableComponent.this.setComponentBeingViewed(component);
            }

            @Override
            public Color getColor() {
                return ScrollableComponent.this.getColor();
            }

            @Override
            public void appendOnResizeRule(CanResize resizeRule) {
                ScrollableComponent.this.appendOnResizeRule(resizeRule);
            }

            @Override
            public void addFooterComponent(Component<?> component) {
                ScrollableComponent.this.addFooterComponent(component);
                // call resize to adjust scrollbars
                ScrollableComponent.this.resize((Dimension)ScrollableComponent.this);
            }

            @Override
            public WindowViewComponent getViewPort() {
                return ScrollableComponent.this.viewer;
            }
        };
    }

}
