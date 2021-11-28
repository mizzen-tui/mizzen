package dtb.terminalIO.examples.plot.gameoflife;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.geom.Dimension;

public class GameOfLife {

    public static void main(String args[]) throws IOException {

        new TUIApp(args).setDefaultMainApplicationPanel()
                        .confirmOnExit()
                        .setOnReadyStart((tApp) -> {

                GameOfLifeComponent gameComponent = new GameOfLifeComponent(Colors.DEFAULT_COLOR, new Dimension.Simple(32,32));

                gameComponent.drawShape(GameOfLifeSampleShapes.spaceship);

                tApp.getMainApplicationPanel().add( gameComponent);
                tApp.appendOnResizeAction((app, size)->{
                    gameComponent.resize( new Dimension.Simple(size.getHeight(), size.getWidth()));
                });

                FocusManager fm = FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(), tApp.getEventBus());
                tApp.getEventBus().appendEventHandler(fm);

                FocusManager.UTIL.verifyPathsToFocusables(fm, (Component<?>)tApp.getMainApplicationPanel());
                
                
                gameComponent.start(tApp.getEventBus());

        }).start();
        
    }
    

}
