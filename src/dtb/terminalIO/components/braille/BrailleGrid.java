package dtb.terminalIO.components.braille;

import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.plot.Plotter;
import dtb.terminalIO.components.plot.Plotter.Plot;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.shape.LinePlotter;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;

public class BrailleGrid extends Composite {

    Plotter plotter;
    Rectangle gridRectangle;

    public BrailleGrid(Color color, Dimension d) {
	super(color);
	this.plotter = new Plotter(color, new Dimension.Simple(d.getHeight(), d.getWidth()));
	this.add(plotter);
	gridRectangle = BrailleUtils.componentRectangleToGridRectangle(plotter);
    }

    public void setEmptyChar(char emptyChar) {
        super.setEmptyChar(emptyChar);
        this.plotter.setEmptyChar(emptyChar);
    }
    
    public Rectangle getGridRectangle() {
	return this.gridRectangle;
    }

    Position gridPosToComponentPosition(Position gridPosition) {
	return new Position.Simple(gridPosition.getRow() / 4, gridPosition.getCol() / 2);
    }

    public void plotPosition(Position gridPosition) {
	if ( ! this.gridRectangle.containsPosition(gridPosition)) {
	    return ;  
	}
	Position plotterCharacterPosition = BrailleUtils.gridPosToComponentPosition(gridPosition);

	Plot plotAtPos = plotter.getPlotAt(plotterCharacterPosition.getRow(), plotterCharacterPosition.getCol());
	char theCHAR = plotAtPos == null ? 0 : plotAtPos.getPlotChar();
	theCHAR = BrailleUtils.setBraillePixel(theCHAR, gridPosition);
	if (plotAtPos != null) {
	    plotAtPos.setPlotChar(theCHAR);
	} else {
	    this.plotter.addPlot(plotterCharacterPosition.getRow(), plotterCharacterPosition.getCol(), theCHAR);
	}
    }

    public void plotPosition(int row, int col) {
	plotPosition(new Position.Simple(row, col));
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
	};
	return plot;
    }
    
    public void drawline(int x1, int y1, int x2, int y2) {
	new LinePlotter().draw(x1, y1, x2, y2, asPointPlotter());
    }

    public void setPlotColor(Color color) {
	this.plotter.setColor(color);
	
    }

    public void clearPlots() {
	this.plotter.clearPlots();
    }
    
}
