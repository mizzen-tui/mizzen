package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.input.MultiLineTextInput;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.core.ScreenCanvasBackend;
import dtb.terminalIO.core.ScreenCanvasBackend.UpdateStrategy;
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
        .setMainApplicationPanel(new Composite().setBounds(30, 120).setEmptyChar('X').setBorder(false))
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {

            MultiLineTextInput mTextBox = new MultiLineTextInput(); mTextBox.setText("Hello World!\n");

            mTextBox.setColor(  new FGBGColors( Colors.BBlu, Colors.On_Bla  )  );

            ScrollableComponent sc = new ScrollableComponent(Colors.Whi, 
                                                             (Dimension)tApp.getMainApplicationPanel(), 
                                                             mTextBox,
                                                             ()-> new DefaultFocusManager(Keys.SHIFT_TAB_KEYSTROKE, Keys.TAB_KEYSTROKE).setNavKeysFirst(false));
            sc.setFillOnClear(false);

            sc.resize(  new Dimension.Simple(sc.getHeight()-2, sc.getWidth()));

            tApp.getMainApplicationPanel().add(2,0, sc);

//            tApp.getMainApplicationPanel().add(2,0, mTextBox);


            //mTextBox.setText("Hello World!");

            ScreenCanvasBackend backEnd = (ScreenCanvasBackend) tApp.getCanvas().getCanvasBackend();
            //backEnd.setUpdateStrategy(UpdateStrategy.UPDATE_CHANGED  );
            backEnd.setUpdateStrategy(UpdateStrategy.UPDATE_FULL  );

            FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),
                    tApp.getEventBus(),
                    ()-> new DefaultFocusManager().setNavKeysFirst(false).setCycling(false));

            tApp.getEventBus().appendEventHandler( mainFocusManager );

            AtomicInteger counter = new AtomicInteger(0);

//            tApp.getEventBus().appendEventHandler(new OnIdleEventEventHandler.RepeatingInterval(1000, (te) -> {
//                int count = counter.incrementAndGet();
//                String stats =  "\n------------------------\n"+
//                                "IDLE task invoke counter = " + count + 
//                                "   \n" + ((EventBusImpl)tApp.getEventBus()).statsAsStr() + 
//                                "\n" + 
//                                "Text segments on screen: " + tApp.getCanvas().segmentsSize() ;
//
//                Logging.info(stats);
//            
//             }));

            FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, (Component<?>)tApp.getMainApplicationPanel());

        }).start();

    }

}
