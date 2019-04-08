package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyInputEventsProducer;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.PositionVerticalRelative;
import dtb.terminalIO.util.Stty;

public class ButtonBarTest {

    public static void main(String[] args) throws IOException {

        TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);

        Composite container = new Composite();
        
        container.setBounds(new Dimension.Simple(20,50));
        
        TextComponent statusMSGs = new TextComponent(new FGBGColors(Colors.BBlu,Colors.On_IYel), new Dimension.Simple(1,container.getWidth()));
        
        ButtonBar bbar1 = new ButtonBar(Colors.IGre);

        Canvas master_canvas = new Canvas(screen);
        master_canvas.setSize(container.getHeight() + 4, container.getWidth() + 5);
        master_canvas.setEmptyChar(' ');

        container.add(bbar1);
        
        container.add( new PositionVerticalRelative(bbar1, 3), statusMSGs);

        
        Runnable renderComponents = () ->{
                master_canvas.addComponentSegments(container);
                master_canvas.refresh();
        };
        
        bbar1.addButton("B1", Keys.F1_KEYSTROKE, (te)-> statusMSGs.setText("B1 Pressed"));
        bbar1.addButton("B2", Keys.F2_KEYSTROKE, (te)-> statusMSGs.setText("B2 Pressed"));
        bbar1.addButton("B3", Keys.F3_KEYSTROKE, (te)-> statusMSGs.setText("B3 Pressed"));
        
        bbar1.setUpdateCanvas(renderComponents);
        

        renderComponents.run();

        screen.add(ControlCode.HIDE_CURSOR);
        renderComponents.run();

        EventBus eventBus = new EventBus();
        KeyInputEventsProducer keyInputEventsProducer = null;
        try {
            keyInputEventsProducer = new KeyInputEventsProducer(eventBus);
        } catch (Exception e) {
            eventBus.stopConsuming();
            return;
        }
        
        eventBus.appendEventHandler(bbar1);

        EventHandler terminate = new EventHandler() {

            @Override
            public void handle(TerminalEvent<?> te) {
                eventBus.stopConsuming();
                screen.add(ControlCode.SHOW_CURSOR);
                screen.refresh();

                try {
                    Stty.setCooked();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // next refresh() is necessary to get cursor to be below
                // rendered canvas
                master_canvas.refresh();
                return;
            }
        };

        eventBus.disableIdleEvents();

        eventBus.appendEventHandler(master_canvas.asShowCursorEventHandler());
        eventBus.appendEventHandler(master_canvas.asHideCursorEventHandler());

        eventBus.appendEventHandler(new KeyInputEventHandler(Keys.CTRL_C_KEYSTROKE, terminate));

        eventBus.appendEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, terminate));

        TestsCommon.addNoGraphicsToggleEvent(eventBus, screen, renderComponents);

    }

}
