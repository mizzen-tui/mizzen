package mizzen.components.menu;

import java.util.ArrayList;
import java.util.List;

import mizzen.events.focus.RequestFocusEvent;
import mizzen.components.ComponentWithFocus;
import mizzen.components.ButtonBar;
import mizzen.components.Component;
import mizzen.components.HasName;
import mizzen.components.Hiliter;
import mizzen.components.input.Button;
import mizzen.components.menu.MenuItem.LeftRightSelector;
import mizzen.controlcodes.color.Color;
import mizzen.events.EventHandler;
import mizzen.events.RequestUpdateCanvasEvent;
import mizzen.events.TerminalEvent;
import mizzen.events.keyboard.KeyInputEventHandler;
import mizzen.events.keyboard.Keys;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.util.Logging;

public class MenuBar extends ComponentWithFocus<ButtonBar, MenuBar> implements LeftRightSelector {

    Menu currentActiveMenu = null;
    MenuBarButton currentMenuButton = null;
    List<MenuBarButton> menuBarItems = new ArrayList<MenuBarButton>();

    public MenuBar(Color color) {
        super(color, new ButtonBar(color));
        getPanel().setNoBorderChars();

        // an open menu is part of the menu bar for focus (it is its button's popup): when focus leaves
        // both, the menu closes
        focus().whenFocusLeaves((bus) -> closeActiveMenu());

        // TODO's

        // Keydown -- needs to map to first item in menu
        //getWrappedComponent().appendEventHandler(eventHandler)

        // need to trap - left and right keys in order to select left or right

    }

    public void addDropDownMenu(String menuButtonText, Menu menu) {
        MenuBarButton menuButton = new MenuBarButton(this.getColor(), menuButtonText, this, menu);
        menuButton.onChange(onMenuItemSelectedEventHandler(menuButton, menu));
        menuButton.setHiliter(Hiliter.NOOP);

        menu.setColor(getColor());
        menu.setSelectedColor(getColor().invert());
        menu.setMenuBar(this);

        getPanel().addComponent(menuButton);
        this.menuBarItems.add(menuButton);
    }

    /** close the open menu, if any, and show its button as not chosen */
    void closeActiveMenu() {
        if (currentActiveMenu != null) {
            currentActiveMenu.popDownMenu();
            currentActiveMenu = null;
            currentMenuButton.setColor(getColor());
            currentMenuButton = null;
        }
    }

    private EventHandler onMenuItemSelectedEventHandler(MenuBarButton menuButton, Menu menu) {
        return new EventHandler() {

            @Override
            public void handle(TerminalEvent<?> te) {

                boolean currentAndMenuSame = (currentActiveMenu != null && currentActiveMenu == menu);

                if (currentActiveMenu != null) {
                    currentActiveMenu.popDownMenu();
                    currentActiveMenu = null;
                    currentMenuButton.setColor(getColor()); 
                    currentMenuButton = null;
                    if (currentAndMenuSame) {
                        te.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(MenuBar.this));
                        return;
                    }
                }

                menu.popUpMenu( Component.UTIL.getCanvasOffsetFor(menuButton).addRows(1) , menuButton);
                if (menu.isVisible()) {
                    currentActiveMenu = menu;
                    currentMenuButton = menuButton;
                    currentMenuButton.setColor( getColor().invert() );
                } else {
                    currentActiveMenu = null;
                    if (currentMenuButton != null) {
                        currentMenuButton.setColor( getColor());
                    }
                    currentMenuButton = null;
                }
                te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(MenuBar.this));
            }
        };
    }

    @Override
    public void selectNextLeft(TerminalEvent<?> te) {

        if (currentActiveMenu == null) {
            return;
        }

        Logging.info("menuBar selectLeft");
        int idx = this.menuBarItems.indexOf(currentMenuButton);
        if (idx == 0) {
            idx = this.menuBarItems.size()-1;
        } else {
            idx--;
        }
        Button nextButton = menuBarItems.get(idx);
        // move focus like a click (focus may be in the open menu, outside the menu bar)
        te.getEventBus().raiseEvent(new RequestFocusEvent(nextButton));
        nextButton.onClick(te.getEventBus());
    }

    @Override
    public void selectNextRight(TerminalEvent<?> te) {
        if (currentActiveMenu == null) {
            return;
        }
        Logging.info("menuBar selectRight");
        int idx = this.menuBarItems.indexOf(currentMenuButton);
        if (idx == this.menuBarItems.size()-1) {
            idx = 0;
        } else {
            idx++;
        }
        Button nextButton = menuBarItems.get(idx);
        // move focus like a click (focus may be in the open menu, outside the menu bar)
        te.getEventBus().raiseEvent(new RequestFocusEvent(nextButton));
        nextButton.onClick(te.getEventBus());
    }

    static class MenuBarButton extends Button {

        MenuBar menuBar;
        Menu menu;

        public MenuBarButton(Color color, String buttonText, MenuBar menuBar, Menu menu) {
            super(color, buttonText);
            this.menuBar = menuBar;
            this.menu = menu;
            this.appendEventHandler(new KeyInputEventHandler(Keys.DOWN_KEYSTROKE, 
                    (te)-> {
                        Logging.info(HasName.UTIL.getName(MenuBarButton.this)+" " + "DOWN KEY CLICK");
                        // warning - this feels weak - explain later .. ;-)
                        te.getEventBus().asynch((te2)->{
                            if (this.menu.menuContainer != null) {
                                this.menu.getMenuContainer().selectFirst(te2);
                            }
                        });
                    }));
            this.appendEventHandler(new KeyInputEventHandler(Keys.UP_KEYSTROKE, 
                    (te)-> {
                        Logging.info(HasName.UTIL.getName(MenuBarButton.this)+" " + "UP KEY CLICK");
                        // warning - this feels weak - explain later .. ;-)
                        te.getEventBus().asynch((te2)->{
                            if (this.menu.menuContainer != null) {
                                this.menu.getMenuContainer().selectLast(te2);
                            } 
                        });
                    }));
            this.appendEventHandler(new KeyInputEventHandler(Keys.LEFT_KEYSTROKE, 
                    (te)-> {
                        Logging.info(HasName.UTIL.getName(MenuBarButton.this)+" " + "LEFT KEY CLICK");
                        this.menuBar.selectNextLeft(te);
                    }));
            this.appendEventHandler(new KeyInputEventHandler(Keys.RIGHT_KEYSTROKE, 
                    (te)-> {
                        Logging.info(HasName.UTIL.getName(MenuBarButton.this)+" " + "RIGHT KEY CLICK");
                        this.menuBar.selectNextRight(te);
                    }));
        }
        
        @Override
        protected void draw(Color color, IScreenLineSegmentsBuffer mySegments) {
            // TODO Auto-generated method stub
            super.draw(color, mySegments);
        }
        
        
        @Override
        public void hilite() {
            super.hilite();
        }

        @Override
        public void unHilite() {
            super.unHilite();
        }

        
    }


}
