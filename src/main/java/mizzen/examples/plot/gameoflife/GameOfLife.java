package mizzen.examples.plot.gameoflife;

import java.io.IOException;

import mizzen.app.TUIApp;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;

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

                    gameComponent.start(tApp.getEventBus());

        }).start();
        
    }
    

}
