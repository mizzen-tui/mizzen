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
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.StringUtils;

public class GameOfLifeComponent extends AbstractComponentWithContainer<Composite> {

    public static int SPEED_SLOW        = 1000;
    public static int SPEED_MEDIUM      =  500;
    public static int SPEED_FAST        =  250;
    public static int SPEED_REALLY_FAST =  100;

    private GridData gridData;
    private GameOfLifeEngine gameOfLifeEngine;
    private OnIdleEventEventHandler gameTicker;
    private VerticalPanel mainPanel;
    private GameGridRenderer renderer;
    private int gameTickInterval = SPEED_FAST;
    Dimension desiredSize;

    public GameOfLifeComponent(Color color, Dimension d) {

        super(color);
        
        this.desiredSize = d;

        gameOfLifeEngine = new GameOfLifeEngine();

        renderer = new GameGridTextComponentRenderer(color);

        gridData = new GridData();

        DropdownListBox<Integer> speedOptions = new DropdownListBox<Integer>(GameSpeeds.speedOptions());
        speedOptions.onChange((te)->{
            gameTickInterval = speedOptions.getValue();
            doStart(te.getEventBus());
        });

        DropdownListBox<int[][]> shapeOptions = new DropdownListBox<int[][]>(GameOfLifeSampleShapes.shapes(), new DraggableCopiedTextComponentItemFactory<int[][]>());
        shapeOptions.onChange((te)->{
            drawShape(shapeOptions.getValue());
        });

        DropdownListBox<GameGridRenderer.Builder> rendererOptions = new DropdownListBox<GameGridRenderer.Builder>(GameGridRenderer.UTIL.standardGameRenderers());
        rendererOptions.onChange((te)->{
            GameGridRenderer newRenderer = rendererOptions.getValue().build(color);
            replaceRendererComponent(newRenderer); 
        });

        HorizontalPanel optionsPanel = new HorizontalPanel().setHorizontalSpacer(1);

        optionsPanel.add(new TextComponent("Speed: "));
        optionsPanel.add(speedOptions);

        optionsPanel.add(new TextComponent("Shape: "));
        optionsPanel.add(shapeOptions);

        optionsPanel.add(new TextComponent("Styles: "));
        optionsPanel.add(rendererOptions);

        mainPanel = new VerticalPanel();
        mainPanel.add(optionsPanel);
        mainPanel.add(renderer.getComponent());

        Dimension rendererComponentSize = resizeRendererComponent(d);
        resizeGridDataFromRendererSize(rendererComponentSize);

        this.setWrappedComponent(mainPanel);

        this.setEmptyChar(' ');
        this.setFillOnClear(true);

    }
    
    private void replaceRendererComponent(GameGridRenderer newRenderer) {
         if (this.renderer == null) {
             mainPanel.add(newRenderer.getComponent());
             this.renderer = newRenderer;
             return;
         }
         Dimension gameSize = componentSizeToRendererComponentSize(this.desiredSize); 
         int idx = mainPanel.indexOf(this.renderer.getComponent());
         newRenderer.resizeComponent(gameSize);
         mainPanel.insert(idx, newRenderer.getComponent());
         mainPanel.remove(this.renderer.getComponent());
         this.renderer = newRenderer;
         resizeGridDataFromRendererSize( gameSize );
         renderGeneration();
         this.markAsDirty();
    }

    @Override
    public void resize(Dimension d) {
        this.desiredSize = d;
        Dimension rendererComponentSize = resizeRendererComponent(d);
        resizeGridDataFromRendererSize(rendererComponentSize);
        super.resize(d);
    }

    private Dimension calcDataGridSize(Dimension rendererComponentSize) {
        if (rendererComponentSize.getHeight()<4 || rendererComponentSize.getWidth()<3) {
            return Dimension.ZERO;
        }
        Dimension gameGridSize = renderer.componentSizeToGridSize( rendererComponentSize  );
        return gameGridSize;
    }

    private Dimension componentSizeToRendererComponentSize(Dimension componentSize) {
        Position positionInMainPanel = mainPanel.childAsRectangleOffset(this.renderer.getComponent()); 
        Dimension rendererComponentSize = new Dimension.Simple(componentSize.getHeight() - 2 - positionInMainPanel.getRow() , componentSize.getWidth()-2);
        return rendererComponentSize;
    }

    private Dimension resizeRendererComponent(Dimension componentSize) {
        Dimension rendererComponentSize = componentSizeToRendererComponentSize(componentSize);
        this.renderer.resizeComponent(rendererComponentSize);
        return rendererComponentSize;
    }

    private void resizeGridDataFromRendererSize(Dimension gameSize) {
        gridData.setSize(calcDataGridSize(gameSize));
    }
    
    public void start(EventBus eventBus) {
        eventBus.appendIdleEventHandler(new OnIdleEventEventHandler.OneTime(te->{  doStart(te.getEventBus()); }));
    }

    private void renderNextGeneration() {
        gridData.array2D = gameOfLifeEngine.makeNextGeneration(gridData.array2D);
        gridData.generation += 1;
        renderGeneration();
    }
    
    private void renderGeneration() {
        setTitle(String.format("[ Gen: %4d   %2d X %2d ]", gridData.generation, gridData.getWidth(),  gridData.getHeight()));
        renderer.render(gridData);
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
        gridData.copyShapeIntoGridCentered(shapeFromArray);
        
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
