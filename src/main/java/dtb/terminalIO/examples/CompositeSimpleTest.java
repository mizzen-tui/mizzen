package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.util.Logging;

public class CompositeSimpleTest {


    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);

        new TUIApp(args)
                .setCanvasSize(24, 55)
                .setEmptyChar('░')
                .setDefaultMainApplicationPanel()
                .confirmOnExit()
                .setOnReadyStart((tApp) -> {

                    tApp.getMainApplicationPanel().add(10,10, new TextComponent("Test1"));

                    tApp.getMainApplicationPanel().add(12,15, new TextComponent(Colors.IRed,"Test2\nand more1\nand more2\nand more3 and more4..\nand then some more5"));
                    tApp.getMainApplicationPanel().add(13,15, new TextComponent("Test3"));
                    tApp.getMainApplicationPanel().add(14,22, new BorderedComponent(new Button("Test4")));

                    tApp.getMainApplicationPanel().add(18,22, new BorderedComponent(new Button("Test5")));

                    Button toggleDrawType = new Button(Composite.DRAW_VIA_PARTS ? "Drawing via Parts" : "Drawing traditional");
                    toggleDrawType.setHotKey(Keys.CTRL_V_KEYSTROKE);
                    toggleDrawType.onChange((te)-> {
                       Composite.DRAW_VIA_PARTS = ! Composite.DRAW_VIA_PARTS;
                       toggleDrawType.setButtonText(Composite.DRAW_VIA_PARTS ? "Drawing via Parts" : "Drawing traditional");
                       te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(toggleDrawType));
                    });

                    tApp.getMainApplicationPanel().add(5, 3, new BorderedComponent( toggleDrawType ));

                    ButtonBar bb = new ButtonBar();
                    TextComponent textStatus = new TextComponent("-----------------");
                    tApp.getMainApplicationPanel().add(1, 3, new BorderedComponent( bb ));
                    tApp.getMainApplicationPanel().add(4, 3, textStatus);

                    bb.addButton("F1", Keys.F1_KEYSTROKE, (te)-> textStatus.setText("F1 pressed .. ") );
                    bb.addButton("F2", Keys.F2_KEYSTROKE, (te)-> textStatus.setText("F2 pressed .. ") );


                }).start();

    }

}
