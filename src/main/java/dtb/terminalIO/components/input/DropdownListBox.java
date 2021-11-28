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
import dtb.terminalIO.components.input.ListBox.TextComponentItemFactory;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
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
    private T value;

    private int maxItemWidth = 10;  // arbitrary default.

    protected DropdownListBox(Color color) {
        super(color, new VerticalPanel(color));

        this.button = new Button(color, "");
        this.getContainer().add(this.button);

        this.listBox = new ListBox<T>(color) {
            protected void onMouseSelected(EventBus eventBus) {
                eventBus.appendEventHandler(new OnIdleEventEventHandler.OneTime((te)->{
                    hideListBox();
                    setSelectedFromListBox(eventBus);
                    RequestUpdateCanvasEvent.invoke(eventBus, this);
                }));
            }
        };
        this.listBox.setBorder(true);

        DefaultFocusManager fm = new DefaultFocusManager(Keys.UP_KEYSTROKE, Keys.DOWN_KEYSTROKE).setNavKeysFirst(false)
                                                                                                .setCycling(false);
        this.setFocusManager( fm );
        fm.addFocusable(button);
        fm.addFocusable(listBox);

        setSelectedOntoTextView();

//        this.listBox.onChange((te)->{
//            // emit a new ValueChangedEvent event indicating that the 'typed' value of this 
//            // outer component has changed due to the ListBox component changing
//            setSelectedOntoTextView();
//            te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(DropdownListBox.this));
//            //te.getEventBus().raiseEvent(new HasValue.ValueChangedEvent<T>(DropdownListBox.this));
//        });
        
        this.button.onChange((te)->{
            showListBox();
            RequestUpdateCanvasEvent.invoke(te, this);
        });
        

        appendEventHandler( new KeyInputEventHandler(Keys.ENTER_KEYSTROKE, (te)-> { 
                                                                                    hideListBox(); 
                                                                                    setSelectedFromListBox(te.getEventBus());
                                                                                    RequestUpdateCanvasEvent.invoke(te, this);
                                                                                  }));

        appendEventHandler( new KeyInputEventHandler(Keys.ESC_KEYSTROKE, (te)-> { 
                                                                                    hideListBox();
                                                                                    setSelectedValue(this.getValue());
                                                                                    RequestUpdateCanvasEvent.invoke(te, this);
                                                                                }));

        appendEventHandler( new KeyInputEventHandler(new KeyStroke[] {Keys.UP_KEYSTROKE, Keys.DOWN_KEYSTROKE},
                                                     (te)-> ! listBoxVisible(),
                                                     (te)-> { 
                                                        showListBox(); 
                                                        RequestUpdateCanvasEvent.invoke(te, this);
                                                     }));
        
        
        this.preferredRectangleForLayout = new Rectangle.AbstractDelegate() {

            @Override
            public Rectangle getRectangle() {
                return DropdownListBox.this.button;
            }
            
//            @Override
//            public int getWidth() {
//                return super.getWidth() + 4 + (hasBorder() ? 2 : 0);
//            }

//            @Override
//            public int getHeight() {
//                return super.getHeight() + (hasBorder() ? 2 : 0);
//            }

        };

    }

    public DropdownListBox() {
        this(Colors.FGBGColors.NO_COLOR_SPECIFIED);
    }

    private void calcAndSetMaxItemWidth(List<DisplayableValue<T>> displayableItems) {
        setMaxItemWidth( 2 + displayableItems.stream().map( (li) -> Dimension.UTIL.stringToDimension( li.toString() ).getWidth()).max( Integer::compareTo).get());
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
            Color arrowColor = hasFocus ? getColor().bold() : Colors.BIBla;
            String arrow = arrowColor.asString() +"[v]" + getColor().asString();
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

        Composite rootPanel = getListBoxRootPanel();
        if (rootPanel != null) {
            return rootPanel;
        }

        return this.getContainer();
    }


    protected boolean listBoxVisible() {
        Composite listBoxParent = this.getListBoxParent();
        if ( listBoxParent.containsChild(listBox)) {
            return true;
        }
        return false;
    }

    protected boolean listBoxVisibleAndOnTop() {
        Composite listBoxParent = this.getListBoxParent();
        if ( listBoxParent.containsChild(listBox) && listBoxParent.isChildOnTop(listBox)) {
            return true;
        }
        return false;
    }
    
    public void showListBox() {
        if (listBoxVisibleAndOnTop()) {
            return;
        }
        Composite listBoxParent = this.getListBoxParent();

        // sometimes we do have the listbox in the parent - but it is not at the top ..
        if ( listBoxParent.containsChild(listBox) ) {
            listBoxParent.bringToFront(this.listBox);
            return;
        }
        
        if (listBoxParent == getContainer()) {
            this.getListBoxParent().add(this.listBox);
        } else {
            // if we can, we place the listBox on the root panel and bring to the front..
            // this ensures that the full drop down gets displayed without distorting
            // internal container ( VerticalPanel ) size..
            Position thisPositionOnRootPanel = Component.UTIL.getCanvasOffsetFor(this);
            Position listBoxPosition = thisPositionOnRootPanel.offsetAdd(this.getHeight(),0);
            if (listBoxParent.hasBorder()) {
                listBoxPosition = listBoxPosition.offsetAdd(-1,-1);
            }
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
            hideListBox();
        } else {
            showListBox();
        }
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        super.setFocus(focus, eventBus);
        setSelectedOntoTextView();
        if (focus) {
            showListBox();
        } 

        if (!focus) {
            hideListBox();
        }
        Component.UTIL.bringToFront(DropdownListBox.this);
    }

    @Override
    public void onClick(EventBus eventBus) {
        this.setFocus(true, eventBus);
        RequestUpdateCanvasEvent.invoke(eventBus, this);
    }

    HasValue.OnValueChangedEventHandler<T> onChangeHandler;

    public void onChange(EventHandler consumeChange) {
        onChangeHandler = HasValue.UTIL.bindConsumerAsOnValueChangedHandler(this, onChangeHandler, this, consumeChange);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append(": "+this.button.getButtonText());
        return sb.toString();
    }

}
