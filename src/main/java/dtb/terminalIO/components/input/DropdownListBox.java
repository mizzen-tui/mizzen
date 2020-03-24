package dtb.terminalIO.components.input;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ComponentWithFocusManager;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.input.DisplayableValue.BooleanDisplayValue;
import dtb.terminalIO.components.input.DisplayableValue.StringDisplayValue;
import dtb.terminalIO.components.input.ListBox.ComponentItemFactory;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.StringUtils;

public class DropdownListBox<T> extends ComponentWithFocusManager  
        implements HasValue<T> , Clickable {

    private ListBox<T> listBox;
    private Button button;
    
    private int maxItemWidth = 10;
    
    protected DropdownListBox(Color color) {
        super(color, new VerticalPanel());
        
        this.button = new Button(color, "");
        this.getContainer().add(this.button);
        
        this.listBox = new ListBox<T>(color);
        this.listBox.setBorder(true);

        DefaultFocusManager fm = new DefaultFocusManager(Keys.UP_KEYSTROKE, Keys.DOWN_KEYSTROKE).setNavKeysFirst(false).setCycling(false);
        this.setFocusManager( fm );
        fm.addFocusable(button);
        fm.addFocusable(listBox);
        setSelectedOntoTextView();

        appendEventHandler( new HasValue.OnValueChangedEventHandler<T>(this.listBox) {
            @Override
            public void valueChanged(TerminalEvent<?> te, HasValue<T> source) {
                setSelectedOntoTextView();
                te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(DropdownListBox.this));
                // let everyone else know that our value has been modified.
                te.getEventBus().raiseEvent(new HasValue.ValueChangedEvent<>(DropdownListBox.this));
            }
        });

        appendEventHandler( new KeyInputEventHandler(Keys.ENTER_KEYSTROKE, (te)-> { showListBox(); }));
    }

    public DropdownListBox() {
        this(FGBGColors.NO_COLOR_SPECIFIED);
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
        return trueFalseDropDown(FGBGColors.NO_COLOR_SPECIFIED);
    }

    public static DropdownListBox<Boolean> yesNoDropDown(Color color) {
        return new DropdownListBox<Boolean>(color, 
                new BooleanDisplayValue[] {new BooleanDisplayValue("yes", true),
                                           new BooleanDisplayValue("no", false)});
    }
    
    public static DropdownListBox<Boolean> yesNoDropDown() {
        return yesNoDropDown(FGBGColors.NO_COLOR_SPECIFIED);
    }
    
    private void setMaxItemWidth(int maxItemWidth) {
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
        return this.button;
    }

    public void setSelectedValue(T value) {
        this.listBox.setSelectedValue(value);
        setSelectedOntoTextView();
    }
    
    
    private void setSelectedOntoTextView() {
        //T selected = this.listBox.getSelectedValue();
        HasValue<T> selected = this.listBox.getSelected();
        // T  selected = this.getValue();
        // buttons resize themselves based on text.
        // so we pad text to avoid this
        String buttonText;
        char emptyCH = '_';
        if (maxItemWidth < 8) {
            if (selected == null) {
                buttonText = String.format("%-"+(5)+"s", StringUtils.repeat(emptyCH, 5));
            } else {
                buttonText = String.format("%-"+(5)+"s", StringUtils.truncate((selected.toString()),5));
            }
        } else {
            boolean hasFocus = hasFocus();
            String arrow = hasFocus ? Colors.BIBla.asString() +"[v]" + ControlCode.RCol.asString() : "";
            int padLen = maxItemWidth-(hasFocus?1:0);
            if (selected == null) {
                buttonText = String.format("%-"+(padLen)+"s"+arrow, StringUtils.repeat(emptyCH, padLen));
            } else {
                buttonText = String.format("%-"+(padLen)+"s"+arrow, StringUtils.truncate(StringUtils.padRight((selected.toString()),padLen),padLen-1));
            }
        }
        
        this.button.setButtonText( buttonText );
    }
    
    

    @Override
    public T getValue() {
        return this.listBox.getValue();
    }

    protected boolean listBoxVisible() {
        if (this.getContainer().containsChild(listBox)) {
            return true;
        }
        return false;
    }
    
    private void showListBox() {
        if (listBoxVisible()) {
            return;
        }
        Logging.info("showListBox()");
        this.getContainer().add(this.listBox);
    }
    
    private void hideListBox() {
        if (!listBoxVisible()) {
            return;
        }
        Logging.info("hideListBox()");
        this.getContainer().remove(this.listBox);
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

}
