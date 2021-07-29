package dtb.terminalIO.components.input;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ComponentWithFocusManager;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.input.DisplayableValue.BooleanDisplayValue;
import dtb.terminalIO.components.input.DisplayableValue.StringDisplayValue;
import dtb.terminalIO.components.input.ListBox.ComponentItemFactory;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.events.mouse.Clickable;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.StringUtils;

public class DropdownListBox<T> extends ComponentWithFocusManager<DropdownListBox<T>>  
                                implements HasValue<T> , 
                                           Clickable {

    private ListBox<T> listBox;
    private Button button;
    private Rectangle preferredRectangleForLayout;

    private int maxItemWidth = 10;  // arbitrary default.

    protected DropdownListBox(Color color) {
        super(color, new VerticalPanel());

        this.button = new Button(color, "");
        this.getContainer().add(this.button);

        this.listBox = new ListBox<T>(color);
        this.listBox.setBorder(true);

        DefaultFocusManager fm = new DefaultFocusManager(Keys.UP_KEYSTROKE, Keys.DOWN_KEYSTROKE).setNavKeysFirst(false)
                                                                                                .setCycling(false);
        this.setFocusManager( fm );
        fm.addFocusable(button);
        fm.addFocusable(listBox);

        setSelectedOntoTextView();

        this.listBox.onChange((te)->{
            // emit a new ValueChangedEvent event indicating that the 'typed' value of this 
            // outer component has changed due to the ListBox component changing
            setSelectedOntoTextView();
            te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(DropdownListBox.this));
            te.getEventBus().raiseEvent(new HasValue.ValueChangedEvent<T>(DropdownListBox.this));
        });

        appendEventHandler( new KeyInputEventHandler(Keys.ENTER_KEYSTROKE, (te)-> { toggleShowHide(); }));

        appendEventHandler( new KeyInputEventHandler(Keys.ESC_KEYSTROKE, (te)-> { hideListBox(); }));
        
        this.preferredRectangleForLayout = new Rectangle.AbstractDelegate() {

            @Override
            public Rectangle getRectangle() {
                return DropdownListBox.this.button;
            }
            
            @Override
            public int getWidth() {
                return super.getWidth() + (hasBorder() ? 2 : 0);
            }

            @Override
            public int getHeight() {
                return super.getHeight() + (hasBorder() ? 2 : 0);
            }

        };

    }

    public DropdownListBox() {
        this(Colors.FGBGColors.NO_COLOR_SPECIFIED);
    }

    private void calcAndSetMaxItemWidth(List<DisplayableValue<T>> displayableItems) {
        setMaxItemWidth( 4 + displayableItems.stream().map( (li) -> Dimension.UTIL.stringToDimension( li.toString() ).getWidth()).max( Integer::compareTo).get());
    }
    
    public DropdownListBox(DisplayableValue<T>[] listItems) {
        this();
        setItems(Arrays.asList(listItems));
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
        setItems(Arrays.asList(listItems));
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
                                           new BooleanDisplayValue("no", false)});
    }

    public static DropdownListBox<Boolean> yesNoDropDown() {
        return yesNoDropDown(Colors.FGBGColors.NO_COLOR_SPECIFIED);
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
        return result;
    }

    public void setSelectedValue(T value) {
        this.listBox.setSelectedValue(value);
        setSelectedOntoTextView();
    }

    private void setSelectedOntoTextView() {

        DisplayableValue<T> selected = this.listBox.getSelected();
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
            String arrow = hasFocus ? Colors.BIBla.asString() +"[v]" + Colors.RCol.asString() : "";
            int padLen = maxItemWidth-(hasFocus?1:0);
            if (selected == null) {
                buttonText = String.format("%-"+(padLen)+"s"+arrow, StringUtils.repeat(emptyCH, padLen));
            } else {
                buttonText = String.format("%-"+(padLen)+"s"+arrow, StringUtils.truncate(StringUtils.padRight((selected.getDisplayValue()),padLen),padLen-1));
            }
        }

        this.button.setButtonText( buttonText );
    }


    @Override
    public T getValue() {
        return this.listBox.getValue();
    }

    private Composite getListBoxRootPanel() {
        
        Component<?> rootPanel = Component.UTIL.getRootPanelOf(this);
        if (rootPanel != null && rootPanel instanceof Composite) {
            return (Composite) rootPanel;
        }

        return null;

    }
    
    protected Composite getListBoxParent() {

        Composite rootPanel = getListBoxRootPanel();
        if (rootPanel != null) {
            return rootPanel;
        }

        return this.getContainer();
    }


    protected boolean listBoxVisible() {
        if (this.getListBoxParent().containsChild(listBox)) {
            return true;
        }
        return false;
    }
    
    public void showListBox() {
        if (listBoxVisible()) {
            return;
        }
        Composite listBoxParent = this.getListBoxParent();
        if (listBoxParent == getContainer()) {
            this.getListBoxParent().add(this.listBox);
        } else {
            // if we can, we place the listBox on the root panel and bring to the front..
            // this ensures that the full drop down gets displayed without distorting
            // internal container ( VerticalPanel ) size..
            Position thisPositionOnRootPanel = Component.UTIL.getCanvasOffsetFor(this);
            // I'm confused : I don't know why I need to also add -1 to row/col
            Position listBoxPosition = thisPositionOnRootPanel.offsetAdd(this.getHeight()-1,-1);
            listBoxParent.add(listBoxPosition, this.listBox);
            listBoxParent.bringToFront(this.listBox);
        }
    }
    
    public void hideListBox() {
        if (!listBoxVisible()) {
            return;
        }
        this.getListBoxParent().remove(this.listBox);
    }
    
    protected void toggleShowHide() {
        if (listBoxVisible()) {
            hide();
        } else {
            show();
        }
    }
    
    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        super.setFocus(focus, eventBus);
        if (focus) {
            showListBox();
        } 

        if (!focus) {
            hideListBox();
        }
        setSelectedOntoTextView();
        Component.UTIL.bringToFront(DropdownListBox.this);
    }

    @Override
    public void onClick(EventBus eventBus) {
        this.setFocus(true, eventBus);
    }
    
    HasValue.OnValueChangedEventHandler<T> onChangeHandler;
    
    public void onChange(EventHandler consumeChange) {

        onChangeHandler = HasValue.UTIL.bindConsumerAsOnValueChangedHandler(this, onChangeHandler, this, consumeChange);

    }

}
