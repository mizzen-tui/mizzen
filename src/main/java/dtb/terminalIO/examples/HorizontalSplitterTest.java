package dtb.terminalIO.examples;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.WindowFrame;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.frameset.FrameSet;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.CheckBoxInput;
import dtb.terminalIO.components.input.MultiLineTextInput;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.Logging;

public class HorizontalSplitterTest {



    public static void main(String[] args) throws Exception {

        Logging.setEnabled(true);

        new TUIApp(args)
        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {

            ButtonBar controls = new ButtonBar(Colors.BCya, "controls").setNoBorderChars();

            FrameSet fs = new FrameSet(Colors.BIWhi, new Dimension.Simple(30,70));
            fs.setBorder(true);
            fs.setEmptyColor(Colors.On_IBla);
            fs.setFillOnClear(true);

            Composite first = new VerticalPanel(); //new TextComponent("ABC-123");
            fs.add(first);

            first.add(new TextComponent("ABC-123"));
            first.add(new Button("TEST-BUTTON"));

            Component<?> second = new TextComponent("XYZ");
            fs.addRight(second, first, true);

            Composite third = new VerticalPanel();
            TextComponent third1 = new TextComponent("3rd one ...............");
            third.add(1,1, third1);
            CheckBoxInput cb_1 = new CheckBoxInput();
            third.add(2,1, cb_1);
            third.add(3,1, new MultiLineTextInput());

            third.setEmptyColor(Colors.On_Yel);
            third.setFillOnClear(true);

            fs.addRight(third, second, true);

            //fs.onResizeExpandRight(first, 33);
            //fs.onResizeExpandRight(second, 33);
            fs.onResizeExpandDownAndRight(third);

            EventHandler splitter1_moveLeft = (te) -> {
                fs.moveLeft(1, first, second);
            };         

            EventHandler splitter1_moveRight = (te) -> {
                fs.moveRight(1, first, second);
            };

            EventHandler splitter2_moveLeft = (te) -> {
                fs.moveLeft(1, second, third);
            };

            EventHandler splitter2_moveRight = (te) -> {
                fs.moveRight(1, second, third);
            };         

            WindowFrame wf = new WindowFrame(Colors.IBlu, "A test of frames", fs);

            controls.addComponent(new TextComponent("Horizontal Splitter Test"));
            controls.addComponent(new TextComponent("  Splitter-1 "));
            controls.addButton("<-", Keys.CTRL_A_KEYSTROKE, splitter1_moveLeft);     
            controls.addButton("->", Keys.CTRL_S_KEYSTROKE, splitter1_moveRight);     
            controls.addComponent(new TextComponent(" ---- "));
            controls.addComponent(new TextComponent("  Splitter-2 "));
            controls.addButton("<-", Keys.CTRL_K_KEYSTROKE, splitter2_moveLeft);     
            controls.addButton("->", Keys.CTRL_L_KEYSTROKE, splitter2_moveRight);     

            tApp.getMainApplicationPanel().add(0,4, controls);

             tApp.getMainApplicationPanel().add(2,4, wf);
            //tApp.getMainApplicationPanel().add(2,25, fs);

        }).start();
        
    }
    
    
    
}
