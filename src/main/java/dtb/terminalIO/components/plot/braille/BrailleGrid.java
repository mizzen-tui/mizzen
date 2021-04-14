package dtb.terminalIO.components.plot.braille;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.plot.Plotter;
import dtb.terminalIO.components.plot.Plotter.Plot;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionTransform;
import dtb.terminalIO.textsegments.CleanString;


// TODO:   Dont extend composite (too slow)..  change and then optimize draw() method
public class BrailleGrid extends Composite {

    Plotter plotter;
    Rectangle gridRectangle;

    public BrailleGrid(Color color, Dimension d) {
        super(color);
        this.plotter = new Plotter(color, new Dimension.Simple(d.getHeight(), d.getWidth()));
        this.add(plotter);
        this.plotter.setName("grid-plotter");
        gridRectangle = BrailleUtils.componentRectangleToGridRectangle(plotter);
    }

    public BrailleGrid(Color color, int height, int width) {
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
        // what about if we have a border on this component
        this.plotter.resize(d);
        gridRectangle = BrailleUtils.componentRectangleToGridRectangle(plotter);
    }

    public BrailleGrid setEmptyChar(char emptyChar) {
        super.setEmptyChar(emptyChar);
        this.plotter.setEmptyChar(emptyChar);
        return this;
    }

    public Rectangle getGridRectangle() {
        return this.gridRectangle;
    }
    
    public void markAsDirty() {
        super.markAsDirty();
        this.plotter.markAsDirty();
    }

    Position gridPosToComponentPosition(Position gridPosition) {
        return new Position.Simple(gridPosition.getRow() / 4, gridPosition.getCol() / 2);
    }

    public void plotPosition(Position gridPosition, Color plotColor, CleanString text) {
        if (!this.gridRectangle.containsPosition(gridPosition)) {
            return;
        }
        Position plotterCharacterPosition = BrailleUtils.gridPosToComponentPosition(gridPosition);
        this.plotter.addText(plotterCharacterPosition.getRow(), plotterCharacterPosition.getCol(), text, plotColor);
    }

    public void plotPosition(Position gridPosition, CleanString text) {
        plotPosition(gridPosition, this.plotter.getColor(), text);
    }

    public void plotPosition(Position gridPosition, Color plotColor, char theCHAR) {
        if (!this.gridRectangle.containsPosition(gridPosition)) {
            return;
        }
        Position plotterCharacterPosition = BrailleUtils.gridPosToComponentPosition(gridPosition);

        Plot plotAtPos = plotter.getPlotAt(plotterCharacterPosition.getRow(), plotterCharacterPosition.getCol());
        if (plotAtPos != null) {
            plotAtPos.setPlotChar(theCHAR);
            plotAtPos.setPlotColor(plotColor);
        } else {
            this.plotter.addPlot(plotterCharacterPosition.getRow(), plotterCharacterPosition.getCol(), theCHAR,
                    plotColor);
        }
    }

    public void plotPosition(Position gridPosition, Color plotColor) {
        if (!this.gridRectangle.containsPosition(gridPosition)) {
            return;
        }
        Position plotterCharacterPosition = BrailleUtils.gridPosToComponentPosition(gridPosition);

        Plot plotAtPos = plotter.getPlotAt(plotterCharacterPosition.getRow(), plotterCharacterPosition.getCol());
        char theCHAR = plotAtPos == null ? 0 : plotAtPos.getPlotChar();
        theCHAR = BrailleUtils.setBraillePixel(theCHAR, gridPosition);
        if (plotAtPos != null) {
            // update the plot with new char
            plotAtPos.setPlotChar(theCHAR);
            plotAtPos.setPlotColor(plotColor);
        } else {
            this.plotter.addPlot(plotterCharacterPosition.getRow(), 
                                 plotterCharacterPosition.getCol(), 
                                 theCHAR,
                                 plotColor);
        }
    }

    public void plotPosition(Position gridPosition) {
        plotPosition(gridPosition, this.plotter.getColor());
    }

    public void plotPosition(int row, int col) {
        plotPosition(new Position.Simple(row, col));
    }

    public void plotPosition(int row, int col, Color plotColor) {
        plotPosition(new Position.Simple(row, col), plotColor);
    }

    public void plotPosition(int row, int col, Color plotColor, char theCHAR) {
        plotPosition(new Position.Simple(row, col), plotColor, theCHAR);
    }

    public void unplotPosition(Position gridPosition) {
        Position pos = BrailleUtils.gridPosToComponentPosition(gridPosition);
        Plot plotAtPos = plotter.getPlotAt(pos.getRow(), pos.getCol());
        char theCHAR = plotAtPos == null ? 0 : plotAtPos.getPlotChar();
        theCHAR = BrailleUtils.unsetBraillePixel(theCHAR, gridPosition);
        if (plotAtPos != null) {
            plotAtPos.setPlotChar(theCHAR);
        } else {
            this.plotter.addPlot(pos.getRow(), pos.getCol(), theCHAR);
        }
    }

    public void unplotPosition(int row, int col) {
        unplotPosition(new Position.Simple(row, col));
    }

    public PointPlotter asPointPlotter() {
        PointPlotter plot = new PointPlotter() {
            @Override
            public void plot(int x, int y) {
                plotPosition(y, x);
            }

            @Override
            public Color setNextPlotColor(Color color) {
                Color prevColor = BrailleGrid.this.getPlotColor();
                BrailleGrid.this.setPlottingColor(color);
                return prevColor;
            }

            @Override
            public void clearPlots() {
                BrailleGrid.this.clearPlots();
            }

        };
        return plot;
    }

    public Color getPlotColor() {
        return this.plotter.getColor();
    }

    public void setPlottingColor(Color color) {
        this.plotter.setColor(color);
    }

    public void clearPlots() {
        this.plotter.clearPlots();
    }

}
