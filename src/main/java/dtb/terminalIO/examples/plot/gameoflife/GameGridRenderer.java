package dtb.terminalIO.examples.plot.gameoflife;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Dimension;

public interface GameGridRenderer {
    
    
    default Dimension componentSizeToGridSize(Dimension d) {
        return new Dimension.Simple(d);
    }

    public void render(GridDataContainer gridContainer);
    
    public Component<?> getComponent();
    
    
    /**
     * Draw into a simple TextComponent
     */
    public static class Simple implements GameGridRenderer {

        TextComponent component;
        GOLasString textRenderer;
        
        public Simple(Color color) {
            component = new TextComponent(color,"");
            textRenderer = new GOLasString();
        }

        public Dimension componentSizeToGridSize(Dimension d) {
            Dimension d2 = textRenderer.calcGameBoardDimension(d);
            float heightRatio = (float)d.getHeight() / (float)d2.getHeight(); 
            float widthRatio =  (float)d.getWidth() / (float)d2.getWidth();
            return new Dimension.Simple(Math.round(d.getHeight()*heightRatio), Math.round(d.getWidth()*widthRatio));
        }

        @Override
        public void render(GridDataContainer gridContainer) {
            this.component.setTextAndResize(textRenderer.array2DAsString(gridContainer.getGridData()));
        }

        @Override
        public Component<?> getComponent() {
            return component;
        }
        
    }

}
