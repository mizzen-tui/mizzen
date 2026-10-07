package dtb.terminalIO.components.modal;

import dtb.terminalIO.components.ChildComponentManager;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.modal.DialogComponent.ButtonSpec;
import dtb.terminalIO.components.modal.DialogComponent.DialogKeyPressed;
import dtb.terminalIO.components.modal.DialogComponent.IconStyle;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.StringUtils;

public class MessageBox {
    
//    static enum IconStyle {
//        QUESTION,
//        INFO,
//        WARNING,
//        ERROR
//    }
    
    
//    static Color iconToIconColor(IconStyle icon) {
//        switch (icon) {
//        case QUESTION:
//            return new Colors.FGBGColors(Colors.BIBla, Colors.On_IGre);
//        case INFO:
//            return new Colors.FGBGColors(Colors.BIBla, Colors.On_IPur);
//        case WARNING:
//            return new Colors.FGBGColors(Colors.BIBla, Colors.On_Yel);
//        case ERROR:
//            return new Colors.FGBGColors(Colors.BIWhi, Colors.On_IRed);
//
//        default:
//            return null;
//        }
//    }

//    static Component<?> iconToIconComponent(IconStyle icon) {
//        switch (icon) {
//        case QUESTION:
//             return questionICON();
//        case INFO:
//            return informationICON();
//        case WARNING:
//            return warningICON();
//        case ERROR:
//            return errorICON();
//
//        default:
//            return null;
//        }
//    }
    
//    private static Component<?> makeICON(char icon, Color iconColor) {
//        BorderedComponent question = new BorderedComponent(iconColor, new TextComponent(iconColor," " + icon + " "));
//        return question;
//    }
//    
//
//    private static Component<?> questionICON() {
//        return makeICON('?', new Colors.FGBGColors(Colors.BIBla, Colors.On_IGre));
//    }
//    
//    private static Component<?> informationICON() {
//        return makeICON('i', new Colors.FGBGColors(Colors.BIBla, Colors.On_IPur));
//    }
//    
//    private static Component<?> warningICON() {
//        return makeICON('!', new Colors.FGBGColors(Colors.BIBla, Colors.On_Yel));
//    }
//    
//    private static Component<?> errorICON() {
//        return makeICON('X', new Colors.FGBGColors(Colors.BIWhi, Colors.On_IRed));
//    }
    
    // internal class to specify buttons that will be built in call to simpleDialog(..)
//    private static class ButtonSpec {
//        public final String buttonLabel;
//        public final KeyStroke onPressedKeyStroke;
//        public final EventHandler onPressed;
//        
//        public ButtonSpec(String buttonLabel, KeyStroke onPressedKeyStroke, EventHandler onPressed) {
//            super();
//            this.buttonLabel = buttonLabel;
//            this.onPressedKeyStroke = onPressedKeyStroke;
//            this.onPressed = onPressed;
//        }
//    }
    
    /**
     * Render a simple messagebox dialog with buttons 
     * @param icon
     * @param title
     * @param buttonSpec
     * @param eventBus
     * @param parentPanel
     * @param message
     */
    public static void simpleDialog(IconStyle icon,
                                     String title,
                                     ButtonSpec[] buttonSpec,
                                     EventBus eventBus, 
                                     ChildComponentManager<?> parentPanel,
                                     String message,
                                     Component<?> customComponent,
                                     EventHandler onCloseHandler ) {

        Color dialogColor = new Colors.FGBGColors(Colors.Bla,Colors.On_Whi);
        dialogColor.setBoldTransform((c)-> new Colors.FGBGColors(Colors.BIWhi, Colors.On_Bla));
        dialogColor.setInvertTransform((c)-> new Colors.FGBGColors(Colors.BIWhi, Colors.On_Bla));

        ModalPanel dialog = new ModalPanel( dialogColor );      // modal: TAB / SHIFT-TAB go round inside it

        dialog.setBorderColor(DialogComponent.iconToIconColor(icon));
        dialog.setBorder(true); 
        if (title != null && title.length()>0) {
            dialog.setTitle(DialogComponent.iconToIconColor(icon), " " + title + " ");
        }
        dialog.setFillOnClear(true);
        dialog.setEmptyColor(dialogColor);
        dialog.setName("dialog");

        if (icon != null && Dimension.UTIL.stringToDimension(message).getHeight()==1) {
            message = "\n" + message;
        }

        TextComponent messageText = new TextComponent(dialog.getColor(), message);
        messageText.setFillOnClear(true);
        messageText.setEmptyColor(dialogColor);
        messageText.setName("dialog.messageText");

        VerticalPanel mainPanel = new VerticalPanel(dialogColor).setVerticalSpacer(1);
        mainPanel.setName("dialog.mainPanel");

        HorizontalPanel messagePanel = new HorizontalPanel(dialogColor).setHorizontalSpacer(2);
        messagePanel.setFillOnClear(true);
        messagePanel.setEmptyColor(dialogColor);
        if (icon != null) {
            messagePanel.add(DialogComponent.iconToIconComponent(icon));
        }
        messagePanel.add(messageText);
        messagePanel.setName("dialog.messagePanel");

        Position messageTextPosition = new Position.Simple(1,1);

        mainPanel.add(messagePanel);
        if (customComponent != null) {
            mainPanel.add(customComponent);
        }

        dialog.add(messageTextPosition, mainPanel);

        HorizontalPanel buttons = new HorizontalPanel();
        TextComponent buttonLeftPadding = new TextComponent(dialog.getColor(), "");
        buttons.setFillOnClear(true);
        buttons.setEmptyColor(dialog.getColor());

        buttons.setColor(dialog.getColor());
        buttons.setHorizontalSpacer(3);
        buttons.setName("dialog.buttons");
        

        buttons.add(buttonLeftPadding);

        Button[] buttonsForButtonBar = new Button[buttonSpec.length];

        for (int i=0; i<buttonSpec.length; i++) {
            Button theButton = new Button(dialog.getColor(), buttonSpec[i].buttonLabel);
            theButton.setHotKey(buttonSpec[i].onPressedKeyStroke);
            theButton.setBorder(true);
            buttons.add(theButton);
            buttonsForButtonBar[i] = theButton; //buttons.addButton(buttonSpec[i].buttonLabel, buttonSpec[i].onPressedKeyStroke, null).setBorder(true);
        }

        int buttonPositionCol = (dialog.getWidth() - buttons.getWidth()) / 2 ;
        if (buttonPositionCol >3) {
        	buttonLeftPadding.setTextAndResize(StringUtils.repeat(" ", buttonPositionCol-3));
            //buttonPositionCol = messageTextPosition.getCol();
        }

        mainPanel.add(buttons);

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
            dialog.asFocusable().setFocus(false, te.getEventBus());
            te.getEventBus().removeEventHandler(modalDialogEvents);
            te.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(dialog)  );
            if (onCloseHandler != null) {
            	onCloseHandler.handle(te);
            }

        };

        for (int i=0; i< buttonsForButtonBar.length; i++) {
        	DialogKeyPressed buttonKeyPressed = buttonSpec[i].onPressed;
        	boolean removeOnPressed = buttonSpec[i].removeOnPressed;
            EventHandler onPressedButtonEventHandler = (te)-> buttonKeyPressed.handleKeyPressed(removeDialog,te);
            buttonsForButtonBar[i].onChange((te)->{
                if (removeOnPressed) {
                    removeDialog.handle(te);
                }
                if (onPressedButtonEventHandler != null) {
                    te.getEventBus().asynch(onPressedButtonEventHandler);
                }
            });
        }

        EventHandler noActionHandler = (te)-> {
            removeDialog.handle(te);
        };

        modalDialogEvents.appendEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, noActionHandler ) );


        modalDialogEvents.appendEventHandler(dialog.asEventHandler());
//        if (customComponent != null && customComponent instanceof ConditionalEventHandler) {
//            modalDialogEvents.appendEventHandler((ConditionalEventHandler)customComponent);
//        }
        

        eventBus.raiseEvent( new RequestUpdateCanvasEvent(dialog));

        eventBus.raiseEvent( new RequestFocusEvent(buttonsForButtonBar[0]));

    }

    public static void showDialogNew(DialogComponent.IconStyle icon,
            String title,
            ButtonSpec[] buttonSpec,
            EventBus eventBus, 
            ChildComponentManager<?> parentPanel,
            String message,
            Component<?> customComponent,
            EventHandler onCloseHandler ) {

        Color dialogColor = new Colors.FGBGColors(Colors.Bla,Colors.On_Whi);
        dialogColor.setBoldTransform((c)-> new Colors.FGBGColors(Colors.BIWhi, Colors.On_Bla));
        dialogColor.setInvertTransform((c)-> new Colors.FGBGColors(Colors.BIWhi, Colors.On_Bla));

        DialogComponent dialog = new DialogComponent(dialogColor, icon, title);

        dialog.show(eventBus);

    }

    public static void simpleDialog(IconStyle icon,
            String title,
            ButtonSpec[] buttonSpec,
            EventBus eventBus, 
            ChildComponentManager<?> parentPanel,
            String message,
            Component<?> customComponent) {
        simpleDialog(icon, title, buttonSpec, eventBus, parentPanel, message, customComponent, EventHandler.NULL_HANDLER);
    }

    public static void showInfoDialog(EventBus eventBus, 
                                      ChildComponentManager<?> parentPanel,
                                      String message,
                                      EventHandler  onPressed) {
        simpleDialog(IconStyle.INFO, "Info", new ButtonSpec[] {new ButtonSpec("OK", Keys.ENTER_KEYSTROKE, onPressed)}, eventBus, parentPanel, message, null );
    }

    public static void showInfoDialog(EventBus eventBus, 
                                      ChildComponentManager<?> parentPanel,
                                      String message,
                                      Component<?> customComponent,
                                      EventHandler  onPressed) {
         simpleDialog(IconStyle.INFO, "Info", new ButtonSpec[] {new ButtonSpec("OK", Keys.ENTER_KEYSTROKE, onPressed)}, eventBus, parentPanel, message, customComponent );
    }

    public static void showInfoDialog(EventBus  eventBus, 
                                      ChildComponentManager<?> parentPanel,
                                      String message) {
        showInfoDialog(eventBus, parentPanel, message, EventHandler.NULL_HANDLER);
    }

    public static void showInfoDialog(EventBus  eventBus, 
                                      ChildComponentManager<?> parentPanel,
                                      String message,
                                      Component<?> customComponent) {
        showInfoDialog(eventBus, parentPanel, message, customComponent, EventHandler.NULL_HANDLER);
    } 

    public static void showInfoDialog(EventBus  eventBus, 
                                        ChildComponentManager<?> parentPanel,
                                        String message,
                                        Component<?> customComponent,
                                        EventHandler  onPressed,
                                        EventHandler onCloseHandler) {
        showInfoDialog(eventBus, parentPanel, message, customComponent, EventHandler.NULL_HANDLER);
    }

    public static void showWarningDialog(EventBus  eventBus, 
                                         ChildComponentManager<?> parentPanel,
                                         String message,
                                         EventHandler onPressed) {
         simpleDialog(IconStyle.WARNING, "Warning", new ButtonSpec[] {new ButtonSpec("OK", Keys.ENTER_KEYSTROKE, onPressed)}, eventBus, parentPanel, message, null);
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
          simpleDialog(IconStyle.ERROR, "Error", new ButtonSpec[] {new ButtonSpec("OK", Keys.ENTER_KEYSTROKE, onPressed)}, eventBus, parentPanel, message, null);
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

        simpleDialog(IconStyle.QUESTION, " Yes / No ", new ButtonSpec[] {new ButtonSpec("Yes", KeyStroke.caseInsensitive('y'), onYesAction),new ButtonSpec("No", KeyStroke.caseInsensitive('n'), EventHandler.NULL_HANDLER)}, eventBus, parentPanel, message, null);
    }

}
