package dtb.terminalIO.components.input;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.AbstractComponentWrappper;
import dtb.terminalIO.components.HasTitleAndBorder;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.DisplayableValue.BooleanDisplayValue;
import dtb.terminalIO.components.input.DisplayableValue.StringDisplayValue;
import dtb.terminalIO.components.input.ListBox.ComponentItemFactory;
import dtb.terminalIO.components.input.ListBox.TextComponentItemFactory;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyboardHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.events.mouse.Clickable;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.StringUtils;

public class DropdownListBox<T> extends AbstractComponentWrappper<VerticalPanel>
                                implements SettableHasValueComponentMixen<AbstractComponentWrappper<VerticalPanel>, T>,
                                           HasTitleAndBorder<DropdownListBox<T>>,
                                           ToFocusable,
                                           Clickable,
                                           KeyboardHandler {

    private ListBox<T> listBox;
    private ScrollableComponent listBoxScrollPane;
    private Button button;
    private Rectangle preferredRectangleForLayout;
    private T value;

    private int maxItemWidth = 10;  // arbitrary default.
    // true while focus is being handed back from the open list to this dropdown
    // (item selected, ENTER/ESC, TAB/SHIFT-TAB) - receiving that focus must not reopen the list
    boolean returningFocusFromListBox = false;
    // ENTER / ESC / mouse pick: keep the selection, focus back to the button, close the list (see constructor)
    private final EventHandler onSelectedItemHandler;
    // focus: one TAB stop (button + open list), UP / DOWN move between them; opens / closes the list
    private final ParentFocusBehaviour focus;
    // its own "value changed" event, for onChange
    private final ChainedEventHandler events = ChainedEventHandler.useChainOfResponsibilityStrategy("DropdownListBox-events");

    protected DropdownListBox(Color color) {
        super(color, new VerticalPanel(color));
        getWrappedComponent().setName("vertical-Panel-ddlb");

        this.button = new Button(color, "");
        getWrappedComponent().add(this.button);

        
        // ENTER / ESC / mouse pick in the open list: keep the selection, give focus back to the button,
        // then close the list. (Left open, the list sat behind this dropdown's panel - brought to front
        // when focus came back - with only its bottom border row showing below the panel.)
        onSelectedItemHandler = (te) -> {
            EventBus eventBus = te.getEventBus();
            setSelectedFromListBox(eventBus);
            returningFocusFromListBox = true;
            te.getEventBus().raiseEvent(new RequestFocusEvent(DropdownListBox.this.button)
                    .then(()->{
                        returningFocusFromListBox = false;
                        hideListBox();
                        RequestUpdateCanvasEvent.invoke(eventBus, this);
                    }));
            RequestUpdateCanvasEvent.invoke(eventBus, this);
        };
        
        this.listBox = new ListBox<T>(color) {
            protected void onMouseSelected(EventBus eventBus) {
                eventBus.asynch(onSelectedItemHandler);
            }

            @Override
            protected void onUnFocus(Focusable newFocusable, EventBus eventBus) {
                // The open list lost focus. If focus went somewhere other than this dropdown
                // (e.g. a click elsewhere) close the list. Checked once the focus move has
                // finished: newFocusable is only the sibling where focus moved (often a container),
                // so it can't tell "back to this dropdown" from "elsewhere".
                eventBus.asynch((te) -> {
                    if (!dropdownOrListHasFocus()) {
                        hideListBox();
                        RequestUpdateCanvasEvent.invoke(te, DropdownListBox.this);
                    }
                });
            }
        };

        this.listBox.setBorder(true);

        // one TAB stop: the button and the open list (it counts as this dropdown's - Component.getPopupOwner);
        // UP / DOWN move between them (and open the list - see onKey)
        this.focus = ParentFocusBehaviour.of(this)
                                         .singleTabStop()
                                         .arrowsMoveInside(ArrowKeys.UP_DOWN)
                                         .handleKeys(this)
                                         .whenFocusArrives(this::focusArrived)
                                         .whenFocusLeaves(this::focusLeft)
                                         .handleEvents(events.asConditionalEventHandler());
        this.setName("DropdownListBox");

        setSelectedOntoTextView();

        this.listBox.onChange((te)->{
            // emit a new ValueChangedEvent event indicating that the 'typed' value of this 
            // outer component has changed due to the ListBox component changing
            setSelectedFromListBox(te.getEventBus());
            //te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(DropdownListBox.this));
            RequestUpdateCanvasEvent.invoke(te, DropdownListBox.this);
            //te.getEventBus().raiseEvent(new HasValue.ValueChangedEvent<T>(DropdownListBox.this));
        });

        this.button.onChange((te)->{
            showListBox();
            RequestUpdateCanvasEvent.invoke(te, this);
        });

        // ENTER on the button or in the list keeps the selection and closes the list - before their
        // own ENTER (the button's would open the list, the list's only raises "value changed").
        // ESC and the arrows: see onKey. TAB / SHIFT-TAB in the open list need nothing here: the list
        // is part of this dropdown for focus (Component.getPopupOwner), so they move on from the
        // dropdown, and the list closes when focus leaves it (onUnFocus)
        this.button.prependEventHandler(new KeyInputEventHandler(Keys.ENTER_KEYSTROKE, onSelectedItemHandler).setName("DDLB-BUTTON-ENTER"));
        this.listBox.prependEventHandler(new KeyInputEventHandler(Keys.ENTER_KEYSTROKE, onSelectedItemHandler).setName("DDLB-LISTBOX-ENTER"));

        this.preferredRectangleForLayout = new Rectangle.AbstractDelegate() {
            @Override
            public Rectangle getRectangle() {
                if (DropdownListBox.this.hasBorder()) {
                    return new Rectangle.Simple(DropdownListBox.this.getRow(),
                                                DropdownListBox.this.getCol(),
                                                DropdownListBox.this.button.getHeight()+2,
                                                DropdownListBox.this.button.getWidth()+2);
                }
                return DropdownListBox.this.button;
            }
        };

    }

    public DropdownListBox() {
        this(Colors.FGBGColors.NO_COLOR_SPECIFIED);
    }

    /**
     * Keys the button or the open list didn't use: ESC keeps the selection and closes the list
     * (focus back on the button); UP / DOWN while the list is closed open it.
     */
    @Override
    public boolean onKey(KeyEvent ke) {
        if (ke.isKeyStroke(Keys.ESC_KEYSTROKE)) {
            onSelectedItemHandler.handle(ke);
            return true;
        }
        if (opensList(ke)) {
            showListBox();
            RequestUpdateCanvasEvent.invoke(ke, this);
            return true;
        }
        return false;
    }

    @Override
    public boolean wantsKey(KeyEvent ke) {
        return ke.isKeyStroke(Keys.ESC_KEYSTROKE) || opensList(ke);
    }

    private boolean opensList(KeyEvent ke) {
        return !listBoxVisible() && (ke.isKeyStroke(Keys.UP_KEYSTROKE) || ke.isKeyStroke(Keys.DOWN_KEYSTROKE));
    }


    /**
     * Is keyboard focus in the open list - the list itself, or its scroll pane and scrollbars (e.g.
     * while dragging the scrollbar)? See focusInside.
     */
    private boolean listHasFocus() {
        if (!listBoxVisible()) {
            return false;
        }
        return focusInside(ToFocusable.UTIL.toFocusable(getListBoxOrItsScrollPane()),
                           ToFocusable.UTIL.toFocusable(this.listBox));
    }

    /** Is keyboard focus anywhere in this dropdown: its button, or its open list (and scroll pane)? */
    private boolean dropdownOrListHasFocus() {
        return focusInside(ToFocusable.UTIL.toFocusable(this),
                           ToFocusable.UTIL.toFocusable(this.button), ToFocusable.UTIL.toFocusable(this.listBox));
    }

    /**
     * Is the focused component inside node? Asks the root focus manager what is focused (individual
     * hasFocus() flags can be stale); the open list counts as part of this dropdown for focus
     * (Component.getPopupOwner), so "inside the dropdown" includes the list's scroll pane and
     * scrollbars. When the root does not know (e.g. inside a modal dialog, which moves focus on its
     * own) the flags of fallbacks answer instead.
     */
    private boolean focusInside(Focusable node, Focusable... fallbacks) {
        FocusManager root = FocusManager.UTIL.getRootFocusManager(node);
        Focusable focused = root != null ? FocusManager.UTIL.findCurrentFocus(root) : null;
        if (focused != null) {
            return FocusManager.UTIL.containsFocusable(node, focused);
        }
        for (Focusable fallback : fallbacks) {
            if (fallback != null && fallback.hasFocus()) {
                return true;
            }
        }
        return false;
    }


    private void calcAndSetMaxItemWidth(List<DisplayableValue<T>> displayableItems) {
        setMaxItemWidth( 2 + displayableItems.stream().map( (li) -> Dimension.UTIL.stringToDimension( li.toString() ).getWidth()).max( Integer::compareTo).orElse(0));   // an empty list is allowed
    }

    public DropdownListBox(DisplayableValue<T>[] listItems) {
        this();
        setItems(Arrays.asList(listItems), new TextComponentItemFactory<T>());
        calcAndSetMaxItemWidth(Arrays.asList(listItems));
    }

    public DropdownListBox(Color color, DisplayableValue<T>[] listItems, ComponentItemFactory<T> displayItemToComponent) {
        this(color);
        setItems(Arrays.asList(listItems), displayItemToComponent);
        calcAndSetMaxItemWidth(Arrays.asList(listItems));
    }

    public DropdownListBox(DisplayableValue<T>[] listItems, ComponentItemFactory<T> displayItemToComponent) {
        this();
        setItems(Arrays.asList(listItems), displayItemToComponent);
        setMaxItemWidth( Stream.of(listItems).map( (li) -> Dimension.UTIL.stringToDimension( li.toString() ).getWidth()).max( Integer::compareTo).get());
    }

    public DropdownListBox(Color color, DisplayableValue<T>[] listItems) {
        this(color);
        setItems(Arrays.asList(listItems), new TextComponentItemFactory<T>());
        calcAndSetMaxItemWidth(Arrays.asList(listItems));
    }

    public static DropdownListBox<String> fromStrings(String[] listItems) {
        return new DropdownListBox<String>(StringDisplayValue.fromStringValues(listItems));
    }

    public static DropdownListBox<String> fromStrings(String[] listItems, ComponentItemFactory<String> displayItemToComponent) {
        return new DropdownListBox<String>(StringDisplayValue.fromStringValues(listItems), displayItemToComponent);
    }

    public static DropdownListBox<String> fromStrings(Color color, String[] listItems) {
        return new DropdownListBox<String>(color, StringDisplayValue.fromStringValues(listItems));
    }

    public static DropdownListBox<String> fromStrings(Color color, String[] listItems, ComponentItemFactory<String> displayItemToComponent) {
        return new DropdownListBox<String>(color, StringDisplayValue.fromStringValues(listItems), displayItemToComponent);
    }

    public static DropdownListBox<Boolean> trueFalseDropDown(Color color) {
        return new DropdownListBox<Boolean>(color, 
                new BooleanDisplayValue[] {new BooleanDisplayValue(true),
                                           new BooleanDisplayValue(false)});
    }

    public static DropdownListBox<Boolean> trueFalseDropDown() {
        return trueFalseDropDown(Colors.FGBGColors.NO_COLOR_SPECIFIED);
    }

    public static DropdownListBox<Boolean> yesNoDropDown(Color color) {
        return new DropdownListBox<Boolean>(color, 
                new BooleanDisplayValue[] {new BooleanDisplayValue("yes", true),
                                           new BooleanDisplayValue("no", false)}).setName("Yes-No-ddlb");
    }

    public static DropdownListBox<Boolean> yesNoDropDown() {
        return yesNoDropDown(Colors.FGBGColors.NO_COLOR_SPECIFIED);
    }

    @Override
    public Focusable asFocusable() {
        return focus.asFocusable();
    }

    /** does the dropdown (its button or its open list) have focus? */
    public boolean hasFocus() {
        return focus.hasFocus();
    }

    // ---- title and border: its panel's ------------------------------------------------------

    @Override
    public boolean hasBorder() {
        return getWrappedComponent().hasBorder();
    }

    @Override
    public DropdownListBox<T> setBorder(boolean hasBorder) {
        getWrappedComponent().setBorder(hasBorder);
        return this;
    }

    @Override
    public DropdownListBox<T> setBorderColor(Color borderColors) {
        getWrappedComponent().setBorderColor(borderColors);
        return this;
    }

    @Override
    public DropdownListBox<T> setTitle(Color titleColors, String title) {
        getWrappedComponent().setTitle(titleColors, title);
        return this;
    }

    @Override
    public DropdownListBox<T> setTitle(String title) {
        getWrappedComponent().setTitle(title);
        return this;
    }

    @Override
    public String getTitle() {
        return getWrappedComponent().getTitle();
    }

    @Override
    public void hilite() {
        getWrappedComponent().hilite();
    }

    @Override
    public void unHilite() {
        getWrappedComponent().unHilite();
    }

    public DropdownListBox<T> setName(String name) {
        super.setName(name);
        if (this.listBox != null) {
            this.listBox.setName(name+"-listBox");
        }
        if (this.listBoxScrollPane != null) {
            this.listBoxScrollPane.setName(name+"-listBoxScrollPane");
        }
        if (this.button != null) {
            this.button.setName(name+"-button");
        }
        return this;
    }
    
    @Override
    public DropdownListBox<T> setColor(Color drawingcolor) {
        super.setColor(drawingcolor);
        if (this.listBox != null) {
            this.listBox.setColor(drawingcolor);
        }
        if (this.button != null) {
            this.button.setColor(drawingcolor);
        }
        return this;
    }

    public void setParentContainer(ParentContainer parentContainer) {
        if (parentContainer == null) {
            this.hideListBox();
        }
        super.setParentContainer(parentContainer);
    }

    public void setMaxItemWidth(int maxItemWidth) {
        if (maxItemWidth<1) {
            throw new IllegalArgumentException("bad max width specified :"+maxItemWidth);
        }
        if (this.maxItemWidth != maxItemWidth) {
            this.markAsDirty();
        }
        this.maxItemWidth = maxItemWidth;
        setSelectedOntoTextView();
    }

    public void setItems(List<DisplayableValue<T>> displayableItems) {
        this.listBox.setItems(displayableItems);
        this.button.resize( new Dimension.Simple(this.button.getHeight(), this.listBox.getWidth()) );
    }

    public void setItems( List<DisplayableValue<T>> displayableItems , ComponentItemFactory<T> displayItemToComponent) {
        this.listBox.setItems(displayableItems, displayItemToComponent);
        this.button.resize( new Dimension.Simple(this.button.getHeight(),  this.listBox.getWidth()) );
    }

    public Rectangle getPreferredRectangleForLayout() {
        return preferredRectangleForLayout;
    }

    public boolean setSelected(int index) {
        boolean result = this.listBox.setSelected(index);
        setSelectedOntoTextView();
        this.value = this.listBox.getValue();
        return result;
    }

    public void setSelectedValue(T value) {
        this.value = value;
        if (value != null) {
           this.listBox.setSelectedValue(value);
           setSelectedOntoTextView();
        } else {
            setDisplayableValueOntoTextView(null);
        }
    }

    public void setValue(T value) {
        setSelectedValue(value);
    }

    private void setSelectedFromListBox(EventBus eventBus) {
        setSelectedOntoTextView();
        this.value = this.listBox.getValue();
        if (this.value != null) {
           eventBus.raiseEvent(new HasValue.ValueChangedEvent<T>(DropdownListBox.this));
        }
    }

    private void setSelectedOntoTextView() {
        setDisplayableValueOntoTextView(this.listBox.getSelected());
    }

    private void setDisplayableValueOntoTextView(DisplayableValue<T> selected) {

        // buttons resize themselves based on text.
        // so we pad text to avoid this
        String buttonText;
        char emptyCH = '_';
        if (maxItemWidth < 8) {
            if (selected == null) {
                buttonText = String.format("%-"+(5)+"s", StringUtils.repeat(emptyCH, 5));
            } else {
                buttonText = String.format("%-"+(5)+"s", StringUtils.truncate((selected.getDisplayValue()),5));
            }
        } else {
            boolean hasFocus = hasFocus();
//            Color arrowColor = hasFocus ? getColor().bold() : Colors.BIBla;
//            String arrow = arrowColor.asString() +"[v]" + getColor().asString();
            String arrow =  "[v]" ;
            int padLen = maxItemWidth; //-(hasFocus?1:0);
            if (selected == null) {
                buttonText = String.format("%-"+(padLen)+"s"+arrow, StringUtils.repeat(emptyCH, padLen));
            } else {
                buttonText = String.format("%-"+(padLen)+"s"+arrow, StringUtils.truncate(StringUtils.padRight((selected.getDisplayValue()),padLen),padLen-1));
            }
        }

        this.button.setButtonText( buttonText );
        this.markAsDirty();
    }

    @Override
    public T getValue() {
        return value;
    }

    private Composite getListBoxRootPanel() {
        Component<?> rootPanel = Component.UTIL.getRootPanelOf(this);
        if (rootPanel != null && rootPanel instanceof Composite) {
            return (Composite) rootPanel;
        }
        return null;
    }

    protected Composite getListBoxParent() {
        if (this.getListBoxOrItsScrollPane() != null) {
            if (this.getListBoxOrItsScrollPane().getParentContainer() != null) {
                return (Composite) this.getListBoxOrItsScrollPane().getParentContainer();
            }
        }

        Composite rootPanel = getListBoxRootPanel();
        if (rootPanel != null) {
            return rootPanel;
        }

        return getWrappedComponent();
    }


    protected boolean listBoxVisible() {
        Composite listBoxParent = this.getListBoxParent();
        if ( listBoxParent.containsChild(getListBoxOrItsScrollPane())) {
            return true;
        }
        return false;
    }

    private Component<?> getListBoxOrItsScrollPane() {
        if (this.listBoxScrollPane != null) {
            return this.listBoxScrollPane;
        }
        return this.listBox;
    }
    
    protected boolean listBoxVisibleAndOnTop() {
        Composite listBoxParent = this.getListBoxParent();
        if ( listBoxParent.containsChild(getListBoxOrItsScrollPane()) && listBoxParent.isChildOnTop(getListBoxOrItsScrollPane())) {
            return true;
        }
        return false;
    }

    public void showListBox() {
        if (listBoxVisibleAndOnTop()) {
            return;
        }
        Logging.info("showListBox()");

        Composite listBoxParent = this.getListBoxParent();

        // sometimes we do have the listbox in the parent - but it is not at the top ..
        if ( listBoxParent.containsChild(getListBoxOrItsScrollPane()) ) {
            listBoxParent.bringToFront(this.getListBoxOrItsScrollPane());
            return;
        }

        if (listBoxParent == getWrappedComponent()) {
            this.getListBoxParent().add(this.getListBoxOrItsScrollPane());
        } else {
            // if we can, we place the listBox on the root panel and bring to the front ..
            // this ensures that the full drop down gets displayed without distorting
            // internal container ( VerticalPanel ) size..
            Position thisPositionOnRootPanel = Component.UTIL.getCanvasOffsetFor(this);
            Position listBoxPosition = thisPositionOnRootPanel.offsetAdd(this.getHeight()+(this.hasBorder()?-1:0),(this.hasBorder()?1:0));
            if (listBoxParent.hasBorder()) {
                listBoxPosition = listBoxPosition.offsetAdd(-1,-1);
            }
            Rectangle listBoxRect = new Rectangle.Simple(listBoxPosition, listBox);
            if (!listBoxParent.contains(listBoxRect)) {
                Logging.info("ListBOx does not fit");
                
                Rectangle minScrollPaneRectangleSize = new Rectangle.Simple(listBoxPosition, 6, listBox.getWidth());
                if (!listBoxParent.contains(minScrollPaneRectangleSize)) {
                    Logging.info("ListBOx does not fit - need to reposition");
                } else {
                    Rectangle desiredRect = listBoxParent.intersectWith(new Rectangle.Simple(listBoxPosition, listBox));
                    listBox.setBorder(false);
                    this.listBoxScrollPane = new ScrollableComponent(listBox.getColor(), new Dimension.Simple(desiredRect), listBox)
                                                                .removeFooter();
                    listBoxScrollPane.setName(this.getName()+"-listBoxScrollPane");
                }
            }

            // drawn on the root panel, but part of this dropdown for keyboard focus
            Component.UTIL.setPopupOwner(this.getListBoxOrItsScrollPane(), this);
            listBoxParent.add(listBoxPosition, this.getListBoxOrItsScrollPane());
            listBoxParent.bringToFront(this.getListBoxOrItsScrollPane());
            Logging.info("ListBOx added to parent");
        }
    }

    public void hideListBox() {
        if (!listBoxVisible()) {
            return;
        }
        Logging.info("hideListBox()");
        this.getListBoxParent().remove(this.getListBoxOrItsScrollPane());
        if (this.listBoxScrollPane != null) {
            this.listBox.setParentContainer(null);
            this.listBox.setBorder(true);
            this.listBoxScrollPane = null;
        }
    }

    protected void toggleShowHide() {
        if (listBoxVisible()) {
            hideListBox();
        } else {
            showListBox();
        }
    }

    /**
     * Focus arrived at the dropdown: open the list (on top) and move focus into it - unless focus is
     * only coming back from the list (an item was picked: see onSelectedItemHandler).
     */
    private void focusArrived(EventBus eventBus) {
        setSelectedOntoTextView();
        // note: important - bring this component to front first, then showListBox(): the list ends up on top
        Component.UTIL.bringToFront(DropdownListBox.this);
        if (!returningFocusFromListBox) {
            openListAndEnterIt(eventBus);
        }
    }

    /** focus left the dropdown and its list: close the list */
    private void focusLeft(EventBus eventBus) {
        setSelectedOntoTextView();
        hideListBox();
    }

    /** show the list (on top) and move focus into it - unless focus is in it already (then only repaint) */
    private void openListAndEnterIt(EventBus eventBus) {
        boolean alreadyInList = listHasFocus();
        showListBox();
        if (!isDirty()) {
            this.markAsDirty();
        }
        if (alreadyInList) {
            eventBus.raiseEvent(new RequestUpdateCanvasEvent(this));
        } else {
            eventBus.raiseEvent(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(this.listBox))
                                      .then(() -> eventBus.raiseEvent(new RequestUpdateCanvasEvent(this))));
        }
    }

    /** a click on the dropdown: focus it (which opens the list), or - if it has focus - open the list */
    @Override
    public void onClick(EventBus eventBus) {
        if (focus.hasFocus()) {
            Component.UTIL.bringToFront(DropdownListBox.this);
            openListAndEnterIt(eventBus);
        } else {
            eventBus.raiseEvent(new RequestFocusEvent(this.button));
        }
    }

    HasValue.OnValueChangedEventHandler<T> onChangeHandler;

    public void onChange(EventHandler consumeChange) {
        onChangeHandler = HasValue.UTIL.bindConsumerAsOnValueChangedHandler(this, onChangeHandler, events, consumeChange);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append(": "+this.button.getButtonText());
        return sb.toString();
    }

}
