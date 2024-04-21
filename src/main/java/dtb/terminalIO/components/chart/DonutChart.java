package dtb.terminalIO.components.chart;

import java.util.Collection;
import java.util.Collections;

import dtb.terminalIO.components.AbstractComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Hiliter;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.plot.PointPlotter.PointPlot;
import dtb.terminalIO.components.plot.braille.BrailleGrid;
import dtb.terminalIO.components.plot.shape.Circle;
import dtb.terminalIO.components.plot.shape.CircleAlgorithm.DonutCircleAlgorithm;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

public class DonutChart extends AbstractComponent<DonutChart> implements ParentContainer, CanResize, Hiliter {
    

    private BrailleGrid grid;
    private TextComponent stats;
    private int dataPoint;
    private int maxValue;
    private String message;
    
    public DonutChart(Color color, Dimension size) {
        super(color);
        this.grid = new BrailleGrid(color, size);
        this.grid.setParentContainer(this);
        stats = new TextComponent("    ");
        this.grid.add(0,3,stats);
        this.setData(100, 100, "100%");
    }
    
    @Override
    public DonutChart setColor(Color drawingcolor) {
        super.setColor(drawingcolor);
        this.grid.setColor(drawingcolor);
        return this;
    }
    
    
    public DonutChart setPlottingColor(Color color) {
        this.grid.setPlottingColor(color);
        return this;
     }
    
    public DonutChart setTextColor(Color drawingcolor) {
       this.stats.setColor(drawingcolor);
        return this;
    }
    
    @Override
    public void hilite() {
        grid.hilite();
    }

    @Override
    public void unHilite() {
        grid.unHilite();
    }

    @Override
    public int getCol() {
        return grid.getCol();
    }

    @Override
    public int getRow() {
        return grid.getRow();
    }

    @Override
    public int getWidth() {
        return grid.getWidth();
    }

    @Override
    public int getHeight() {
        return grid.getHeight();
    }

    @Override
    public void moveTo(int row, int col) {
        grid.moveTo(row, col);
    }

    @Override
    public void resize(Dimension d) {
        grid.resize(d);
    }


    @Override
    public Collection<? extends Component<?>> getChildren() {
        return Collections.singleton(grid);
    }


    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        return Position.ZERO;
    }


    @Override
    public void childResized(Component<?> child) {
        markAsDirty();
        if (this.getParentContainer() != null) {
            this.getParentContainer().childResized(this);
        }
    }

    int shift90(int angle) {
      // shift 90 degrees
      return (angle + 90) % 360;
    }
    
    
    private int lastDataPoint = -1;
    
    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        
        // rotate clockwise
        int startAngle =  360 - (int)Math.round(((double)this.dataPoint / (double) this.maxValue  * 360.0 ));
        int endAngle = 0;
//        if (lastDataPoint >= 0 && lastDataPoint < this.dataPoint) {  // we are increasing
//            endAngle = 360 - (int)Math.round(((double)this.lastDataPoint / (double) this.maxValue  * 360.0 ));
//            lastDataPoint = this.dataPoint;
//        } 
//        lastDataPoint = this.dataPoint;
        
        startAngle = shift90(startAngle);  // now 0 is at 12 oclock ( 90 degrees )
        endAngle = shift90(endAngle);
//        startAngle = shift90(startAngle);  // now 0 is at 12 oclock ( 90 degrees )
//        endAngle = shift90(endAngle);

        DonutCircleAlgorithm donutPlotter = new DonutCircleAlgorithm(startAngle, endAngle);
        
        Rectangle gridRectangle = this.grid.getGridRectangle();

        PointPlot centerOfGrid = new PointPlot(gridRectangle.centerPosition());
        
        int radius = Math.min(gridRectangle.getWidth(), gridRectangle.getHeight()) / 2;
        
        Circle circle = new Circle(centerOfGrid, radius).setAlgorithm(donutPlotter);
        
        this.clear();
        
        if (lastDataPoint < 0) {
            this.grid.clearPlots();
            this.grid.clear();
        }
        circle.draw(  this.grid.asPointPlotter()  );
        
        if (message != null) {
            Position textPosition = this.grid.centered(this.stats, Position.ZERO);
            this.grid.moveChildTo(textPosition, stats);
        }
        
        segments.merge(this.grid.getSegmentsBuffer());
    }


    @Override
    protected Rectangle getRectangleForClearAndFill() {
        return grid;
    }

    public void markAsDirty() {
        super.markAsDirty();
        this.grid.markAsDirty();
    }

    public void setData(int dataPoint, int maxValue, String message) {
        this.dataPoint = dataPoint;
        this.maxValue = maxValue;
        this.message = message;
        this.stats.setTextAndResize(message);
        markAsDirty();
    }

}
