package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ComponentInspector;
import dtb.terminalIO.components.WindowFrame;
import dtb.terminalIO.components.input.MultiLineTextInput;
import dtb.terminalIO.components.scroll.ScrollableComponent2;
import dtb.terminalIO.components.scroll.SearchFunctionForScrollableComponent;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.OnIdleEventEventHandler.RepeatingInterval;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.Logging;

public class MultiLineTextInputTest {

    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);
        
        new TUIApp(args)
        .setCanvasSize(50, 100)
        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {

            MultiLineTextInput mTextBox = new MultiLineTextInput(); 
            mTextBox.setText("Silly Sally sold\nSeaShells at the seashore\nWhat a Silly Sally thing to do!");

            mTextBox.setColor(  new Colors.FGBGColors( Colors.BBlu, Colors.On_Bla  )  );

            ScrollableComponent2 sc = new ScrollableComponent2(Colors.Yel, 
                                                               (Dimension)tApp.getMainApplicationPanel(), 
                                                                mTextBox);

            sc.setFillOnClear(true);
            sc.setEmptyChar('.');

            SearchFunctionForScrollableComponent.addFinderToScrollable(sc.asSearchableView());

            sc.resize(  new Dimension.Simple(sc.getHeight()-2, sc.getWidth()));
            
            tApp.getMainApplicationPanel().add(2,0, sc);
            
            ComponentInspector inspector = new ComponentInspector(Colors.BIGre, 
                                                                  new Dimension.Simple(25,60), 
                                                                  (Component<?>)tApp.getMainApplicationPanel(), 
                                                                  tApp.getApplicationFocusManager());

            WindowFrame wfInspector = new WindowFrame(inspector.getColor(),"Components",inspector);

            tApp.getEventBus().appendEventHandler( inspector.toggleViewEventHandler() );
            tApp.getEventBus().appendEventHandler( inspector.refreshEventHandler() );
            // every 1/4 second we update ComponentInspector with current component hierarchy and focus data
            tApp.getEventBus().appendIdleEventHandler(new RepeatingInterval(250, (te)->{
                                                         inspector.refreshEventHandler().handle(te);
                                                      }));

            tApp.getMainApplicationPanel().add(22,44, wfInspector);

        }).start();

    }

}
