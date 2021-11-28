package dtb.terminalIO.examples.plot.gameoflife;

import dtb.terminalIO.components.AbstractComponentWithContainer;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.input.DisplayableValue;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.input.ListBox.ComponentItemFactory;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.StringUtils;

public class GameOfLifeComponent extends AbstractComponentWithContainer<Composite> {

    public static int SPEED_SLOW        = 1000;
    public static int SPEED_MEDIUM      =  500;
    public static int SPEED_FAST        =  250;
    public static int SPEED_REALLY_FAST =  100;

    private GridDataContainer dataGridContainer;
    private GameOfLifeEngine gameOfLifeEngine;
    private OnIdleEventEventHandler gameTicker;
    private GameGridRenderer renderer;
    private int gameTickInterval = SPEED_FAST;

    public GameOfLifeComponent(Color color, Dimension d) {

        super(color);

        gameOfLifeEngine = new GameOfLifeEngine();

        renderer = new GameGridRenderer.Simple(color);

        dataGridContainer = new GridDataContainer();

        DropdownListBox<Integer> speedOptions = new DropdownListBox<Integer>(GameSpeeds.speedOptions());
        speedOptions.onChange((te)->{
            gameTickInterval = speedOptions.getValue();
            doStart(te.getEventBus());
        });

        DropdownListBox<int[][]> shapeOptions = new DropdownListBox<int[][]>(GameOfLifeSampleShapes.shapes(), new DraggableCopiedTextComponentItemFactory<int[][]>());
        shapeOptions.onChange((te)->{
            drawShape(shapeOptions.getValue());
        });

        HorizontalPanel optionsPanel = new HorizontalPanel().setHorizontalSpacer(1);

        optionsPanel.add(new TextComponent("Speed: "));
        optionsPanel.add(speedOptions);

        optionsPanel.add(new TextComponent("Shape: "));
        optionsPanel.add(shapeOptions);
        
        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.add(optionsPanel);
        mainPanel.add(renderer.getComponent());

        resizeRenderer(d);

        this.setWrappedComponent(mainPanel);

        this.setEmptyChar(' ');
        this.setFillOnClear(true);

    }

    @Override
    public void resize(Dimension d) {
        resizeRenderer(d);
        super.resize(d);
    }

    private Dimension calcGridSize(Dimension gameSize) {
        if (gameSize.getHeight()<4 || gameSize.getWidth()<3) {
            return Dimension.ZERO;
        }
        Dimension rendererComponentSize = new Dimension.Simple(gameSize.getHeight()-3, gameSize.getWidth()-2);
        Dimension gameGridSize = renderer.componentSizeToGridSize( rendererComponentSize  );
        return gameGridSize;
    }

    private void resizeRenderer(Dimension gameSize) {
        dataGridContainer.setSize(calcGridSize(gameSize));
    }
    
    public void start(EventBus eventBus) {
        eventBus.appendIdleEventHandler(new OnIdleEventEventHandler.OneTime(te->{  doStart(te.getEventBus()); }));
    }

    private void renderNextGeneration() {
        dataGridContainer.grid = gameOfLifeEngine.makeNextGeneration(dataGridContainer.grid);
        dataGridContainer.generation += 1;
        setTitle("Gen: " + dataGridContainer.generation /*+ " " + new Dimension.Simple(this.getPreferredRectangleForLayout()) + " .. " + calcGridSize(this) + " .. " + this.getParentContainer()*/);
        renderer.render(dataGridContainer);
    }
    
    private void doStart(EventBus eventBus) {

        // clean out previous idle event
        stop(eventBus);

        if (gameTicker == null) {
            gameTicker = new OnIdleEventEventHandler.RepeatingInterval(gameTickInterval, (te)-> {
                renderNextGeneration();
                te.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(GameOfLifeComponent.this));
            });
        }

       eventBus.appendIdleEventHandler(gameTicker);
    }

    public void stop(EventBus eventBus) {
       if (gameTicker != null) {
           eventBus.removeEventHandler(gameTicker);
           gameTicker = null;
       }
    }

    public void drawShape(int[][] shapeFromArray) {
        dataGridContainer.copyShapeIntoGridCentered(shapeFromArray);
        
    }
    
    public static class DraggableCopiedTextComponentItemFactory<T> implements ComponentItemFactory<T> {

        @Override
        public Component<?> makeComponentFromdisplayableItem(Color color, int maxWidthStr, DisplayableValue<T> displayableItem) {
 
            Dimension d = new Dimension.Simple(Dimension.UTIL.stringToDimension(displayableItem.getDisplayValue()).getHeight(), maxWidthStr);
  
            TextComponent tc = new TextComponent(color, d, StringUtils.padRight(displayableItem.getDisplayValue(),maxWidthStr)) ;
            return tc;
        }
    }


}
