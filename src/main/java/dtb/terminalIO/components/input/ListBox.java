package dtb.terminalIO.components.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import dtb.terminalIO.components.AbstractComponent;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.CharComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HiliterAdapter;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.input.DisplayableValue.StringDisplayValue;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.events.mouse.GetComponentAtOffset;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.StringUtils;

public class ListBox<T> extends AbstractComponent<ListBox<T>>  implements HasValue<T>, ToFocusable, ParentContainer, GetComponentAtOffset {
    
    List<ListItem> items = new ArrayList<ListItem>();
    VerticalPanel  itemsContainer;
    int            selected;
    KeyStroke      keystrokePrevious       = Keys.UP_KEYSTROKE;
    KeyStroke      keystrokeNext           = Keys.DOWN_KEYSTROKE;
    Color          selectedColor           = new FGBGColors(Colors.IYel);

    // Components that are rendered in the list
    abstract class ItemComponent  {
         Component<?> component;
         
         public Component<?> getComponent() {
            return component;
        }

        ItemComponent(Component<?> component) {
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

        /*
         *                         "■",
                        "□",
                        "▪",
                        "▫"
        mapChars.map('◍','*');
        mapChars.map('◌','o');

         * */
        
    }
    
    ItemComponent itemComponentfactory(Component<?> component) {
             if (component instanceof TextComponent) {
                 return new ItemComponent(component) {

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
                 return new ItemComponent(component) {

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
                 return new ItemComponent(component) {

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
                 return new ItemComponent(component) {

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
             return new ItemComponent(component) {

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
        
        private HasValue<T> hasValue;
        private ItemComponent  itemComponent;
                    
        public ListItem(HasValue<T> hasValue, Component<?> component) {
            super();
            this.hasValue = hasValue;
            this.itemComponent = itemComponentfactory(component);
        }

        public HasValue<T> getHasValue() {
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
    }
    
    public ListBox() {
        this(FGBGColors.NO_COLOR_SPECIFIED);
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
    
    
    ComponentItemFactory<T> DEFAULT_COMPONENT_FACTORY = (color, maxWidthStr, displayableItem) -> new ListItemComponent<T>(this.getColor(), maxWidthStr, displayableItem);
    
    
    public void setItems( List<DisplayableValue<T>> displayableItems , ComponentItemFactory<T> displayItemToComponent) {
        this.items = new ArrayList<ListItem>(displayableItems.size());
        this.itemsContainer.clearAndRemoveAllChildren();
        int maxWidthStr = displayableItems.stream().map( (dv)-> Dimension.UTIL.stringToDimension( dv.getDisplayValue()) ).mapToInt((d)->d.getWidth()).max().orElse(0);
        int maxHeightStr = displayableItems.stream().map( (dv)-> Dimension.UTIL.stringToDimension( dv.getDisplayValue()) ).mapToInt((d)->d.getHeight()).max().orElse(0);
        if (maxHeightStr>1 && itemsContainer.getVerticalSpacer()==0) {
            itemsContainer.setVerticalSpacer(1);
        }
        
        int padding = 2;
        
        for (DisplayableValue<T> displayableItem : displayableItems) {
            
            
            Component<?> component = displayItemToComponent.makeComponentFromdisplayableItem(this.getColor(), maxWidthStr, displayableItem);
            if (component instanceof Composite) {
                padding = 4;
            }

            ListItem item = new ListItem(displayableItem, component);
            item.itemComponent.unSelect();
            items.add( item );
            
            this.itemsContainer.add(component);
        }
        itemsContainer.setBounds( null ); // forces recalc of dimensions
        itemsContainer.setBounds( new Dimension.Simple( itemsContainer.getHeight()+padding , itemsContainer.getWidth()+padding) );
        selected = -1;
    }
    
    public void setItems( List<DisplayableValue<T>> displayableItems) {
        setItems(displayableItems, DEFAULT_COMPONENT_FACTORY);
    }

    public ListBox<T> setColor(Color drawingcolor) {
        super.setColor(drawingcolor);
        this.itemsContainer.setColor(drawingcolor);
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
    public HasValue<T> indexOf(int index) {
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
    
    public boolean setSelected(int index) {
        if (index < 0 || index >= size()) {
            return false;
        }

        // unhilite
        int curSelected = getSelectedIndex();
        if (curSelected>-1) {
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
    
    
    public HasValue<T> getSelected() {
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
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.merge(this.itemsContainer.getSegmentsBuffer());
    }

    private void raiseEventRequestShowSelected(EventBus eventBus) {
        int curSelected = getSelectedIndex();
        if (curSelected>-1) {
            ItemComponent curSelectedComponent = getItemComponentAt(curSelected);
            new ShowComponentOnFocus().onFocus(curSelectedComponent.getComponent(), eventBus);
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
    
    public Focusable asFocusable() {

        ChainedEventHandler chainedHandler = ChainedEventHandler.useChainOfResponsibilityStrategy();

        chainedHandler.appendEventHandler(new KeyInputEventHandler(this.keystrokePrevious, 
                (te)->  canSetSelectedPrev(),
                (te)->  {
                            if (setSelectedPrev()) {
                                te.getEventBus().raiseEvent(  new HasValue.ValueChangedEvent<>(ListBox.this) );
                                raiseEventRequestShowSelected(te.getEventBus());
                            }
                        }));
        
        chainedHandler.appendEventHandler(new KeyInputEventHandler(this.keystrokeNext, 
                (te)->  canSetSelectedNext(),
                (te)->  {
                            if (setSelectedNext()) {
                                te.getEventBus().raiseEvent(  new HasValue.ValueChangedEvent<>(ListBox.this) );
                                raiseEventRequestShowSelected(te.getEventBus());
                            }
                        }));
        
        return new DefaultFocusableBehaviour.Simple(this, chainedHandler.asConditionalEventHandler()) {

            @Override
            public void setFocus(boolean focus, EventBus eventBus) {
                super.setFocus(focus, eventBus);
                if (focus) {
                    int curSelected = getSelectedIndex();
                    if (curSelected>-1) {
                        setSelected(curSelected);
                    } else {
                        setSelected(0);
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

    public void setBorder(boolean hasBorder) {
        itemsContainer.setBorder(hasBorder);
    }

    public void setTitle(String title) {
        itemsContainer.setTitle(title);
        
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
    }

}
