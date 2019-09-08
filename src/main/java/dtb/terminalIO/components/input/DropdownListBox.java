package dtb.terminalIO.components.input;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import dtb.terminalIO.components.AbstractComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.input.DisplayableValue.BooleanDisplayValue;
import dtb.terminalIO.components.input.DisplayableValue.StringDisplayValue;
import dtb.terminalIO.components.input.ListBox.ComponentItemFactory;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.mouse.GetComponentAtOffset;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

public class DropdownListBox<T> extends AbstractComponent<DropdownListBox<T>>  
        implements HasValue<T>, Focusable, ParentContainer, GetComponentAtOffset {

    private VerticalPanel container;
    private ListBox<T> listBox;
    private Button button;
    
    private Focusable meAsFocusable = null;
    private int maxItemWidth = 10;
    
    protected DropdownListBox(Color color) {
        super(color);
        
        this.container = new VerticalPanel();
        this.container.setParentContainer(this);

        this.button = new Button(color, "");
        this.button.setBorder(true);
        this.container.add(this.button);
        
        this.listBox = new ListBox<T>(color);
        this.listBox.setBorder(true);
    }
    
    public DropdownListBox() {
        this(FGBGColors.NO_COLOR_SPECIFIED);
    }

    public DropdownListBox(DisplayableValue<T>[] listItems) {
        this();
        setItems(Arrays.asList(listItems));
    }
    
    public DropdownListBox(Color color, DisplayableValue<T>[] listItems, ComponentItemFactory<T> displayItemToComponent) {
        this(color);
        setItems(Arrays.asList(listItems), displayItemToComponent);
    }

    public DropdownListBox(DisplayableValue<T>[] listItems, ComponentItemFactory<T> displayItemToComponent) {
        this();
        setItems(Arrays.asList(listItems), displayItemToComponent);
        maxItemWidth = 1 + Stream.of(listItems).map( (li) -> Dimension.UTIL.stringToDimension( li.toString() ).getWidth()).max( Integer::compareTo).get();
    }

    public DropdownListBox(Color color, DisplayableValue<T>[] listItems) {
        this(color);
        setItems(Arrays.asList(listItems));
        maxItemWidth = 1 + Stream.of(listItems).map( (li) -> Dimension.UTIL.stringToDimension( li.toString() ).getWidth()).max( Integer::compareTo).get();
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

    protected Component<?> getVisibleChild() {
        boolean listBoxVisible = this.container.containsChild(listBox);
        if (asFocusable().hasFocus()) {
            if (!listBoxVisible) {
                synchronized(this) {
                    if ( ! this.container.containsChild(listBox)) { // check again
                        this.container.add(this.listBox);
                    }
                }
            }
        } else {
            if (listBoxVisible) {
                synchronized(this) {
                    if (this.container.containsChild(listBox)) { // check again
                        this.container.remove(this.listBox);
                    }
                }
            }
        }
        return container;
    }

    @Override
    public Collection<? extends Component<?>> getChildren() {
        return Collections.singleton(getVisibleChild());
    }

    @Override
    public int getCol() {
        return getVisibleChild().getCol();
    }

    @Override
    public int getRow() {
        return getVisibleChild().getRow();
    }

    @Override
    public int getWidth() {
        return getVisibleChild().getWidth();
    }

    @Override
    public int getHeight() {
        return getVisibleChild().getHeight();
    }

    @Override
    public void moveTo(int row, int col) {
        getVisibleChild().moveTo(row, col);
    }

    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        return Position.ZERO;
    }

    @Override
    public void childResized(Component<?> child) {
        this.markAsDirty();
    }

    private void setSelectedOntoTextView() {
        HasValue<T> selected = this.listBox.getSelected();
        // T  selected = this.getValue();
        // buttons resize themselves based on text.
        // so we pad text to avoid this
        String buttonText;
        if (selected == null) {
            buttonText = String.format("%-"+maxItemWidth+"s", " ");
        } else {
            buttonText = String.format("%-"+maxItemWidth+"s", selected.toString());
        }
        
        this.button.setButtonText( buttonText );
    }
    
    
    private Focusable asFocusable() {
        if (meAsFocusable != null) {
            return meAsFocusable;
        }
        synchronized (this) {
            if (meAsFocusable != null) {
                return meAsFocusable;
            }
            Focusable lisBoxFocusable = this.listBox.asFocusable();

            ChainedEventHandler chainedHandler = ChainedEventHandler.useChainOfResponsibilityStrategy();

            chainedHandler.appendEventHandler(lisBoxFocusable); 
            
            chainedHandler.appendEventHandler( new HasValue.OnValueChangedEventHandler<T>(this.listBox) {
                @Override
                public void valueChanged(TerminalEvent<?> te, HasValue<T> source) {
                    setSelectedOntoTextView();
                }
            });

            meAsFocusable = new DefaultFocusableBehaviour.Simple(this, chainedHandler.asConditionalEventHandler()) {
                @Override
                public void setFocus(boolean focus, EventBus eventBus) {
                    super.setFocus(focus, eventBus);
                    if (focus) {
                        button.setFocus(focus, eventBus);
                        lisBoxFocusable.setFocus(focus, eventBus);
                        setSelectedOntoTextView();
                        Component.UTIL.bringToFront(DropdownListBox.this);
                    } else {
                        if (!focus) {
                            button.setFocus(focus, eventBus);
                            lisBoxFocusable.setFocus(focus, eventBus);
                            setSelectedOntoTextView();
                       }
                    }
                    markAsDirty();
                }
            };
        }
        return meAsFocusable;
    }

    @Override
    public T getValue() {
        return this.listBox.getValue();
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.merge(this.getVisibleChild().getSegmentsBuffer());
    }

    public Component<?> getComponentAtOffset(Position offset) {
        Component<?> component = this.container.getComponentAtOffset(offset);
        if (component == this.container) {
            return this;
        }
        return component;
    }
    
    
    @Override
    protected Rectangle getRectangleForClearAndFill() {
        return getVisibleChild();
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if (asFocusable().canHandle(te) != null) {
            return this;
        }
        return null;
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        asFocusable().handle(te);
    }

    @Override
    public boolean hasFocus() {
        return asFocusable().hasFocus();
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        asFocusable().setFocus(focus, eventBus);
    }
}
