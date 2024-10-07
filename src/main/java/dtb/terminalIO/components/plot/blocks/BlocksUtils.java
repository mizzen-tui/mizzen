package dtb.terminalIO.components.plot.blocks;

import dtb.terminalIO.components.plot.Plotter;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors.BgColor;
import dtb.terminalIO.controlcodes.color.Colors.FGBGColors;
import dtb.terminalIO.controlcodes.color.Colors.FgColor;
import dtb.terminalIO.controlcodes.color.Colors.RGBColorBackground;
import dtb.terminalIO.controlcodes.color.Colors.RGBColorFGBG;
import dtb.terminalIO.controlcodes.color.Colors.RGBColorForeground;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;

public class BlocksUtils {
    
//    static char BLOCK_PLOT_CHAR_TOP    = '`';
//    static char BLOCK_PLOT_CHAR_BOTTOM = 'e';
    static char BLOCK_PLOT_CHAR_TOP    = '▀';
    static char BLOCK_PLOT_CHAR_BOTTOM = '▄';
    static char BLOCK_CHAR_ROW_0 = BLOCK_PLOT_CHAR_TOP; 
    static char BLOCK_CHAR_ROW_1 = BLOCK_PLOT_CHAR_BOTTOM; 
    
    public static boolean isBlockChar(char theChar) {
        return theChar == BLOCK_PLOT_CHAR_TOP || theChar == BLOCK_PLOT_CHAR_BOTTOM;
    }
    
    public static Position gridPosToComponentPosition(Position gridPosition) {
        return new Position.Simple(gridPosition.getRow() / 2, gridPosition.getCol());
    }

    public static Dimension componentDimensionToGridDimension(Dimension componentDimension) {
        return new Dimension.Simple(componentDimension.getHeight() * 2, componentDimension.getWidth());
    }

    /**
     * A grid rectangle in relation to a component rectangle is 1 * (times) wider
     * and 2 * taller.
     */
    public static Rectangle componentRectangleToGridRectangle(final Rectangle componentRectangle) {
        return new Rectangle.Delegate(componentRectangle) {
            @Override
            public int getCol() {
                return super.getCol();
            }

            @Override
            public int getRow() {
                return super.getRow() * 2;
            }

            @Override
            public int getWidth() {
                return super.getWidth();
            }

            @Override
            public int getHeight() {
                return super.getHeight() * 2;
            }
        };
    }

//    public static BlocksPlot setBlockPixel(char currentCHAR, Color currentColor, Position pos) {
//        return setBlocksPixel(currentCHAR, currentColor, pos.getRow(), pos.getCol());
//    }
/**
 * case 1   row 0
 *      [X]
 *      [ ]  -> BLOCK_PLOT_CHAR_TOP
 * 
 * case 2   row 1
 *      [ ]
 *      [X]  -> BLOCK_PLOT_CHAR_BOTTOM
 * 
 * case 2   row 0, 
 *      [ ]
 *      [X]
 * 
 * case 2   row 1
 *      [ ]
 *      [X]
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 */
    
    static Color makeColor(Color foreground, Color background) {
        if (foreground == null) {
            return background;
        }
        if (background == null) {
            return foreground;
        }
        if (foreground instanceof FgColor && background instanceof BgColor) {
            Color newColor = new FGBGColors((FgColor)foreground, (BgColor)background);
            return newColor;
        }
        if (foreground instanceof RGBColorForeground && background instanceof RGBColorBackground) {
            Color newColor = RGBColorFGBG.instance((RGBColorForeground)foreground, (RGBColorBackground)background);
            return newColor;
        }
        if (foreground instanceof RGBColorFGBG || background instanceof RGBColorFGBG) {
            return foreground != null ? foreground : background;
        }
        // TODO - handle RGB
        return foreground;
    }
    
    public static BlocksPlot setBlockPixel(BlocksPlot plot, Position gridPosition, Color plotColor) {
        int theROW = gridPosition.getRow() % 2;
        if (theROW == 0) {
            plot.row0 = true;
            plot.setForeground(plotColor);
        } else {
            plot.row1 = true;
            plot.background = plotColor.invert();
        }
        plot.plotColor = makeColor(plot.row0 || plot.foreground != null ? plot.foreground : null, 
                                   plot.row1 || plot.background != null ? plot.background : null);
        
        //plot.plotColor = Colors.RCol;
        plot.plotChar = BLOCK_PLOT_CHAR_TOP;
        //plot.plotChar = 'x';
        //plot.plotChar = '●';
        return plot;
    }
    
    
    static Color addBackgroundColor(Color color) {
        if (color instanceof FGBGColors) {
            return color;
        }
        if (color instanceof FgColor) {
            return new FGBGColors((FgColor) color, (BgColor) color.invert());
        }
        if (color instanceof BgColor) {
            return color;
        }
        
        return color;
    }
    
    static Color addForegroundColor(Color color) {
        if (color instanceof FGBGColors) {
            return color;
        }
        if (color instanceof FgColor) {
            return color;
        }
        if (color instanceof BgColor) {
            return new FGBGColors((FgColor) color.invert(), (BgColor) color);
        }

        return color;
    }
    

    public static char unsetBlockPixel(char theCHAR, Position gridPosition) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    public static class BlocksPlot extends Plotter.Plot {
        public boolean row0 = false;
        public boolean row1 = false;
        public Color background;
        private Color foreground;
        public BlocksPlot(int row, int col, Color plotColor) {
            super(row, col, ' ', plotColor);
            if ( row % 2 == 0 ) {
                setForeground(plotColor.getForeGroundColor());
                this.row0 = true;
                if (plotColor instanceof FgColor) {
                    background = plotColor.getBackGroundColor();
                }
            } else {
                this.row1 = true;
                background = plotColor.getBackGroundColor();
                setForeground(plotColor.getForeGroundColor());
            }
        }
        
        public Color getForeground() {
            return foreground;
        }
        public void setForeground(Color foreground) {
            if (foreground instanceof RGBColorBackground) {
                throw new IllegalStateException("forgeground == background!");
            }
            this.foreground = foreground;
        }
        
        
        
    }


}
