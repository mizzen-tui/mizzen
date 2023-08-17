package dtb.terminalIO.components.menu;

import dtb.terminalIO.components.AbstractComponentWrappper;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Hiliter;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;

public class MenuBar extends AbstractComponentWrappper<ButtonBar> {
    
    Menu currentActiveMenu = null;
    Button currentMenuButton = null;
    

    public MenuBar(Color color) {
        super(color, new ButtonBar(color));
        this.getWrappedComponent().setNoBorderChars();
    }

    public void addDropDownMenu(String menuButtonText, Menu menu) {
        Button menuButton = new Button(this.getColor(), menuButtonText);
        menuButton.onChange(onMenuItemSelectedEventHandler(menuButton, menu));
        menuButton.setHiliter(Hiliter.NOOP);
        
        menu.setColor(getColor());
        menu.setSelectedColor(getColor().invert());

        this.getWrappedComponent().addComponent(menuButton);
    }
    
    private EventHandler onMenuItemSelectedEventHandler(Button menuButton, Menu menu) {
        return new EventHandler() {
            
            @Override
            public void handle(TerminalEvent<?> te) {
                
                if (currentActiveMenu != null && currentActiveMenu != menu) {
                    currentActiveMenu.popDownMenu();
                    currentActiveMenu = null;
                    currentMenuButton.setColor( getColor()); 
                    currentMenuButton = null;
                }
                
                menu.popUpMenu( Component.UTIL.getCanvasOffsetFor(menuButton).addRows(0) , menuButton);
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
                
                te.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(MenuBar.this));
            }
        };
    }

}
