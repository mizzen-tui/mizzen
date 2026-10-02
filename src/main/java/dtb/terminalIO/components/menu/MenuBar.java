package dtb.terminalIO.components.menu;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.components.AbstractWrappedComponentWithFocusManager;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.Hiliter;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.menu.MenuItem.LeftRightSelector;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.Logging;

public class MenuBar extends AbstractWrappedComponentWithFocusManager<ButtonBar> implements LeftRightSelector {

    Menu currentActiveMenu = null;
    MenuBarButton currentMenuButton = null;
    List<MenuBarButton> menuBarItems = new ArrayList<MenuBarButton>();

    public MenuBar(Color color) {
        super(color, new ButtonBar(color));
        this.getWrappedComponent().setNoBorderChars();

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

        this.getWrappedComponent().addComponent(menuButton);
        this.menuBarItems.add(menuButton);
    }

    @Override
    public void unFocus(Focusable newFocusable, EventBus eventBus) {
        super.unFocus(newFocusable, eventBus);
        if (currentActiveMenu != null) {
            if (currentActiveMenu.isFocusablePartOfMenu(newFocusable)) {
                return;
            }
            // the new focusable component is not part of this menu
            // so do normal unfocus behaviour which is to pop down (remove) the menu
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
