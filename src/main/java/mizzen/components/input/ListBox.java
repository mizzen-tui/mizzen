package mizzen.components.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import mizzen.components.AbstractComponent;
import mizzen.components.BorderedComponent;
import mizzen.components.CharComponent;
import mizzen.components.Component;
import mizzen.components.GetComponentAtOffset;
import mizzen.components.HasName;
import mizzen.components.HiliterAdapter;
import mizzen.components.ParentContainer;
import mizzen.components.TextComponent;
import mizzen.components.composite.Composite;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.DisplayableValue.StringDisplayValue;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.ChainedEventHandler;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.EventHandlerContainer;
import mizzen.events.RequestUpdateCanvasEvent;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.KeyInputEventHandler;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.Keys;
import mizzen.events.mouse.ApplicationMouseEvent;
import mizzen.events.mouse.Clickable;
import mizzen.events.mouse.MOUSE_ACTION;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.util.StringUtils;

/**
 * Presents a list of items to user to select from.
 * 
 * <p/>
 * 
 * User the {@link #onChange} method to detect and 'handle' selection changed events
 * 
 * @author dbalme
 *
 * @param <T>
 */
public class ListBox<T> extends AbstractComponent<ListBox<T>>  
                                implements FormInput<T>, 
                                           ToFocusable, 
                                           ParentContainer, 
                                           GetComponentAtOffset,
                                           Clickable,
                                           EventHandlerContainer {

    static boolean FILL_ON_CLEAR_DEFAULT = true;
    
    List<ListItem> items = new ArrayList<ListItem>();
    VerticalPanel  itemsContainer;
    int            selected;
    KeyStroke      keystrokePrevious       = Keys.UP_KEYSTROKE;
    KeyStroke      keystrokeNext           = Keys.DOWN_KEYSTROKE;
    Color          selectedColor           = new Colors.FGBGColors(Colors.IYel);
    int            eventTriggerForValueChanged = DEFAULT_TRIGGER_FOR_ONVALUE_CHANGED;
    
    // VALUE_CHANGED_ON_... flags (setEventTriggerForValueChanged): see HasValue

    public static interface ItemComponent  {
        Component<?> getComponent();
        void selectWithFocus();
        void selectWithNoFocus();
        default void selectWithNoFocus(Focusable newFocusable) {
            this.selectWithNoFocus();
        }
        void unSelect();
    }


    // Components that are rendered in the list
    public static abstract class AbstractItemComponent  implements ItemComponent {
         Component<?> component;

         public Component<?> getComponent() {
            return component;
        }

        AbstractItemComponent(Component<?> component) {
             this.component = component;
         }

         abstract public void selectWithFocus();
         abstract public void selectWithNoFocus();
         abstract public void unSelect();
    }

    static class ListItemComponent<T> extends Composite {

        TextComponent tc;
        CharComponent bullet;

        ListItemComponent(Color color, int maxWidthStr, DisplayableValue<T> displayableItem) {
            super(color);

            Dimension strDim = Dimension.UTIL.stringToDimension( displayableItem.getDisplayValue());

            Dimension tcDim = new Dimension.Simple( strDim.getHeight(), maxWidthStr ); 
            tc = new TextComponent(this.getColor(), tcDim, displayableItem.getDisplayValue());
            tc.setFillOnClear(FILL_ON_CLEAR_DEFAULT);

            bullet = new CharComponent( this.getColor() , '-');

            this.add( 0,0, bullet);
            this.add( 0,2, tc);
        }

        public void setBulletChar(char ch) {
            this.bullet.setChar(ch);
        }

        public void setUnSelectedBullet() {
            this.bullet.setChar('□');
        }

        public void setSelectedBullet() {
            this.bullet.setChar('■');
        }

    }

    ItemComponent itemComponentfactory(Component<?> component) {
             if (component instanceof ItemComponent) {
                 return (ItemComponent) component;
             }
             if (component instanceof TextComponent) {
                 return new AbstractItemComponent(component) {

                    @Override
                    public void selectWithFocus() {
                        //new HiliterAdapter<Component<?>>(getComponent()).getHiliter().hilite();
                        getComponent().setColor(ListBox.this.selectedColor.invert());
                    }

                    @Override
                    public void unSelect() {
                        //new HiliterAdapter<Component<?>>(getComponent()).getHiliter().unHilite();
                        getComponent().setColor(ListBox.this.getColor());
                    }

                    @Override
                    public void selectWithNoFocus() {
                        getComponent().setColor(ListBox.this.selectedColor.bold());
                    }
                 };
             }

             if (component instanceof BorderedComponent) {
                 return new AbstractItemComponent(component) {

                    @Override
                    public void selectWithFocus() {
                        new HiliterAdapter<Component<?>>(getComponent()).getHiliter().hilite();
                        getComponent().setColor(selectedColor);
                        ((BorderedComponent)getComponent()).setShowBorder(true);
                    }

                    @Override
                    public void unSelect() {
                        
                        new HiliterAdapter<Component<?>>(getComponent()).getHiliter().unHilite();
                        getComponent().setColor(ListBox.this.getColor());
                        ((BorderedComponent)getComponent()).setShowBorder(false);
                    }

                    @Override
                    public void selectWithNoFocus() {
                        getComponent().setColor(selectedColor);
                        ((BorderedComponent)getComponent()).setShowBorder(false);
                    }
                     
                 };
             }

             if (component instanceof ListItemComponent) {
                 return new AbstractItemComponent(component) {

                    @Override
                    public void selectWithFocus() {
                        ListItemComponent<?> component = (ListItemComponent<?>)getComponent();
                        new HiliterAdapter<Component<?>>(component).getHiliter().hilite();
                        component.setColor(selectedColor);
                        component.setBorder(true);
                        component.setSelectedBullet();
                    }

                    @Override
                    public void unSelect() {
                        ListItemComponent<?> component = (ListItemComponent<?>)getComponent();
                        new HiliterAdapter<Component<?>>(component).getHiliter().unHilite();
                        component.setColor(ListBox.this.getColor());
                        component.setBorder(false);
                        component.setUnSelectedBullet();
                    }

                    @Override
                    public void selectWithNoFocus() {
                        ListItemComponent<?> component = (ListItemComponent<?>)getComponent();
                        new HiliterAdapter<Component<?>>(component).getHiliter().hilite();
                        component.setColor(selectedColor);
                        component.setBorder(false);
                        component.setSelectedBullet();
                    }
                 };
             }

             if (component instanceof Composite) {
                 return new AbstractItemComponent(component) {

                    @Override
                    public void selectWithFocus() {
                        new HiliterAdapter<Component<?>>(getComponent()).getHiliter().hilite();
                        getComponent().setColor(selectedColor);
                        ((Composite)getComponent()).setBorder(true);
                    }

                    @Override
                    public void unSelect() {
                        new HiliterAdapter<Component<?>>(getComponent()).getHiliter().unHilite();
                        getComponent().setColor(ListBox.this.getColor());
                        ((Composite)getComponent()).setBorder(false);
                    }

                    @Override
                    public void selectWithNoFocus() {
                        new HiliterAdapter<Component<?>>(getComponent()).getHiliter().hilite();
                        getComponent().setColor(selectedColor);
                        ((Composite)getComponent()).setBorder(false);
                    }
                 };
             }
             return new AbstractItemComponent(component) {

                 @Override
                 public void selectWithFocus() {
                     new HiliterAdapter<Component<?>>(getComponent()).getHiliter().hilite();
                 }

                 @Override
                 public void unSelect() {
                     new HiliterAdapter<Component<?>>(getComponent()).getHiliter().unHilite();
                 }

                @Override
                public void selectWithNoFocus() {
                    selectWithFocus();
                }
              };
    }

    // DisplayValue ItemComponent pair - this is what is managed by the ListBox
    class ListItem {

        private DisplayableValue<T> hasValue;
        private ItemComponent  itemComponent;
                    
        public ListItem(DisplayableValue<T> hasValue, Component<?> component) {
            super();
            this.hasValue = hasValue;
            this.itemComponent = itemComponentfactory(component);
        }

        public DisplayableValue<T> getHasValue() {
            return hasValue;
        }

        public void setDisplayValue(DisplayableValue<T> displayValue) {
            this.hasValue = displayValue;
        }

        public ItemComponent getItemComponent() {
            return itemComponent;
        }

        public void setItemComponent(ItemComponent itemComponent) {
            this.itemComponent = itemComponent;
        }

        public T getValue() {
            return getHasValue().getValue();
        }

    }

    public ListBox(Color color) {
        super(color);
        this.selected = -1;
        itemsContainer = new VerticalPanel(color);
        itemsContainer.setColor(color);
        itemsContainer.setParentContainer(this);
        itemsContainer.setFillOnClear(FILL_ON_CLEAR_DEFAULT);
        this.setFillOnClear(FILL_ON_CLEAR_DEFAULT);
    }

    public ListBox() {
        this(Colors.FGBGColors.NO_COLOR_SPECIFIED);
    }

    public ListBox(DisplayableValue<T>[] listItems) {
        this();
        setItems(Arrays.asList(listItems));
    }
    
    public ListBox(DisplayableValue<T>[] listItems, ComponentItemFactory<T> displayItemToComponent) {
        this();
        setItems(Arrays.asList(listItems), displayItemToComponent);
    }

    public ListBox(Color color, DisplayableValue<T>[] listItems, ComponentItemFactory<T> displayItemToComponent) {
        this(color);
        setItems(Arrays.asList(listItems), displayItemToComponent);
    }

    public ListBox(Color color, DisplayableValue<T>[] listItems) {
        this(color);
        setItems(Arrays.asList(listItems), DEFAULT_COMPONENT_FACTORY);
    }

    public static ListBox<String> fromStrings(String[] listItems) {
        return new ListBox<String>(StringDisplayValue.fromStringValues(listItems));
    }

    public static ListBox<String> fromStrings(String[] listItems, ComponentItemFactory<String> displayItemToComponent) {
        return new ListBox<String>(StringDisplayValue.fromStringValues(listItems), displayItemToComponent);
    }

    public static ListBox<String> fromStrings(Color color, String[] listItems) {
        return new ListBox<String>(color, StringDisplayValue.fromStringValues(listItems));
    }

    public static ListBox<String> fromStrings(Color color, String[] listItems, ComponentItemFactory<String> displayItemToComponent) {
        return new ListBox<String>(color, StringDisplayValue.fromStringValues(listItems), displayItemToComponent);
    }

    @FunctionalInterface
    public interface ComponentItemFactory<T> {
        Component<?> makeComponentFromdisplayableItem(Color color, int maxWidthStr, DisplayableValue<T> displayableItem);
    }

    public static class TextComponentItemFactory<T> implements ComponentItemFactory<T> {

        @Override
        public Component<?> makeComponentFromdisplayableItem(Color color, int maxWidthStr, DisplayableValue<T> displayableItem) {
 
            Dimension d = new Dimension.Simple(Dimension.UTIL.stringToDimension(displayableItem.getDisplayValue()).getHeight(), maxWidthStr);
  
            TextComponent tc = new TextComponent(color, d, StringUtils.padRight(displayableItem.getDisplayValue(),maxWidthStr)) ;
            return tc;
        }
    }

    ComponentItemFactory<T> DEFAULT_COMPONENT_FACTORY = new TextComponentItemFactory<T>(); //(color, maxWidthStr, displayableItem) -> new ListItemComponent<T>(this.getColor(), maxWidthStr, displayableItem);

    public void setSelectedColor(Color selectedColor) {
        this.selectedColor = selectedColor;
    }

    public int getEventTriggerForValueChanged() {
        return eventTriggerForValueChanged;
    }

    public void setEventTriggerForValueChanged(int eventTriggerForValueChanged) {
        this.eventTriggerForValueChanged = eventTriggerForValueChanged;
    }

    public void setItems( List<? extends DisplayableValue<T>> displayableItems , ComponentItemFactory<T> displayItemToComponent) {
        this.items = new ArrayList<ListItem>(displayableItems.size());
        this.itemsContainer.clearAndRemoveAllChildren();
        int maxWidthStr = displayableItems.stream().map( (dv)-> Dimension.UTIL.stringToDimension( dv.getDisplayValue()) ).mapToInt((d)->d.getWidth()).max().orElse(0);
        int maxHeightStr = displayableItems.stream().map( (dv)-> Dimension.UTIL.stringToDimension( dv.getDisplayValue()) ).mapToInt((d)->d.getHeight()).max().orElse(0);
        if (maxHeightStr>1 && itemsContainer.getVerticalSpacer()==0) {
            itemsContainer.setVerticalSpacer(1);
        }

        for (DisplayableValue<T> displayableItem : displayableItems) {

            Component<?> component = displayItemToComponent.makeComponentFromdisplayableItem(this.getColor(), maxWidthStr, displayableItem);
//            if (component instanceof Composite) {
//                padding = 4;
//            }

            ListItem item = new ListItem(displayableItem, component);
            item.itemComponent.unSelect();
            items.add( item );

            this.itemsContainer.add(component);
        }
        //itemsContainer.setBounds( null ); // forces recalc of dimensions
        //itemsContainer.setBounds( new Dimension.Simple( itemsContainer.getHeight() , itemsContainer.getWidth()) );
        selected = -1;
    }

    public void setItems( List<? extends DisplayableValue<T>> displayableItems) {
        setItems(displayableItems, DEFAULT_COMPONENT_FACTORY);
    }
    
    public void setSelectedValue(T value) {
        int idx = indexOf(value);
        if (idx == -1) {
            throw new IllegalArgumentException(HasName.UTIL.getName(this)+": Attempted to set to a value of "+String.valueOf(value));
        }
        setSelected(idx);
    }

    public int indexOf(ListItem item) {
        if (item == null) {
            throw new NullPointerException(HasName.UTIL.getName(this)+": item paramater null!");
        }
        if (this.items == null) {
            return -1;
        }

        return this.items.indexOf(item);
    }

    public int indexOf(T value) {
        if (value == null) {
            throw new NullPointerException(HasName.UTIL.getName(this)+": value paramater null!");
        }
        if (this.items == null) {
            return -1;
        }
        Optional<ListBox<T>.ListItem> itemO = this.items.stream().filter((item)-> value.equals(item.hasValue.getValue())).findFirst();
        if (itemO.isPresent()) {
            // inefficient - traversing twice - there are fancy techniques to avoid this. 
            return indexOf(itemO.get());
        }
        return -1;
    }

    public int indexOf(Component<?> childComponent) {
        if (childComponent == null) {
            throw new NullPointerException(HasName.UTIL.getName(this)+": childComponent paramater null!");
        }
        if (this.items == null) {
            return -1;
        }
        Optional<ListBox<T>.ListItem> itemO = this.items.stream().filter((item)-> childComponent == item.itemComponent.getComponent()).findFirst();
        if (itemO.isPresent()) {
            // inefficient - traversing twice - there are fancy techniques to avoid this. 
            return indexOf(itemO.get());
        }
        return -1;
    }

    public ListBox<T> setColor(Color drawingcolor) {
        super.setColor(drawingcolor);
        this.itemsContainer.setColor(drawingcolor);
        if (this.items != null) {
            for (ListBox<T>.ListItem item : this.items) {
                Component<?> listItemComponent = item.itemComponent.getComponent();
                listItemComponent.setColor(drawingcolor);
            }
        }
        return this;
    }

    public int size() {
        return items.size();
    }

    /**
     * @return selected item index (position in list)
     */
    public int getSelectedIndex() {
        if (selected < 0 || selected >= size()) {
            return -1;
        }
        return selected;
    }

    /**
     * returns value of item at index
     */
    public DisplayableValue<T> getValueAt(int index) {
        if (index < 0 || index >= size()) {
            return null;
        }
        return items.get(index).getHasValue();
    }

    private ItemComponent getItemComponentAt(int index) {
        if (index < 0 || index >= size()) {
            return null;
        }
        ItemComponent itemComponent = items.get(index).itemComponent;
        return itemComponent;
    }

    /**
     * @param index
     * @return  true if success in selecting item referenced by index
     */
    public boolean setSelected(int index) {
        if (index < 0 || index >= size()) {
            return false;
        }

        // unhilite
        int curSelected = getSelectedIndex();
        if (curSelected>-1 && curSelected != index) {
            ItemComponent curSelectedComponent = getItemComponentAt(curSelected);
            if (curSelectedComponent != null) {
                curSelectedComponent.unSelect();
            }
        }

        // select
        selected = index;

        curSelected = getSelectedIndex();
        if (curSelected>-1) {
            ItemComponent curSelectedComponent = getItemComponentAt(curSelected);
            if (curSelectedComponent != null) {
                curSelectedComponent.selectWithFocus();
            }
        }

        this.markAsDirty();

        return true;
    }

    // returns true if we can select a 'previous' item
    private boolean canSetSelectedPrev() {
        int selected = getSelectedIndex();
        if (selected < 0) {
            if (size()>0) {
                return true;
            } else {
                return false;
            }
        }
        return selected > 0;
    }

    // returns true if we can select a 'next' item
    private boolean canSetSelectedNext() {
        int selected = getSelectedIndex();
        if (selected < 0) {
            if (size()>0) {
                return true;
            } else {
                return false;
            }
        }
        return selected + 1 < size();
    }

    public boolean setSelectedPrev() {
        int selected = getSelectedIndex();
        if (selected < 0) {
            if (size()>0) {
                return setSelected(size()-1);
            } else {
                return false;
            }
        }
        return setSelected(selected - 1);
    }

    private boolean setSelectedNext() {
        int selected = getSelectedIndex();
        if (selected < 0) {
            if (size()>0) {
                return setSelected(0);
            } else {
                return false;
            }
        }
        return setSelected(selected + 1);
    }

    public DisplayableValue<T> getSelected() {
        int selected = getSelectedIndex();
        if (selected < 0) {
            return null;
        }
        return items.get(selected).getHasValue();
    }

    public T getSelectedValue() {
        int selected = getSelectedIndex();
        if (selected < 0) {
            return null;
        }
        return items.get(selected).getValue();
    }

    @Override
    public T getValue() {
        HasValue<T> selectedItem = getSelected();
        return selectedItem == null ? null : selectedItem.getValue();
    }
    
    @Override
    public void setValue(T value) {
        this.setSelectedValue(value);
    }


    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.merge(this.itemsContainer.getSegmentsBuffer());
    }

    private void raiseEventRequestShowSelected(EventBus eventBus) {
        int curSelected = getSelectedIndex();
        if (curSelected>-1) {
            ItemComponent curSelectedComponent = getItemComponentAt(curSelected);
            new ShowComponentOnFocus().onFocus(curSelectedComponent.getComponent(), eventBus);
            eventBus.raiseEvent(new RequestUpdateCanvasEvent(this));
        }
    }

    @Override
    public Component<?> getComponentAtOffset(Position offset) {
        Component<?> component = this.itemsContainer.getComponentAtOffset(offset);
        if (component == this.itemsContainer) {
            return this;
        }
        return component;
    }

    private Focusable focusable;
    private ChainedEventHandler chainedHandler;

    public Focusable asFocusable() {
        if (focusable != null) {
            return focusable;
        }
        focusable = makeFocusable();
        return focusable;
    }

    /**
     * Called after this ListBox has lost focus. Override to react (e.g. DropdownListBox closes its
     * popup list). Note newFocusable is the new focusable at the level where focus moved, which may
     * be a container of the final target rather than the target itself.
     */
    protected void onUnFocus(Focusable newFocusable, EventBus eventBus) {
    }

    private Focusable makeFocusable() {

        ChainedEventHandler chainedHandler = getChainedEventHandler();

        return new DefaultFocusableBehaviour.Simple(this, chainedHandler.asConditionalEventHandler()) {

            @Override
            public void unFocus(Focusable newFocusable, EventBus eventBus) {
                super.unFocus(newFocusable, eventBus);
                int curSelected = getSelectedIndex();
                if (curSelected > -1) {
                   ItemComponent curSelectedComponent = getItemComponentAt(curSelected);
                   if (curSelectedComponent != null) {
                       curSelectedComponent.selectWithNoFocus(newFocusable);
                   }
                }
                onUnFocus(newFocusable, eventBus);
            }

            public void focus(Focusable oldFocusable, EventBus eventBus) {
                if (suppressCLICK) {return;}
                super.focus(oldFocusable, eventBus);
                int curSelected = getSelectedIndex();
                if (curSelected>-1) {
                    setSelected(curSelected);
                } else {
                    setSelected(0);
                    if ((getEventTriggerForValueChanged() & VALUE_CHANGED_ON_FOCUS) > 0 ) {
                        eventBus.raiseEvent(  new HasValue.ValueChangedEvent<>(ListBox.this) );
                    }
                }
                raiseEventRequestShowSelected(eventBus);
            }

            @Override
            public void setFocus(boolean focus, EventBus eventBus) {
                super.setFocus(focus, eventBus);
                if (suppressCLICK) {
                    if (focus && !isDirty()) {
                        ListBox.this.markAsDirty();
                        eventBus.raiseEvent(new RequestUpdateCanvasEvent(ListBox.this));
                    }
                    return;
                }
                if (focus) {
                    int curSelected = getSelectedIndex();
                    if (curSelected>-1) {
                        setSelected(curSelected);
                    } else {
                        setSelected(0);
                        if ((getEventTriggerForValueChanged() & VALUE_CHANGED_ON_FOCUS) > 0 ) {
                            eventBus.raiseEvent(  new HasValue.ValueChangedEvent<>(ListBox.this) );
                        }
                    }
                    raiseEventRequestShowSelected(eventBus);
                } else {
                    if (!focus) {
                        int curSelected = getSelectedIndex();
                        if (curSelected > -1) {
                           ItemComponent curSelectedComponent = getItemComponentAt(curSelected);
                           if (curSelectedComponent != null) {
                               curSelectedComponent.selectWithNoFocus();
                           }
                        }
                   }
                }
                markAsDirty();
            }
        };
    }

    private ChainedEventHandler getChainedEventHandler() {
        if (chainedHandler != null) {
            return chainedHandler;
        }

        chainedHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);

        chainedHandler.appendEventHandler(new KeyInputEventHandler(this.keystrokePrevious, 
                (te)->  canSetSelectedPrev(),
                (te)->  {
                            if (setSelectedPrev()) {
                                if ((getEventTriggerForValueChanged() & VALUE_CHANGED_ON_PREV_KEYSTROKE) > 0 ) {
                                    te.getEventBus().raiseEvent(  new HasValue.ValueChangedEvent<>(ListBox.this) );
                                }
                                raiseEventRequestShowSelected(te.getEventBus());
                            }
                        }));

        chainedHandler.appendEventHandler(new KeyInputEventHandler(this.keystrokeNext, 
                (te)->  canSetSelectedNext(),
                (te)->  {
                            if (setSelectedNext()) {
                                if ((getEventTriggerForValueChanged() & VALUE_CHANGED_ON_NEXT_KEYSTROKE) > 0 ) {
                                    te.getEventBus().raiseEvent(  new HasValue.ValueChangedEvent<>(ListBox.this) );
                                }
                                raiseEventRequestShowSelected(te.getEventBus());
                            }
                        }));

        chainedHandler.appendEventHandler(new KeyInputEventHandler(Keys.ENTER_KEYSTROKE, 
                (te)->  getSelectedIndex()>-1,
                (te)->  {
                            if (getSelectedIndex()>-1) {
                                if ((getEventTriggerForValueChanged() & VALUE_CHANGED_ON_ENTER_KEY) > 0 ) {
                                    te.getEventBus().raiseEvent(  new HasValue.ValueChangedEvent<>(ListBox.this) );
                                }
                                raiseEventRequestShowSelected(te.getEventBus());
                            }
                        }).setName("ListBox-ENTER-KEY-onValueChangedHandler"));

        return chainedHandler;
    }

    @Override
    public int getCol() {
        return itemsContainer.getCol();
    }

    @Override
    public int getRow() {
        return itemsContainer.getRow();
    }

    @Override
    public int getWidth() {
        return itemsContainer.getWidth();
    }

    @Override
    public int getHeight() {
        return itemsContainer.getHeight();
    }

    @Override
    public void moveTo(int row, int col) {
        itemsContainer.moveTo(row, col);
    }

    @Override
    protected Rectangle getRectangleForClearAndFill() {
        return itemsContainer;
    }

    public ListBox<T> setBorder(boolean hasBorder) {
        itemsContainer.setBorder(hasBorder);
        return this;
    }

    public ListBox<T> setTitle(String title) {
        itemsContainer.setTitle(title);
        return this;
    }

    public ListBox<T> setTitle(Color titleColors, String title) {
        itemsContainer.setTitle(titleColors, title);
        return this;
    }

    @Override
    public Collection<? extends Component<?>> getChildren() {
        return Collections.singleton(this.itemsContainer);
    }

    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        if (child == this.itemsContainer) {
            return Position.ZERO;
        }
        return null;
    }

    @Override
    public void childResized(Component<?> child) {
        this.markAsDirty();
        if (this.getParentContainer() != null) {
            this.getParentContainer().childResized(this);
        }
    }

    static boolean suppressCLICK = false;  // purely for debugging mouse clicks .. remove when happy with it
    
    @Override
    public void onClick(EventBus eventBus) {
        if (suppressCLICK) {
            return;
        }
        if (! asFocusable().hasFocus()) {
            // focus manager should have already put focus here - but just in case ..
            eventBus.raiseEvent(new RequestFocusEvent(asFocusable()));
        }
    }

    @Override
    public void onClick(ApplicationMouseEvent appMouseEvent) {
        if (suppressCLICK) {return;}
        
        if (MOUSE_ACTION.DOWN_LEFT.equals(appMouseEvent.getMouseAction()) || MOUSE_ACTION.DOWN_LEFT_DRAG.equals(appMouseEvent.getMouseAction())) {
            
            // first find child component , clicked on
            Component<?> componentClicked = getComponentAtOffset(appMouseEvent.getMousePosition() );

            // now travel up its parents until we hit a child component of verticalPanel (itemsContainer)
            // as this will map directly to one of the ListItem's
            while ( componentClicked != null && 
                    componentClicked != this &&
                    componentClicked != itemsContainer &&
                    componentClicked.getParentContainer() != itemsContainer) {
                componentClicked = (Component<?>)componentClicked.getParentContainer();
            }
            if (componentClicked != null && componentClicked.getParentContainer() == itemsContainer) {
                // itemsContainer child component found  so find out its index and then select it
                int selectIDX = indexOf(componentClicked);
                if (selectIDX > -1) {
                    this.setSelected(selectIDX);
                    // now let the rest of the app know that changes have happened to this ListBox
                    onMouseSelected(appMouseEvent.getEventBus()); 
                }
            }

        }
    }

    protected void onMouseSelected(EventBus eventBus) {
        if (suppressCLICK) {
            return;
        }
      // now let the rest of the app know that changes have happened to this ListBox
        if ((getEventTriggerForValueChanged() & VALUE_CHANGED_ON_MOUSE_CLICK) > 0 ) {
            eventBus.raiseEvent(  new HasValue.ValueChangedEvent<>(ListBox.this) );
        }
        
        raiseEventRequestShowSelected(eventBus);
    }

    HasValue.OnValueChangedEventHandler<T> onChangeHandler;

    /**
     * 
     * Use this method to detect selection changed events
     * 
     * @param consumeChange
     */
    public void onChange(EventHandler consumeChange) {
        onChangeHandler = HasValue.UTIL.bindConsumerAsOnValueChangedHandler(this, onChangeHandler, getChainedEventHandler(), consumeChange);

    }

    public String toStringDetails() {
        T selected = getSelectedValue();
        return "count:"+this.items.size()+ ((selected != null) ? " selected: " + String.valueOf(getSelectedValue()) : "");
    }

    @Override
    public EventHandlerContainer prependEventHandler(ConditionalEventHandler eventHandler) {
        return getChainedEventHandler().prependEventHandler(eventHandler);
    }

    @Override
    public EventHandlerContainer appendEventHandler(ConditionalEventHandler eventHandler) {
        return getChainedEventHandler().appendEventHandler(eventHandler);
    }

    @Override
    public EventHandlerContainer removeEventHandler(ConditionalEventHandler eventHandler) {
        return getChainedEventHandler().removeEventHandler(eventHandler);
    }


}
