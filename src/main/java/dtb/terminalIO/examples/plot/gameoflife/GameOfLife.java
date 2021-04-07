package dtb.terminalIO.examples.plot.gameoflife;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.core.ScreenCanvasBackend;
import dtb.terminalIO.core.ScreenCanvasBackend.UpdateStrategy;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;

public class GameOfLife {

    static class GridContainer {
        int[][] grid;
        int generation = 0;
    }
    
    
    public static void main(String args[]) throws IOException {

        new TUIApp(args).setDefaultMainApplicationPanel()
                        .confirmOnExit()
                        .setOnReadyStart((tApp) -> {

                Dimension gameSize = new Dimension.Simple(20,20);

                GameOfLifeEngine gameOfLifeEngine = new GameOfLifeEngine();
                GOLasString renderer = new GOLasString(); 

                int[][] grid = gameOfLifeEngine.make2d(gameSize);
                Position midPoint = new Rectangle.Simple(gameOfLifeEngine.toDimension(grid)).centerPosition();

                PointPlotter plotter = gameOfLifeEngine.asPointPlotter(grid);
                plotGlider(plotter, midPoint);
                
               
                GridContainer gridContainer =  new GridContainer();

                TextComponent gameOfLifeComponent = new TextComponent( renderer.array2DAsString(grid)  );

                BorderedComponent bc = new BorderedComponent(gameOfLifeComponent);

                gridContainer.grid = grid;
                
                VerticalPanel vp = new VerticalPanel(); 
                TextComponent title = new TextComponent("Start ..");
                vp.add(title);
                vp.add(bc);
                
                
                tApp.getMainApplicationPanel().add(5, 5, vp);
                
                ScreenCanvasBackend backEnd = (ScreenCanvasBackend) tApp.getCanvas().getCanvasBackend();
                backEnd.setUpdateStrategy(UpdateStrategy.UPDATE_FULL );

                
                tApp.getEventBus().appendEventHandler( new OnIdleEventEventHandler.RepeatingInterval(500, (te)-> {

                    gridContainer.grid = gameOfLifeEngine.makeNextGeneration(gridContainer.grid);

                    gridContainer.generation += 1;
                    title.setText("Gen: " + gridContainer.generation);
                    gameOfLifeComponent.setText(renderer.array2DAsString(gridContainer.grid));

                    tApp.doRefreshCanvasIfModified();

                } ));
                
                
                

        }).start();
        
    }
    
    static void plotGlider(PointPlotter plotter, Position startPoint)  {
        plotter.plot(startPoint);
        plotter.plot(startPoint.offsetAdd(1, 1));
        plotter.plot(startPoint.offsetAdd(2, 1));
        plotter.plot(startPoint.offsetAdd(3, 1));
        plotter.plot(startPoint.offsetAdd(3, 0));
        plotter.plot(startPoint.offsetAdd(3, -1));
    }

}
