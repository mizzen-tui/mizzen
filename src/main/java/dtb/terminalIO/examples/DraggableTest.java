package dtb.terminalIO.examples;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.DefaultFocusableBehaviour;
import dtb.terminalIO.components.input.DraggableTextComponent;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.events.mouse.ApplicationMouseEvent;
import dtb.terminalIO.events.mouse.Draggable;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.StringUtils;

public class DraggableTest {

    public static void main(String args[]) {

        //Composite.DRAW_VIA_PARTS = false;
        
        Logging.setEnabled(true);

        new TUIApp(args)
        .setEmptyChar('.')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)->{

            Button toggleDrawType = new Button(Composite.DRAW_VIA_PARTS ? "Drawing via Parts" : "Drawing traditional");
            toggleDrawType.onChange((te)-> {
               Composite.DRAW_VIA_PARTS = ! Composite.DRAW_VIA_PARTS;
               toggleDrawType.setButtonText(Composite.DRAW_VIA_PARTS ? "Drawing via Parts" : "Drawing traditional");
               te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(toggleDrawType));
            });

            tApp.getMainApplicationPanel().add(5, 3, new BorderedComponent( toggleDrawType ));

            tApp.getMainApplicationPanel().add(10, 3, new BorderedComponent( new DraggableTextComponent(Colors.Red,"RED")));

            tApp.getMainApplicationPanel().add(14, 3, new DraggableTextComponent(Colors.Blu,"BLUE"));

            String largeText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis nulla elit, placerat sed aliquam at, porta ut justo. Sed vel fringilla nisi, eu laoreet dui. Sed gravida at ipsum et viverra. Ut diam justo, imperdiet sed ante eu, elementum pellentesque sem. Curabitur eleifend turpis eros, nec semper est ultricies eget. Sed sed faucibus mauris, sed porttitor mauris. Ut quis tellus ultrices sapien porttitor tincidunt eu sed risus. Integer ac imperdiet dolor, at ultricies odio. Vestibulum sed pretium ligula. Sed dapibus sapien nunc, eget molestie mauris hendrerit id. Duis blandit tortor eget risus sagittis, at sodales felis congue. Donec vulputate massa aliquam, pretium ante eu, hendrerit ante. Suspendisse potenti. Nam gravida tempus ipsum, non convallis turpis malesuada id. Sed iaculis iaculis condimentum. Fusce pharetra euismod efficitur.";

            tApp.getMainApplicationPanel().add(14, 3, new BorderedComponent(new DraggableTextComponent(Colors.BGre,StringUtils.wrap(largeText, 50))));

            tApp.getMainApplicationPanel().add(20, 3, new BorderedComponent(new DraggableTextComponent(Colors.BGre,getFileData(args))));

            Composite innerComposite = new Composite().setName("innerComposite");
            innerComposite.setBorder(true);
            innerComposite.setFillOnClear(true);
            innerComposite.setEmptyChar(' ');
            innerComposite.add(1, 4, new TextComponent("[Inner composite]"));
            innerComposite.add(3, 2, new TextComponent("With 2 text components"));
            tApp.getMainApplicationPanel().add(22, 3, innerComposite );

            Composite horizontalComposite = new HorizontalPanel().setHorizontalSpacer(2).setName("horizontalComposite");
            horizontalComposite.setBorder(true);
            horizontalComposite.setFillOnClear(true);
            horizontalComposite.add(new TextComponent("---HorizontalComposite-- "));
            horizontalComposite.add(new TextComponent("With 2 text components\nRight here in Texas!"));
            tApp.getMainApplicationPanel().add(33, 3, horizontalComposite );

            FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(
                                                tApp.getMainApplicationPanel(), 
                                                tApp.getEventBus(),
                                                () -> new DefaultFocusManager().setPrevKeyStroke(Keys.SHIFT_TAB_KEYSTROKE)
                                                                               .setNextKeyStroke(Keys.TAB_KEYSTROKE)
                                                                               .setNavKeysFirst(false)
                                                                               .setName("mainFocusManager")
                                                                               .setCycling(false));

            tApp.getEventBus().appendEventHandler(mainFocusManager);

        })
        .start();
        
    }
    
    private static String getFileData(String[] args)  {
        if (args.length == 0) {
            return "Filename not specified";
        }
        
        String fileName = args[0]; 

        // TODO: validation required (file exists etc)

        try {
            byte[] fileByteData = Files.readAllBytes(Paths.get(fileName));

            //String fileData  =  new String(fileByteData,StandardCharsets.UTF_8);
            String fileData = new String(fileByteData,StandardCharsets.UTF_8).replace(ControlCode.ERASE_END_OF_LINE.asString(),"");
            fileData = fileData.replace("\t","    ");
            return fileData;
        } catch (IOException e) {
            return e.getMessage();
        }
    }
    
    static class DraggableTextComponent11 extends TextComponent implements Focusable , Draggable {

        Focusable meAsFocusable;
        ChainedEventHandler chainedHandler;
        String baseText;

        public DraggableTextComponent11(Color color, String text) {
            super(color, hilitedText(text, false));
            this.baseText = text;
           
        }

        private Focusable asFocusable() {

            if (meAsFocusable != null) {
                return meAsFocusable;
            }

            chainedHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);
            
            meAsFocusable =  new DefaultFocusableBehaviour.HiliteOnFocus(this) {
                @Override
                public void setFocus(boolean focus, EventBus eventBus) {
                    super.setFocus(focus, eventBus);
                    DraggableTextComponent11.this.setText(hilitedText(baseText, focus));
                }
            };
            return meAsFocusable;
        }

        private static String hilitedText(String baseText, boolean hilited) {
            if (hilited) {
                return "[" + baseText + "]";
            }
            return " " + baseText + " ";
        }

        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            if (asFocusable().canHandle(te) != null) {
                return this;
            }
            return null;
        }

        @Override
        public void handle(TerminalEvent<?> te) {
            asFocusable().handle(te);
        }

        @Override
        public boolean onDragStart(ApplicationMouseEvent appMouseEvent) {
            Logging.info(HasName.UTIL.getName(this)+":onDragStart evt:" + String.valueOf(appMouseEvent));
            return true;
        }

        @Override
        public boolean onDrag(ApplicationMouseEvent appMouseEvent) {
            Logging.info(HasName.UTIL.getName(this)+":onDrag evt:" + String.valueOf(appMouseEvent));
            Component<?> componentToMove = this.getParentContainer().findChildOrAncestorThatCanBeMoved(this);
            if (componentToMove == null) {
                return false;
            }

            componentToMove.getParentContainer().moveChildToPosition(appMouseEvent.getMousePosition(), componentToMove);
            appMouseEvent.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(this)  );
            return true;
        }

        @Override
        public boolean onDragStop(ApplicationMouseEvent appMouseEvent) {
            Logging.info(HasName.UTIL.getName(this)+":onDragStop evt:" + String.valueOf(appMouseEvent));
            appMouseEvent.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(this)  );
            return true;
        }

        @Override
        public boolean hasFocus() {
            return asFocusable().hasFocus();
        }

        @Override
        public void setFocus(boolean focus, EventBus eventBus) {
            asFocusable().setFocus(focus, eventBus);
        }
        
        @Override
        public FocusManager getParentFocusManager() {
            return asFocusable().getParentFocusManager();
        }

        @Override
        public void setParentFocusManager(FocusManager focusManager) {
            asFocusable().setParentFocusManager(focusManager);
        }
        
        
    }


}
