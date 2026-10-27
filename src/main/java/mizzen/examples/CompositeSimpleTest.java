package mizzen.examples;

import java.io.IOException;

import mizzen.app.TUIApp;
import mizzen.components.BorderedComponent;
import mizzen.components.ButtonBar;
import mizzen.components.TextComponent;
import mizzen.components.composite.Composite;
import mizzen.components.input.Button;
import mizzen.controlcodes.color.Colors;
import mizzen.events.keyboard.Keys;
import mizzen.util.Logging;

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

                    tApp.getMainApplicationPanel().add(10,25, new TextComponent("Test3"));
                    tApp.getMainApplicationPanel().add(14,36, new BorderedComponent(new Button("Test4")));

                    tApp.getMainApplicationPanel().add(12,15, new TextComponent(Colors.IRed,"Test2\nand more1\nand more2\nand more3 and more4..\nand then some more5"));

                    tApp.getMainApplicationPanel().add(18,36, new BorderedComponent(new Button("Test5")));

                    Button toggleDrawType = new Button(Composite.DRAW_VIA_PARTS ? "Drawing via Parts" : "Drawing traditional");
                    toggleDrawType.setHotKey(Keys.CTRL_V_KEYSTROKE);
                    toggleDrawType.onChange((te)-> {
                       Composite.DRAW_VIA_PARTS = ! Composite.DRAW_VIA_PARTS;
                       toggleDrawType.setButtonText(Composite.DRAW_VIA_PARTS ? "Drawing via Parts" : "Drawing traditional");
                    });

                    tApp.getMainApplicationPanel().add(1, 20, new BorderedComponent( toggleDrawType ));

                    ButtonBar bb = new ButtonBar().setNoBorderChars();
                    TextComponent textStatus = new TextComponent("-----------------");
                    tApp.getMainApplicationPanel().add(1, 3, new BorderedComponent( bb ));
                    tApp.getMainApplicationPanel().add(4, 3, textStatus);

                    bb.addButton("F1", Keys.F1_KEYSTROKE, (te)-> textStatus.setText("F1 pressed .. ") );
                    bb.addButton("F2", Keys.F2_KEYSTROKE, (te)-> textStatus.setText("F2 pressed .. ") );


                }).start();

    }

}
