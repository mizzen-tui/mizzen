package dtb.terminalIO.components.chart;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import dtb.terminalIO.components.AbstractComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.SparkLine;

public class SparkLineComponent extends AbstractComponent<SparkLineComponent> implements ParentContainer, CanResize {

    
    private Composite container;
    private TextComponent chart;

    int currentMax = Integer.MIN_VALUE;
    int currentMin = Integer.MAX_VALUE;
    boolean trackingMinMax = false;
    
    public SparkLineComponent(Color color) {
        super(color);
        container = new Composite(color).setName("sparkLineContainer");
        chart = new TextComponent(color, SparkLine.render(0));
        container.add(chart);
        container.setParentContainer(this);
    }

    public SparkLineComponent(Color color, Dimension size) {
        this(color);
        this.resize(size);
    }

    @Override
    public int getCol() {
        return container.getCol();
    }

    @Override
    public int getRow() {
        return container.getRow();
    }

    @Override
    public int getWidth() {
        return container.getWidth();
    }

    @Override
    public int getHeight() {
        return container.getHeight();
    }

    @Override
    public void moveTo(int row, int col) {
        container.moveTo(row, col);
        
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.merge(this.container.getSegmentsBuffer());
    }

    @Override
    protected Rectangle getRectangleForClearAndFill() {
        return container;
    }

    @Override
    public void resize(Dimension d) {
        this.container.resize(d);
        this.chart.resize(d);
        this.markAsDirty();
    }

    public Component<?> getComponentAtOffset(Position offset) {
        Component<?> component = this.container.getComponentAtOffset(offset);
        if (component == this.container) {
            return this;
        }
        return component;
    }
    
    @Override
    public Collection<? extends Component<?>> getChildren() {
        return Collections.singleton(container);
    }

    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        return Position.ZERO;
    }

    @Override
    public void childResized(Component<?> child) {
        markAsDirty();
    }

    public boolean isTrackingMinMax() {
        return trackingMinMax;
    }

    public void setTrackingMinMax(boolean trackingMinMax) {
        this.trackingMinMax = trackingMinMax;
    }

    public void renderPoints(Integer[] data) {
        List<Integer> dataList = Arrays.asList(data);
        if (trackingMinMax) {
            final int max = Math.max(Collections.max(dataList),currentMax), min = Math.min(Collections.min(dataList), currentMin);
            currentMax = max;
            currentMin = min;
            renderPoints(dataList, currentMax, currentMin );
            return;
        }
        chart.setText( SparkLine.render( Arrays.asList(data), chart.getHeight())  );
    }

    public void renderPoints(List<Integer> dataList) {
        if (trackingMinMax) {
            final int max = Math.max(Collections.max(dataList),currentMax), min = Math.min(Collections.min(dataList), currentMin);
            currentMax = max;
            currentMin = min;
            renderPoints(dataList, currentMax, currentMin );
            return;
        }
        chart.setText( SparkLine.render( dataList, chart.getHeight())  );
    }


    public void renderPoints(Integer[] data, int currentMax, int currentMin) {
        chart.setText( SparkLine.render( Arrays.asList(data), chart.getHeight(), currentMax, currentMin)  );
    }

    public void renderPoints(List<Integer> dataList, int currentMax, int currentMin) {
        chart.setText( SparkLine.render( dataList, chart.getHeight(), currentMax, currentMin)  );
    }

}
