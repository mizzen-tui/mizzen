package dtb.terminalIO.components.modal;

import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.ChildComponentManager;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;

public class MessageBox {
    
    static enum IconStyle {
        QUESTION,
        INFO,
        WARNING,
        ERROR
    }
    
    
    static Color iconToIconColor(IconStyle icon) {
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

    static Component<?> iconToIconComponent(IconStyle icon) {
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
    private static class ButtonSpec {
        public final String buttonLabel;
        public final KeyStroke onPressedKeyStroke;
        public final EventHandler onPressed;
        
        public ButtonSpec(String buttonLabel, KeyStroke onPressedKeyStroke, EventHandler onPressed) {
            super();
            this.buttonLabel = buttonLabel;
            this.onPressedKeyStroke = onPressedKeyStroke;
            this.onPressed = onPressed;
        }
    }
    
    /**
     * Render a simple messagebox dialog with buttons 
     * @param icon
     * @param title
     * @param buttonSpec
     * @param eventBus
     * @param parentPanel
     * @param message
     */
    private static void simpleDialog(IconStyle icon,
                                     String title,
                                     ButtonSpec[] buttonSpec,
                                     EventBus eventBus, 
                                     ChildComponentManager<?> parentPanel,
                                     String message) {

        Color dialogColor = new Colors.FGBGColors(Colors.Bla,Colors.On_Whi);
        dialogColor.setBoldTransform((c)-> new Colors.FGBGColors(Colors.BIWhi, Colors.On_Bla));
        dialogColor.setInvertTransform((c)-> new Colors.FGBGColors(Colors.BIWhi, Colors.On_Bla));

        Composite dialog = new Composite( dialogColor );

        dialog.setBorderColor(iconToIconColor(icon));
        dialog.setBorder(true); 
        dialog.setTitle(iconToIconColor(icon), " " + title + " ");
        dialog.setFillOnClear(true);
        dialog.setEmptyColor(dialogColor);
        
        if (icon != null && Dimension.UTIL.stringToDimension(message).getHeight()==1) {
            message = "\n" + message;
        }

        TextComponent messageText = new TextComponent(dialog.getColor(), message);
        messageText.setFillOnClear(true);
        messageText.setEmptyColor(dialogColor);

        HorizontalPanel messagePanel = new HorizontalPanel(dialogColor).setHorizontalSpacer(2);
        messagePanel.setFillOnClear(true);
        messagePanel.setEmptyColor(dialogColor);
        if (icon != null) {
            messagePanel.add(iconToIconComponent(icon));
        }
        messagePanel.add(messageText);
        
        Position messageTextPosition = new Position.Simple(1,1);
        
        dialog.add(messageTextPosition, messagePanel);
        
        ButtonBar buttons = new ButtonBar().setCycling(true);
        buttons.setFillOnClear(true);
        buttons.setEmptyColor(dialog.getColor());

        buttons.setColor(dialog.getColor());
        buttons.setNoBorderChars();
        buttons.setHorizontalSpacer(3);
        
        Button[] buttonsForButtonBar = new Button[buttonSpec.length];
        
        for (int i=0; i<buttonSpec.length; i++) {
            buttonsForButtonBar[i] = buttons.addButton(buttonSpec[i].buttonLabel, buttonSpec[i].onPressedKeyStroke, null).setBorder(true);
        }
        
        int buttonPositionCol = (dialog.getWidth() - buttons.getWidth()) / 2 ;
        if (buttonPositionCol < 0) {
            buttonPositionCol = messageTextPosition.getCol();
        }
        
        Position positionOfButtons = messageTextPosition.offsetAdd(new Position.Simple(messagePanel.getHeight()+2,buttonPositionCol));
        
        dialog.add( positionOfButtons, buttons );

        Position positionOfDialog = Position.ZERO;

        if (parentPanel instanceof Rectangle) {
            positionOfDialog = ((Rectangle)parentPanel).centered(dialog, Position.ZERO);
        }

        parentPanel.add( positionOfDialog, dialog );
        parentPanel.setDropShadow(true, dialog);
    
        //  -- bind dialog events to modalDialogEvents which in 
        //     turn is prepended to the eventBus --
        
        ModalEventHandler modalDialogEvents = new ModalEventHandler(eventBus);
        
        EventHandler removeDialog = (te)->{
            parentPanel.remove(dialog);
            te.getEventBus().removeEventHandler(modalDialogEvents);
            te.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(dialog)  );
        };
        
        for (int i=0; i< buttonsForButtonBar.length; i++) {
            EventHandler onPressedButtonEventHandler = buttonSpec[i].onPressed;
            buttonsForButtonBar[i].onChange((te)->{
                removeDialog.handle(te);
                te.getEventBus().appendEventHandler( new OnIdleEventEventHandler.OneTime(onPressedButtonEventHandler) );
            });
        }
        
        EventHandler noActionHandler = (te)-> {
            removeDialog.handle(te);
        };
        
        modalDialogEvents.appendEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, noActionHandler ) );

        modalDialogEvents.appendEventHandler(buttons);

        eventBus.raiseEvent( new RequestUpdateCanvasEvent(dialog));

        eventBus.raiseEvent( new RequestFocusEvent(buttonsForButtonBar[0]));

        
    }
    
    public static void showInfoDialog(EventBus eventBus, 
                                      ChildComponentManager<?> parentPanel,
                                      String message,
                                      EventHandler  onPressed) {
        simpleDialog(IconStyle.INFO, "Info", new ButtonSpec[] {new ButtonSpec("OK", Keys.ENTER_KEYSTROKE, onPressed)}, eventBus, parentPanel, message );

    }

    public static void showInfoDialog(EventBus  eventBus, 
                                      ChildComponentManager<?> parentPanel,
                                      String message) {
        showInfoDialog(eventBus, parentPanel, message, EventHandler.NULL_HANDLER);
    }
    
    public static void showWarningDialog(EventBus  eventBus, 
                                         ChildComponentManager<?> parentPanel,
                                         String message,
                                         EventHandler onPressed) {
         simpleDialog(IconStyle.WARNING, "Warning", new ButtonSpec[] {new ButtonSpec("OK", Keys.ENTER_KEYSTROKE, onPressed)}, eventBus, parentPanel, message);
    }
    
    public static void showWarningDialog(EventBus eventBus, 
            ChildComponentManager<?> parentPanel,
            String message) {
        showWarningDialog(eventBus, parentPanel, message, (te)->{});
    }
    
    public static void showErrorDialog(EventBus eventBus, 
                                       ChildComponentManager<?> parentPanel,
                                       String message,
                                       EventHandler onPressed) {
          simpleDialog(IconStyle.ERROR, "Error", new ButtonSpec[] {new ButtonSpec("OK", Keys.ENTER_KEYSTROKE, onPressed)}, eventBus, parentPanel, message);
    }
    
    public static void showErrorDialog(EventBus eventBus, 
            ChildComponentManager<?> parentPanel,
            String message) {
        showErrorDialog(eventBus, parentPanel, message, (te)->{});
    }
    
    static boolean integrate = true;
    
    public static void showYesNoDialog(EventBus eventBus, 
                                       ChildComponentManager<?> parentPanel,
                                       String message,
                                       EventHandler onYesAction) {

        simpleDialog(IconStyle.QUESTION, " Yes / No ", new ButtonSpec[] {new ButtonSpec("Yes", KeyStroke.caseInsensitive('y'), onYesAction),new ButtonSpec("No", KeyStroke.caseInsensitive('n'), EventHandler.NULL_HANDLER)}, eventBus, parentPanel, message);
    }

}
