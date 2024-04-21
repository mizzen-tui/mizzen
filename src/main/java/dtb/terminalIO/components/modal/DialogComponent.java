package dtb.terminalIO.components.modal;

import dtb.terminalIO.components.AbstractComponentWithContainer;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.CompositeWithFocusManager;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;

public class DialogComponent extends AbstractComponentWithContainer<CompositeWithFocusManager>{

    public static enum IconStyle {
        QUESTION,
        INFO,
        WARNING,
        ERROR
    }
    
    
    public static Color iconToIconColor(IconStyle icon) {
        switch (icon) {
        case QUESTION:
            return new Colors.FGBGColors(Colors.BIBla, Colors.On_IGre);
        case INFO:
            return new Colors.FGBGColors(Colors.BIBla, Colors.On_IPur);
        case WARNING:
            return new Colors.FGBGColors(Colors.BIBla, Colors.On_Yel);
        case ERROR:
            return new Colors.FGBGColors(Colors.BIWhi, Colors.On_IRed);

        default:
            return null;
        }
    }

    public static Component<?> iconToIconComponent(IconStyle icon) {
        switch (icon) {
        case QUESTION:
             return questionICON();
        case INFO:
            return informationICON();
        case WARNING:
            return warningICON();
        case ERROR:
            return errorICON();

        default:
            return null;
        }
    }
    
    
    private static Component<?> makeICON(char icon, Color iconColor) {
        BorderedComponent question = new BorderedComponent(iconColor, new TextComponent(iconColor," " + icon + " "));
        question.setName("icon-"+icon);
        return question;
    }
    

    private static Component<?> questionICON() {
        return makeICON('?', new Colors.FGBGColors(Colors.BIBla, Colors.On_IGre));
    }
    
    private static Component<?> informationICON() {
        return makeICON('i', new Colors.FGBGColors(Colors.BIBla, Colors.On_IPur));
    }
    
    private static Component<?> warningICON() {
        return makeICON('!', new Colors.FGBGColors(Colors.BIBla, Colors.On_Yel));
    }
    
    private static Component<?> errorICON() {
        return makeICON('X', new Colors.FGBGColors(Colors.BIWhi, Colors.On_IRed));
    }
    
    // internal class to specify buttons that will be built in call to simpleDialog(..)
    
    @FunctionalInterface
    public static interface DialogKeyPressed {
    	void handleKeyPressed(EventHandler onCloseRequest, TerminalEvent<?> te);
    }
    
    public static class ButtonSpec {
        public final String buttonLabel;
        public final KeyStroke onPressedKeyStroke;
        public final DialogKeyPressed onPressed;
        public final boolean removeOnPressed;
        
        public ButtonSpec(String buttonLabel, KeyStroke onPressedKeyStroke, DialogKeyPressed onPressed, boolean removeOnPressed) {
            super();
            this.buttonLabel = buttonLabel;
            this.onPressedKeyStroke = onPressedKeyStroke;
            this.onPressed = onPressed;
            this.removeOnPressed = removeOnPressed;
        }
        
        public ButtonSpec(String buttonLabel, KeyStroke onPressedKeyStroke, EventHandler onPressed, boolean removeOnPressed) {
            super();
            this.buttonLabel = buttonLabel;
            this.onPressedKeyStroke = onPressedKeyStroke;
            this.onPressed = (onCloseRequest, te) -> onPressed.handle(te);
            this.removeOnPressed = removeOnPressed;
        }
        public ButtonSpec(String buttonLabel, KeyStroke onPressedKeyStroke, EventHandler onPressed) {
            this(buttonLabel, onPressedKeyStroke, onPressed, true);
        }
    }
    
    Composite mainVerticalPanelForLayout;
    IconStyle icon;
    
    
    public DialogComponent(Color color, IconStyle icon,  String title) {
        super(color, new CompositeWithFocusManager(color));

        this.icon = icon;

        Color iconColor = iconToIconColor(icon);

        setBorderColor(iconColor);
        setBorder(true); 
        if (icon != null) {
            setTitle(iconColor, " " + title + " ");
        } else {
            setTitle(" " + title + " ");
        }
        setFillOnClear(true);
        setEmptyColor(getColor());

        mainVerticalPanelForLayout = new VerticalPanel(color).setVerticalSpacer(1);
        add(1,1, mainVerticalPanelForLayout);

    }

    public void show(EventBus eventBus) {
        // TODO Auto-generated method stub
        
    }

    public void onClose(EventHandler eventHandler) {
        // TODO Auto-generated method stub
        
    }

    public void setDialogMessage(String message) {
        if (icon != null && Dimension.UTIL.stringToDimension(message).getHeight()==1) {
            message = "\n" + message;
        }

        TextComponent messageText = new TextComponent(getColor(), message);
        messageText.setFillOnClear(true);
        messageText.setEmptyColor(getColor());
        
        HorizontalPanel messagePanel = new HorizontalPanel(getColor()).setHorizontalSpacer(2);
        messagePanel.setFillOnClear(true);
        messagePanel.setEmptyColor(getColor());
        if (icon != null) {
            messagePanel.add(iconToIconComponent(icon));
        }
        messagePanel.add(messageText);

        Position messageTextPosition = new Position.Simple(1,1);

        mainVerticalPanelForLayout.add(messagePanel);
        
        
    }

}
