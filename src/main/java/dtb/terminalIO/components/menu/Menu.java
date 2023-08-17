package dtb.terminalIO.components.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import dtb.terminalIO.components.AbstractComponentWrappper;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.input.DisplayableValue;
import dtb.terminalIO.components.input.ListBox;
import dtb.terminalIO.components.input.ListBox.ComponentItemFactory;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.FocusablesContainer;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.StringUtils;

/**
 *  Menu is not a 'visual' component - but rather a non-visual controller of a popup menu which, in the end, is 
 *  just a {@link ListBox} with special behavior to act like a menu
 *
 */
public class Menu implements HasName<Menu> {

    private String name;
    private List<MenuItem> menuItems;
    private ListBox<Object> listBox;
    Color selectedColor;
    Color color;
    boolean hasSubMenu = false;
    Composite rootPanel;

    public Menu(String name) {
        this.menuItems = new ArrayList<MenuItem>();
        this.name = name;
    }
    public Menu() {
        this(null);
    }

    public Menu addItem(MenuItem menuItem) {
        this.menuItems.add(menuItem);
        return this;
    }
    
    
    protected boolean hasSubMenu() {
        return hasSubMenu;
    }

    public Menu addItem(Object value, EventHandler handleSelected) {
        this.addItem(new MenuItem.Simple(value, handleSelected));
        return this;
    }

    public Menu addItem(Object value, Menu menu) {
        EventHandler handleSelected = (te)-> {
           // menu
        };
        menu.setColor(this.color);
        menu.setSelectedColor(this.selectedColor);
        this.addItem(new MenuItem.SubMenu(String.valueOf(value), menu, this));
        this.hasSubMenu = true;
        return this;
    }

    public Menu addDivider() {
        this.addItem(new MenuItem.Divider(this));
        return this;
    }

    private List<DisplayableValue<Object>> menuItemsAsDisplayableItems() {
        List<DisplayableValue<Object>> displayableItems = new ArrayList<DisplayableValue<Object>>();
        for (MenuItem menuItem :  menuItems) {
            DisplayableValue<Object> e = menuItem;
            displayableItems.add(e);
        }
        return displayableItems;
    }

    public void popUpMenu(Position position, Component<?> parent) {
        if (this.listBox != null) {
            getMenuRootPanel(this.listBox).bringToFront(this.listBox);
            return;
        }
        this.listBox = new ListBox<Object>();
        this.listBox.setBorder(true);
        if (this.color != null) {
            this.listBox.setColor(color);
        } 
        if (this.selectedColor != null) {
            this.listBox.setSelectedColor(this.selectedColor);
        }
        if (this.name != null) {
            this.listBox.setName(this.name);
        }

        this.listBox.setItems(menuItemsAsDisplayableItems(), new MenuTextComponentItemFactory(this));
        getMenuRootPanel(parent).add(position, this.listBox);
        getMenuRootPanel(this.listBox).bringToFront(this.listBox);
        
        Focusable parentAsFocusable = ToFocusable.UTIL.toFocusable(parent);
        if (parentAsFocusable == null) {
            throw new IllegalArgumentException("Error in popUpMenu().  Parent Parameter is not a focusable and a focusable cannot be derived from it");
        }

        ((FocusablesContainer)FocusManager.UTIL.getRootFocusManager( parentAsFocusable )).addFocusable(this.listBox.asFocusable());

        this.listBox.onChange((te)-> {
            MenuItem menuItem = (MenuItem)this.listBox.getSelected();
            menuItem.onSelected(te);
        });

    }

    public void popDownMenu() {
        if (this.listBox != null) {
            getMenuRootPanel(this.listBox).remove(this.listBox);
            Focusable listBoxAsFocusable = ToFocusable.UTIL.toFocusable(this.listBox);
            ((FocusablesContainer)listBoxAsFocusable.getParentFocusManager()).removeFocusable(listBoxAsFocusable);
            this.listBox = null;
            this.traverseAllSubMenus((m)-> m.popDownMenu());
            return;
        }
    }
    
    public void toggleMenu(Position position, Component<?> parent) {
        if (this.isVisible()) {
            this.popDownMenu();
        } else {
            this.popUpMenu(position, parent);
        }
    }

    public boolean isVisible() {
        if (this.listBox == null) {
            return false;
        }
        if (getMenuRootPanel(this.listBox).isChildVisible(this.listBox)) {
            return true;
        }
        return false;
    }
    
    public void setMenuRootPanel(Composite rootPanel) {
        this.rootPanel = rootPanel;
        traverseAllSubMenus((m)-> m.setMenuRootPanel(rootPanel));
    }


    public Composite getMenuRootPanel(Component<?> component) {
        
        if (this.rootPanel != null) {
            return this.rootPanel;
        }

        Component<?> rootPanel = Component.UTIL.getRootPanelOf(component);
        if (rootPanel != null && rootPanel instanceof Composite) {
            this.setMenuRootPanel((Composite)rootPanel);
            return (Composite) rootPanel;
        }

        throw new IllegalStateException("Cannot show Menu! Could not find a root panel that is a component of type Composite!");

    }

    protected boolean menuVisible(Component<?> parent) {
        Composite rootPanel  = this.getMenuRootPanel(parent);
        if ( rootPanel.containsChild(listBox)) {
            return true;
        }
        return false;
    }
    
    private void traverseAllSubMenus(Consumer<Menu> consumer) {
        if (this.hasSubMenu()) {
            this.menuItems.stream()
                    .filter(mi -> mi instanceof MenuItem.SubMenu)
                    .map(mi -> ((MenuItem.SubMenu)mi).getMenu()).forEach(consumer);
        }
    }

    public void setColor(Color color) {
        this.color = color;
        traverseAllSubMenus(m-> m.setColor(color));
    }

    public void setSelectedColor(Color selectedColor) {
        this.selectedColor = selectedColor;
        traverseAllSubMenus(m-> m.setSelectedColor(selectedColor));
    }

    // classes to support custom ListBox listItem Component Factories for Menu items
    
    public static class MenuTextComponentItemFactory implements ComponentItemFactory<Object> {

        Menu menu;

        MenuTextComponentItemFactory(Menu menu) {
            this.menu = menu;
        }

        @Override
        public Component<?> makeComponentFromdisplayableItem(Color color, int maxWidthStr, DisplayableValue<Object> displayableItem) {
            
            String suffix = " ";
            
            if (displayableItem instanceof MenuItem.SubMenu) {
                String rightArrowCharacter = "\u2794";
                suffix += rightArrowCharacter + " "; 
                return new SubMenuComponent(color, maxWidthStr, (MenuItem.SubMenu)displayableItem);
            } else if (menu.hasSubMenu()) {
                suffix += "  ";
            }
            TextComponent tc;
            if (displayableItem instanceof MenuItem.Divider) {
                tc = new TextComponent(color, StringUtils.repeat('─', maxWidthStr + 1 + suffix.length())) ;
            } else {
                tc = new TextComponent(color, " "+ StringUtils.padRight(displayableItem.getDisplayValue(),maxWidthStr) + suffix) ;
            }
            return tc;
        }
    }
    
    public static class SubMenuComponent extends AbstractComponentWrappper<TextComponent>  implements ListBox.ItemComponent {

        MenuItem.SubMenu subMenuItem;

        protected SubMenuComponent(Color color, int maxWidthStr, MenuItem.SubMenu subMenuItem) {
            super(color, new TextComponent(color, " "+ StringUtils.padRight(subMenuItem.getDisplayValue(),maxWidthStr) + " \u2794 "));
            this.subMenuItem = subMenuItem;
        }

        Menu getMenu() {
            
            Menu menu = this.subMenuItem.getMenu();
            if (menu == null) {
                throw new NullPointerException("menu in SubMenuComponent was null!");
            }
            return menu;
        }

        @Override
        public Component<?> getComponent() {
            return this;
        }

        @Override
        public void selectWithFocus() {
            getWrappedComponent().setColor( getMenu().selectedColor );
            Position offset = Component.UTIL.getOffsetFromComponentContainer(getWrappedComponent(), getMenu().getMenuRootPanel(getWrappedComponent()));
            getMenu().popUpMenu(getWrappedComponent().getPostion_TopRight().offsetAdd(offset).offsetAdd(-2,1), subMenuItem.getParentMenu().listBox);
        }

        @Override
        public void unSelect() {
            getWrappedComponent().setColor( getMenu().color );
            if (getMenu().isVisible()) {
                getMenu().popDownMenu();
            }
        }

        @Override
        public void selectWithNoFocus() {
            Menu menu = getMenu();
            Color color = menu.color;
            color = color.bold();
            getWrappedComponent().setColor( color );
            if (getMenu().isVisible()) {
                getMenu().popDownMenu();
            }
        }

    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Menu setName(String name) {
        this.name = name;
        return this;
    }
    
    
    public String toString() {
        return HasName.UTIL.getName(this);
    }



}
