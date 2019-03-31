package dtb.terminalIO.components.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.CharComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HiliterAdapter;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.input.DisplayableValue.StringDisplayValue;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.Focusable;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

public class ListBox<T> extends Composite  implements HasValue<T>, AsFocusable.ToFocusable {
    
    List<ListItem> items = new ArrayList<ListItem>();
    VerticalPanel itemsContainer;
    int selected;
    KeyStroke keystrokePrevious       = Keys.UP_KEYSTROKE;
    KeyStroke keystrokeNext           = Keys.DOWN_KEYSTROKE;
    Color     selectedColor           = new FGBGColors(Colors.IYel);

    // Components that are rendered in the list
    abstract class ItemComponent  {
         Component<?> component;
         
         public Component<?> getComponent() {
            return component;
        }

        ItemComponent(Component<?> component) {
             this.component = component;
         }
         
         abstract public void select();
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
                    public void select() {
                        //new HiliterAdapter<Component<?>>(getComponent()).getHiliter().hilite();
                        getComponent().setColor(ListBox.this.selectedColor.invert());
                    }

                    @Override
                    public void unSelect() {
                        //new HiliterAdapter<Component<?>>(getComponent()).getHiliter().unHilite();
                        getComponent().setColor(ListBox.this.getColor());
                    }
                 };
             }

             if (component instanceof BorderedComponent) {
                 return new ItemComponent(component) {

                    @Override
                    public void select() {
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
                     
                 };
             }
         
             if (component instanceof ListItemComponent) {
                 return new ItemComponent(component) {

                    @Override
                    public void select() {
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
                     
                 };
             }

             if (component instanceof Composite) {
                 return new ItemComponent(component) {

                    @Override
                    public void select() {
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
                     
                 };
             }
             return new ItemComponent(component) {

                 @Override
                 public void select() {
                     new HiliterAdapter<Component<?>>(getComponent()).getHiliter().hilite();
                 }

                 @Override
                 public void unSelect() {
                     new HiliterAdapter<Component<?>>(getComponent()).getHiliter().unHilite();
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
        this.add(itemsContainer);
    }
    
    
    public ListBox() {
        this(FGBGColors.NO_COLOR_SPECIFIED);
    }
    
    public ListBox(DisplayableValue<T>[] listItems) {
        this();
        setItems(Arrays.asList(listItems));
    }
    
    public ListBox(Color color, DisplayableValue<T>[] listItems) {
        this(color);
        setItems(Arrays.asList(listItems));
    }
    
    public static ListBox<String> fromStrings(String[] listItems) {
        return new ListBox<String>(StringDisplayValue.fromStringValues(listItems));
    }

    public static ListBox<String> fromStrings(Color color, String[] listItems) {
        return new ListBox<String>(color, StringDisplayValue.fromStringValues(listItems));
    }

    public void setItems( List<DisplayableValue<T>> displayableItems ) {
        this.items = new ArrayList<ListItem>(displayableItems.size());
        this.itemsContainer.clearAndRemoveAllChildren();
        int maxWidthStr = displayableItems.stream().map( (dv)-> Dimension.UTIL.stringToDimension( dv.getDisplayValue()) ).mapToInt((d)->d.getWidth()).max().orElse(0);
        int maxHeightStr = displayableItems.stream().map( (dv)-> Dimension.UTIL.stringToDimension( dv.getDisplayValue()) ).mapToInt((d)->d.getHeight()).max().orElse(0);
        if (maxHeightStr>1 && itemsContainer.getVerticalSpacer()==0) {
            itemsContainer.setVerticalSpacer(1);
        }
        for (DisplayableValue<T> displayableItem : displayableItems) {
            
            
//            Dimension strDim = Dimension.UTIL.stringToDimension( displayableItem.getDisplayValue());
//            Dimension tcDim = new Dimension.Simple( strDim.getHeight(), maxWidthStr ); 
//            TextComponent tc = new TextComponent(this.getColor(), tcDim, displayableItem.getDisplayValue());
//            
//            Composite component = new Composite();
//            component.setColor(getColor());
//            component.add(tc);
          
            Component<?> component = new ListItemComponent<T>(this.getColor(), maxWidthStr, displayableItem);

            ListItem item = new ListItem(displayableItem, component);
            item.itemComponent.unSelect();
            items.add( item );
            
            this.itemsContainer.add(component);
        }
        itemsContainer.setBounds( null ); // forces recalc of dimensions
        itemsContainer.setBounds( new Dimension.Simple( itemsContainer.getHeight()+2 , itemsContainer.getWidth()+2) );
        fullrecalcDim();
        selected = -1;
        
    }

    public void addItem( DisplayableValue<T> displayableItem ) {
        if (this.items == null) {
           this.items = new ArrayList<ListItem>();
           this.itemsContainer.clearAndRemoveAllChildren();
        }
    }
    
    
    public ListBox<T> setColor(Color drawingcolor) {
        super.setColor(drawingcolor);
        this.itemsContainer.setColor(drawingcolor);
        return this;
    }

//    public TextInput setEmptyColor(Color emptyColor) {
//        super.setEmptyColor(emptyColor);
//        if (this.textRenderer != null) {
//            this.textRenderer.setEmptyColor(emptyColor);
//        }
//        return this;
//    }
//    
//    public TextInput setEmptyChar(char emptyChar) {
//        super.setEmptyChar(emptyChar);
//        if (this.textRenderer != null) {
//            this.textRenderer.setEmptyChar(emptyChar);
//        }
//        return this;
//    }

    
    
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
                curSelectedComponent.select();
            }
        }
        
        return true;
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

    public boolean setSelectedNext() {
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
    public boolean isDirty() {
        return super.isDirty();
    }
    
    @Override
    public void draw(Color color, IScreenLineSegmentsBuffer segments) {
        draw(color, segments, true);
    }


    public Focusable asFocusable(EventBus eventBus) {
        
        ChainedEventHandler chainedHandler = ChainedEventHandler.useChainOfResponsibilityStrategy();
        
        chainedHandler.addEventHandler(new KeyInputEventHandler(this.keystrokePrevious, 
                (te)->  {
                            if (setSelectedPrev()) {
                                eventBus.addEvent(  new HasValue.ValueChangedEvent<>(ListBox.this) );
                            }
                        }));
        
        chainedHandler.addEventHandler(new KeyInputEventHandler(this.keystrokeNext, 
                (te)->  {
                            if (setSelectedNext()) {
                                eventBus.addEvent(  new HasValue.ValueChangedEvent<>(ListBox.this) );
                            }
                        }));
        
        return new AsFocusable.Simple(this, eventBus, chainedHandler.asConditionalEventHandler()) {

            
            @Override
            public void setFocus(boolean focus) {
                super.setFocus(focus);
                
                if (focus) {
                    int curSelected = getSelectedIndex();
                    if (curSelected>-1) {
                        setSelected(curSelected);
//                       ItemComponent curSelectedComponent = getItemComponentAt(curSelected);
//                       if (curSelectedComponent != null) {
//                           curSelectedComponent.select();
//                       }
                    } else {
                        setSelected(0);
                    }
                } else {
                    if (!focus) {
                        int curSelected = getSelectedIndex();
                        if (curSelected>-1) {
                           ItemComponent curSelectedComponent = getItemComponentAt(curSelected);
                           if (curSelectedComponent != null) {
                               curSelectedComponent.unSelect();
                           }
                        }
                   }
                }
                markAsDirty();
            }
        };
    }

    
}
