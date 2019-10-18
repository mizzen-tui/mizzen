package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.input.MultiLineTextInput;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.Logging;

public class MultiLineTextInputTest {

    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);
        
        new TUIApp(args)
        .setCanvasSize(24, 100)
        .setEmptyChar(' ')
        .setMainApplicationPanel(new Composite().setBounds(30, 120).setEmptyChar(' ').setBorder(false))
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {

            MultiLineTextInput mTextBox = new MultiLineTextInput(new Dimension.Simple(60,110));

            mTextBox.setColor(  new FGBGColors( Colors.BBlu, Colors.On_IYel  )  );

            ScrollableComponent sc = new ScrollableComponent(Colors.Whi, 
                                                             (Dimension)tApp.getMainApplicationPanel(), 
                                                             mTextBox,
                                                             ()-> new DefaultFocusManager(Keys.SHIFT_TAB_KEYSTROKE, Keys.TAB_KEYSTROKE)
                                                                       .setNavKeysFirst(false));
            sc.setFillOnClear(false);
            
            sc.resize(  new Dimension.Simple(sc.getHeight()-2, sc.getWidth()));
            
            tApp.getMainApplicationPanel().add(2,0, sc);

            tApp.getEventBus().appendEventHandler( sc );

            mTextBox.setText("Hello World!\n");

        }).start();
        
    }
    
    
}
