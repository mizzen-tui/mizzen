package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ComponentInspector;
import dtb.terminalIO.components.WindowFrame;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.input.MultiLineTextInput;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.components.scroll.SearchFunctionForScrollableComponent;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.OnIdleEventEventHandler.RepeatingInterval;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.Logging;

public class MultiLineTextInputTest {

    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);
        
        new TUIApp(args)
        .setCanvasSize(50, 100)
        .setEmptyChar(' ')
        .setMainApplicationPanel(new Composite().setBounds(30, 120).setEmptyChar('X').setBorder(false))
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {

            MultiLineTextInput mTextBox = new MultiLineTextInput(); 
            mTextBox.setText("Silly Sally sold\nSeaShells at the seashore\nWhat a silly thing to do!");

            mTextBox.setColor(  new Colors.FGBGColors( Colors.BBlu, Colors.On_Bla  )  );

            ScrollableComponent sc = new ScrollableComponent(Colors.Yel, 
                                                             (Dimension)tApp.getMainApplicationPanel(), 
                                                             mTextBox,
                                                             ()-> new DefaultFocusManager().setNavKeysFirst(false)) {
                public Composite bringToFront(Component<?> component) {
                    return super.bringToFront(component);
                }
            };

            sc.setFillOnClear(true);
            sc.setEmptyChar('.');

            sc.resize(  new Dimension.Simple(sc.getHeight()-2, sc.getWidth()));
            
            SearchFunctionForScrollableComponent.addFinderToScrollable(sc, 20);

            tApp.getMainApplicationPanel().add(2,0, sc);

            FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),
                    tApp.getEventBus(),
                    ()-> new DefaultFocusManager().setNavKeysFirst(false).setCycling(false).setName("MainApplicationPanel-FocusManager"));

            tApp.getEventBus().appendEventHandler( mainFocusManager );

            ComponentInspector inspector = new ComponentInspector(Colors.BIGre, 
                    new Dimension.Simple(25,60), 
                    (Component<?>)tApp.getMainApplicationPanel(), 
                    mainFocusManager);
            
            WindowFrame wfInspector = new WindowFrame(inspector.getColor(),"Components",inspector);
            
//            DefaultFocusManager.applyIfDefaultFocusManager(mainFocusManager, (dfalt_fm)->{
//                dfalt_fm.addFocusable(wfInspector);
//            });

            tApp.getEventBus().appendEventHandler( inspector.toggleViewEventHandler() );
            tApp.getEventBus().appendEventHandler( inspector.refreshEventHandler() );
            // every 1/4 second we update ComponentInspector with current component hierarchy and focus data
            tApp.getEventBus().appendIdleEventHandler(new RepeatingInterval(2000, (te)->{
                                                         inspector.refreshEventHandler().handle(te);
                                                      }));
            
            tApp.getMainApplicationPanel().add(22,44, wfInspector);
            
            //FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, (Component<?>)tApp.getMainApplicationPanel());

        }).start();

    }

}
