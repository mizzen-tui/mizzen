package mizzen.components.plot.grid;

import mizzen.components.Component;
import mizzen.components.plot.Plotter;
import mizzen.components.plot.PointPlotter;
import mizzen.components.plot.blocks.BlocksGrid;
import mizzen.components.plot.braille.BrailleGrid;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;

/**
 * Abstraction of a grid.   
 * 
 * <p/>
 * 
 * We do this extra layer of abstraction so that when we plot shapes using PlotShapeController objects,
 * these controllers do not have to know exactly the type of grid implementation (Braille/Blocks/Simple)that is being invoked.
 * 
 * <p/>
 * 
 * It just needs to interact with this Grid context object.
 * 
 * @author dbalme
 *
 */
public interface GridContext {
    PointPlotter asPointPlotter();
    Rectangle getGridRectangle();
    Component<?> asComponent();
    boolean isDirty();
    Position mapCharPositionToGridPosition(Position mousePosition);

    enum GRID_TYPE {
        BRAILLE_GRID_CTX,
        BLOCK_GRID_CTX,
        SIMPLE_GRID_CTX;
        
        static GRID_TYPE fromString(String gameGridType) {
            if ("braille".equalsIgnoreCase(gameGridType)) {
                return BRAILLE_GRID_CTX;
            }
            if ("block".equalsIgnoreCase(gameGridType)) {
                return BLOCK_GRID_CTX;
            }
            if ("simple".equalsIgnoreCase(gameGridType)) {
                return SIMPLE_GRID_CTX;
            }
            
            // default is braille
            return BRAILLE_GRID_CTX;
            
        }
        
    }

    public static GridContext getGridContextFromType(String gridContextType) {
        return getGridContextFromType(GRID_TYPE.fromString(gridContextType));
    }
    public static GridContext getGridContextFromType(GRID_TYPE gridContextType) {
        return getGridContextFromType(gridContextType, Colors.Gre);
    }

    public static GridContext getGridContextFromType(GRID_TYPE gridContextType, Color color) {
        if (GRID_TYPE.BRAILLE_GRID_CTX.equals(gridContextType)) {
            return new BrailleGridContext(new BrailleGrid(color, Dimension.ZERO));
        }
        if (GRID_TYPE.BLOCK_GRID_CTX.equals(gridContextType)) {
            return new BlocksGridContext(new BlocksGrid(color, Dimension.ZERO));
        }
        if (GRID_TYPE.SIMPLE_GRID_CTX.equals(gridContextType)) {
            return new PlotterGridContext(new Plotter(color, Dimension.ZERO));
        }

        // default is braille
        return getGridContextFromType(GRID_TYPE.BRAILLE_GRID_CTX);
    }

}
