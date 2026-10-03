package dtb.terminalIO.components.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import dtb.terminalIO.components.AbstractComponentWrappper;
import dtb.terminalIO.components.AbstractWrappedComponentWithFocusManager;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.input.DisplayableValue;
import dtb.terminalIO.components.input.ListBox;
import dtb.terminalIO.components.input.ListBox.ComponentItemFactory;
import dtb.terminalIO.components.menu.MenuItem.LeftRightSelector;
import dtb.terminalIO.components.menu.MenuItem.SubMenu;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.StringUtils;

/**
 *  Menu is not a 'visual' component - but rather a non-visual controller of a popup menu which, in the end, is 
 *  just a {@link ListBox} with special behavior to act like a menu
 */
public class Menu implements HasName<Menu> , LeftRightSelector {

    private String name;
    private List<MenuItem> menuItems;
    MenuContainer menuContainer;
    private Color selectedColor;
    private Color color;
    boolean hasSubMenu = false;
    private Composite rootPanel;
    private MenuBar menuBar;
    private Menu parentMenu;

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
    
    public Menu getRootMenu() {
        if (this.menuBar != null) {
            return this;
        }
        if (this.parentMenu == null) {
            return this;
        }
        return this.parentMenu.getRootMenu();
    }

    private List<DisplayableValue<Object>> menuItemsAsDisplayableItems() {
        List<DisplayableValue<Object>> displayableItems = new ArrayList<DisplayableValue<Object>>();
        for (MenuItem menuItem :  menuItems) {
            DisplayableValue<Object> e = menuItem;
            displayableItems.add(e);
        }
        return displayableItems;
    }

    protected MenuContainer getMenuContainer() {
        if (this.menuContainer == null) {
            throw new NullPointerException("Tried to access MenuContainer when it is null!");
        }
        return this.menuContainer;
    }
    
    // returns null if nothing selected
    public MenuItem getSelected() {
        if (this.menuContainer == null) {
            return null;
        }
        MenuItem menuItem = getMenuContainer().getSelected();
        return menuItem;
    }

    public void popUpMenu(Position position, Component<?> parent) {
        if (this.menuContainer != null) {
            getMenuRootPanel(this.menuContainer).bringToFront(this.menuContainer);
            return;
        }

        Logging.info(HasName.UTIL.getName(this) + " popUpMenu()" );

        this.menuContainer = new MenuContainer(this, this.color);
        this.menuContainer.setBorder(true);
        if (this.color != null) {
            this.menuContainer.setColor(color);
        }

        if (this.selectedColor != null) {
            this.menuContainer.setSelectedColor(this.selectedColor);
        }

        if (this.name != null) {
            this.menuContainer.setName(this.name);
        }

        this.menuContainer.setItems(menuItemsAsDisplayableItems(), new MenuTextComponentItemFactory(this));
        // drawn on the root panel, but part of parent (its menu button / parent menu) for keyboard focus
        Component.UTIL.setPopupOwner(this.menuContainer, parent);
        getMenuRootPanel(parent).add(position, this.menuContainer);
        getMenuRootPanel(this.menuContainer).bringToFront(this.menuContainer);


        this.menuContainer.onChange((te)-> {
            try {
                MenuItem menuItem = getMenuContainer().getSelected();
                // Logging.info("this.menuContainer.onChange()-------- " + menuItem.toString());
                menuItem.onSelected(te);
                if (menuItem instanceof MenuItem.Simple) {
                     // the menus close: focus goes back to the root menu's owner (its menu button)
                     Component.UTIL.returnFocusToPopupOwner(this.getRootMenu().menuContainer, te.getEventBus());
                     this.getRootMenu().popDownMenu();
                     if (this.getRootMenu().menuBar != null) {
                         this.getRootMenu().menuBar.unFocus(null, te.getEventBus());
                     }
                }
            } catch (Exception e) {
                Logging.info("ERROR!!!!! menuContainer null for "+HasName.UTIL.getName(this)+"-------------------------------");
            }
        });

    }

    public void popDownMenu() {
        if (this.menuContainer != null) {
            this.traverseAllSubMenus((m)-> m.popDownMenu());
            getMenuRootPanel(this.menuContainer).remove(this.menuContainer);
            this.menuContainer = null;
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
        if (this.menuContainer == null) {
            return false;
        }
        if (getMenuRootPanel(this.menuContainer).isChildVisible(this.menuContainer)) {
            return true;
        }
        return false;
    }

    public boolean hasFocus() {
        if (!isVisible()) {
            return false;
        }
        if (this.menuContainer.hasFocus()) {
            return true;
        }
        return false;
    }

    /**
     * Is focusable part of this menu: the menu bar itself, this menu's list, or anything inside it -
     * its submenus at any depth, since a submenu counts as part of the menu it opened from (see
     * Component.getPopupOwner). Used to decide whether a menu stays open when its focus moves.
     */
    boolean isFocusablePartOfMenu(Focusable focusable) {
        if (focusable == null) {
            return false;
        }
        if (this.menuBar != null && ToFocusable.UTIL.toFocusable(this.menuBar) == focusable) {
            return true;
        }
        Focusable menuNode = this.menuContainer == null ? null : ToFocusable.UTIL.toFocusable(this.menuContainer);
        return menuNode != null && FocusManager.UTIL.containsFocusable(menuNode, focusable);
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
        if ( rootPanel.containsChild(menuContainer)) {
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
            menu.parentMenu = this.subMenuItem.getParentMenu();
            return menu;
        }

        @Override
        public Component<?> getComponent() {
            return this;
        }

        @Override
        public void selectWithFocus() {
//            Logging.info("selectWithFocus() ----- " + HasName.UTIL.getName(this));
            getWrappedComponent().setColor( getMenu().selectedColor );
            Position offset = Component.UTIL.getOffsetFromComponentContainer(getWrappedComponent(), getMenu().getMenuRootPanel(getWrappedComponent()));
            getMenu().popUpMenu(getWrappedComponent().getPostion_TopRight().offsetAdd(offset).offsetAdd(-1,1), subMenuItem.getParentMenu().menuContainer); 
        }

        @Override
        public void unSelect() {
            Logging.info("unSelect() --------" + HasName.UTIL.getName(this));
            getWrappedComponent().setColor( getMenu().color );
            if (getMenu().isVisible()) {
                getMenu().popDownMenu();
            }
        }
        @Override
        public void selectWithNoFocus() {
            selectWithNoFocus(null);
        }

        @Override
        public void selectWithNoFocus(Focusable newFocusable) {
//            Logging.info("selectWithNoFocus" + HasName.UTIL.getName(newFocusable, "<none>"));
            Menu menu = getMenu();
            Color color = menu.color;
            color = color.bold();
            getWrappedComponent().setColor( color );
            if (getMenu().isVisible()) {
                if (newFocusable == null) {
                    getMenu().popDownMenu();
                    return;
                }
                if (newFocusable instanceof MenuContainer) {
                    MenuContainer clickedMenuContainer = (MenuContainer)newFocusable;
                    if (clickedMenuContainer.menu == this.getMenu()) {
                        return;
                    }
                    getMenu().popDownMenu();
                }
            }
        }

    }

    static class MenuContainer extends AbstractWrappedComponentWithFocusManager<ListBox<Object>> {

        Menu menu;
        protected MenuContainer(Menu menu, Color color) {
            super(color, new ListBox<Object>());
            this.addFocusable( getListBox().asFocusable() );
            getListBox().setBorder(true);
            getListBox().setEventTriggerForValueChanged(ListBox.VALUE_CHANGED_ON_ENTER_KEY |
                                                        ListBox.VALUE_CHANGED_ON_MOUSE_CLICK);
            if (color != null) {
                getListBox().setColor(color);
            }
            this.menu = menu;

            getListBox().appendEventHandler(new KeyInputEventHandler(Keys.RIGHT_KEYSTROKE, 
                     (te)-> {
                         Logging.info(HasName.UTIL.getName(MenuContainer.this)+" " + "RIGHT KEY CLICK");
                         menu.selectNextRight(te);
                     }));
            getListBox().appendEventHandler(new KeyInputEventHandler(Keys.LEFT_KEYSTROKE, 
                    (te)-> {
                        Logging.info(HasName.UTIL.getName(MenuContainer.this)+" " + "LEFT KEY CLICK");
                        menu.selectNextLeft(te);
                    }));

            getListBox().appendEventHandler(new KeyInputEventHandler(Keys.UP_KEYSTROKE, 
                    (te)->  getListBox().getSelectedIndex() == 0,
                    (te)->  {
                             selectLast(te);
                            }));
            getListBox().appendEventHandler(new KeyInputEventHandler(Keys.DOWN_KEYSTROKE, 
                    (te)->  getListBox().getSelectedIndex() == (getListBox().size() - 1),
                    (te)->  {
                             selectFirst(te);
                            }));
        }

        // convenience method
        protected ListBox<Object> getListBox() {
            ListBox<Object> listBox = this.getWrappedComponent();
            return listBox;
        }

        public MenuItem getSelected() {
            return (MenuItem) getListBox().getSelected();
        }

        public void onChange(EventHandler consumeChange) {
            Logging.info("onChange()  idx = " +  getListBox().getSelectedIndex());
            getListBox().onChange(consumeChange);
        }

        public void setItems(List<DisplayableValue<Object>> menuItemsAsDisplayableItems, MenuTextComponentItemFactory menuTextComponentItemFactory) {
            getListBox().setItems(menuItemsAsDisplayableItems, menuTextComponentItemFactory);
        }

        public void setSelectedColor(Color selectedColor) {
            getListBox().setSelectedColor(selectedColor);
        }

        public void setBorder(boolean hasBorder) {
            getListBox().setBorder(hasBorder);
        }

        public void unFocus(Focusable newFocusable, EventBus eventBus) {
            Logging.info(HasName.UTIL.getName(this)+ " unFocus   to next: " + HasName.UTIL.getName(newFocusable, "<none>"));

            super.unFocus(newFocusable, eventBus);
            if (!this.menu.isFocusablePartOfMenu(newFocusable)) {
                this.menu.popDownMenu();
                // the menu bar lets go only if focus leaves it - not when it moves to one of its own
                // buttons (the open menu is part of the menu bar for focus)
                Focusable menuBarNode = this.menu.menuBar == null ? null : ToFocusable.UTIL.toFocusable(this.menu.menuBar);
                if (menuBarNode != null && (newFocusable == null || !FocusManager.UTIL.containsFocusable(menuBarNode, newFocusable))) {
                    this.menu.menuBar.unFocus(newFocusable, eventBus);
                }
            }
        }

        public void focus(Focusable oldFocusable, EventBus eventBus) {
            Logging.info(HasName.UTIL.getName(this)+ " focus  from prev: " + HasName.UTIL.getName(oldFocusable, "<none>"));
            super.focus(oldFocusable, eventBus);
        }

        public void selectFirst(TerminalEvent<?> te) {
            getListBox().setSelected(0);
            // need to move focus to listbox and then tell canvas we are dirty
            te.getEventBus().raiseEvent(new RequestFocusEvent(this.getListBox().asFocusable()));
            te.getEventBus().raiseEvent(  new RequestUpdateCanvasEvent(this) );
        }

        public void selectLast(TerminalEvent<?> te) {
            if (getListBox().size() > 0) {
                getListBox().setSelected(getListBox().size()-1);
                // need to move focus to listbox and then tell canvas we are dirty
                te.getEventBus().raiseEvent(new RequestFocusEvent(this.getListBox().asFocusable()));
                te.getEventBus().raiseEvent(  new RequestUpdateCanvasEvent(this) );
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

    public void setMenuBar(MenuBar menuBar) {
        this.menuBar = menuBar;
    }

    @Override
    public void selectNextLeft(TerminalEvent<?> te) {
        this.popDownMenu();
        if (this.parentMenu != null) {
            this.parentMenu.getMenuContainer().selectFirst(te);
        } else if (this.menuBar != null) {
            this.menuBar.focus(null, te.getEventBus());
            this.menuBar.selectNextLeft(te);
        }
        te.getEventBus().raiseEvent(  new RequestUpdateCanvasEvent(this) );
    }

    @Override
    public void selectNextRight(TerminalEvent<?> te) {
        MenuItem current = this.getSelected();
        if (current instanceof SubMenu) {
            SubMenu subMenu = (SubMenu) current;
            subMenu.onSelected(te);
            if (subMenu.getMenu().menuContainer != null) {
                subMenu.getMenu().menuContainer.selectFirst(te);
            }
        } else if (this.menuBar != null) {
            this.menuBar.focus(null, te.getEventBus());
            this.menuBar.selectNextRight(te);
        }
        te.getEventBus().raiseEvent(  new RequestUpdateCanvasEvent(this) );
    }

}
