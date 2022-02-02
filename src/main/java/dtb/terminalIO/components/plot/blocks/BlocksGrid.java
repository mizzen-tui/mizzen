package dtb.terminalIO.components.plot.blocks;

import java.util.Collection;
import java.util.Collections;

import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.plot.Plotter;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.blocks.BlocksUtils.BlocksPlot;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.CleanString;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;

public class BlocksGrid extends BoundedComponent<BlocksGrid>  implements ParentContainer {

    Plotter plotter;
    Rectangle gridRectangle;

    public BlocksGrid(Color color, Dimension d) {
        super(color, d);
        this.plotter = new Plotter(color, new Dimension.Simple(d.getHeight(), d.getWidth()));
        this.plotter.setName("grid-plotter");
        this.plotter.setParentContainer(this);
        this.plotter.setFillOnClear( this.getFillOnClearPolicy().isFillOnClear() );
        gridRectangle = BlocksUtils.componentRectangleToGridRectangle(plotter);
    }
    
    public BlocksGrid(Color color, int height, int width) {
        this(color, new Dimension.Simple(height, width));
    }
    
    
    public Plotter getPlotter() {
        return plotter;
    }


    public void resize(Dimension d) {
        if (d != null && d.isSameDimension(this)) {
            return;
        }
        super.resize(d);
        this.plotter.resize(d);
        gridRectangle = BlocksUtils.componentRectangleToGridRectangle(plotter);
    }


    public BlocksGrid setEmptyChar(char emptyChar) {
        super.setEmptyChar(emptyChar);
        this.plotter.setEmptyChar(emptyChar);
        return this;
    }

    public BlocksGrid setFillOnClear(boolean fillOnClear) {
        super.setFillOnClear(fillOnClear);
        if (this.plotter != null) {
           this.plotter.setFillOnClear(fillOnClear);
        }
        return  this;
    }

    public Rectangle getGridRectangle() {
        return this.gridRectangle;
    }
    
    public void markAsDirty() {
        super.markAsDirty();
        if (this.plotter != null) {
           this.plotter.markAsDirty();
        }
    }


    Position gridPosToComponentPosition(Position gridPosition) {
        return new Position.Simple(gridPosition.getRow() / 4, gridPosition.getCol() / 2);
    }

    public void plotPosition(Position gridPosition, Color plotColor, CleanString text) {
        if (!this.gridRectangle.containsPosition(gridPosition)) {
            return;
        }
        Position plotterCharacterPosition = BlocksUtils.gridPosToComponentPosition(gridPosition);
        this.plotter.addText(plotterCharacterPosition.getRow(), plotterCharacterPosition.getCol(), text, plotColor);
    }

    public void plotPosition(Position gridPosition, CleanString text) {
        plotPosition(gridPosition, this.plotter.getColor(), text);
    }

//    public void plotPositionX(Position gridPosition, Color plotColor, char theCHAR) {
//        if (!this.gridRectangle.containsPosition(gridPosition)) {
//            return;
//        }
//        Position plotterCharacterPosition = BlocksUtils.gridPosToComponentPosition(gridPosition);
//
//        Plot plotAtPos = plotter.getPlotAt(plotterCharacterPosition.getRow(), plotterCharacterPosition.getCol());
//        if (plotAtPos != null) {
//            plotAtPos.setPlotChar(theCHAR);
//            plotAtPos.setPlotColor(plotColor);
//        } else {
//            this.plotter.addPlot(plotterCharacterPosition.getRow(), 
//                                 plotterCharacterPosition.getCol(), 
//                                 theCHAR,
//                                 plotColor);
//        }
//    }
    /**
     * 
     *      
     * 
     * 
     * 
     * 
     */
    public void plotPosition(Position gridPosition, Color plotColor) {
        if (!this.gridRectangle.containsPosition(gridPosition)) {
            return;
        }
        Position plotterCharacterPosition = BlocksUtils.gridPosToComponentPosition(gridPosition);

        boolean adding = false;
        BlocksPlot plotAtPos = (BlocksPlot)plotter.getPlotAt(plotterCharacterPosition.getRow(), plotterCharacterPosition.getCol());
        if (plotAtPos == null) {
            adding = true;
            plotAtPos = new BlocksPlot(plotterCharacterPosition.getRow(), plotterCharacterPosition.getCol(), plotColor);
        }

        BlocksPlot newPlotAtPos = BlocksUtils.setBlockPixel(plotAtPos, gridPosition, plotColor);
        if (adding) {
            this.plotter.addPlot(newPlotAtPos);
        }
    }

    public void plotPosition(Position gridPosition) {
        plotPosition(gridPosition, this.plotter.getColor());
    }

    public void plotPosition(int row, int col) {
        plotPosition(new Position.Simple(row, col));
    }

    public void plotPosition(float row, float col) {
        plotPosition(new Position.Simple(row, col));
    }

    public void plotPosition(float row, float col, Color plotColor) {
        plotPosition(new Position.Simple(row, col), plotColor);
    }

//    public void plotPosition(int row, int col, Color plotColor, char theCHAR) {
//        plotPosition(new Position.Simple(row, col), plotColor, theCHAR);
//    }

//    public void unplotPosition(Position gridPosition) {
//        Position pos = BlocksUtils.gridPosToComponentPosition(gridPosition);
//        Plot plotAtPos = plotter.getPlotAt(pos.getRow(), pos.getCol());
//        char theCHAR = plotAtPos == null ? 0 : plotAtPos.getPlotChar();
//        theCHAR = BlocksUtils.unsetBlockPixel(theCHAR, gridPosition);
//        if (plotAtPos != null) {
//            plotAtPos.setPlotChar(theCHAR);
//        } else {
//            this.plotter.addPlot(pos.getRow(), pos.getCol(), theCHAR);
//        }
//    }

    public void unplotPosition(int row, int col) {
        // unplotPosition(new Position.Simple(row, col));
    }

    public PointPlotter asPointPlotter() {
        PointPlotter plot = new PointPlotter() {
            @Override
            public void plot(float x, float y) {
                plotPosition(y, x);
            }

            @Override
            public Color setNextPlotColor(Color color) {
                Color prevColor = BlocksGrid.this.getPlotColor();
                BlocksGrid.this.setPlotColor(color);
                return prevColor;
            }

            @Override
            public void clearPlots() {
                BlocksGrid.this.clearPlots();
            }

        };
        return plot;
    }

    public Color getPlotColor() {
        return this.plotter.getColor();
    }

    public void setPlotColor(Color color) {
        this.plotter.setColor(color);
    }

    public void clearPlots() {
        this.plotter.clearPlots();
    }

    @Override
    public Collection<? extends Component<?>> getChildren() {
        return Collections.singleton(this.plotter);
    }

    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        return Position.ZERO;
    }

    @Override
    public void childResized(Component<?> child) {
        this.markAsDirty();
        
//      if (lastRecordedSize.isSameDimension(this)) {
//          return;
//      }
//      
//      lastRecordedSize = new Dimension.Simple(this);
      
//      Logging.info("childResized() --> " + HasName.UTIL.getName(this) +  "    X child = " + HasName.UTIL.getName(child)  + " child size = "+ Dimension.UTIL.shortStr(child));
      
      if (this.getParentContainer() != null) {
          this.getParentContainer().childResized(this);
      }
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer mySegments) {
        // this next call is basically just a fast ptr assignment - so very quick and no added memory requirements
        if (this.contains(this.plotter)) {
            segments.clearAndMerge(this.plotter.getSegmentsBuffer().getSegments());
            
        } else {
            Rectangle clippedArea = this;
            ScreenLineSegment.UTIL.addSegmentsWithOffsetBounded(Position.ZERO, 
                                                                clippedArea, 
                                                                segments.getSegments(),
                                                                this.plotter.getSegmentsBuffer().getSegments());
        }
    }
    
    
}
