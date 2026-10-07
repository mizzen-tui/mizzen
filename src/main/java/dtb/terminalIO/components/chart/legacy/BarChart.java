package dtb.terminalIO.components.chart;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment.SegmentImpl;
import dtb.terminalIO.util.StringUtils;

/**
 * A Barchart Component
 */
public class BarChart extends BoundedComponent<BarChart>   {

    private List<Double> data;
    private List<String> dataLabels;


    private int barWidth;
    private int barGap;
    private int numBar;
    private double topValue;
    private double scale;
    //private char cellChar = '⣿';
    private char cellChar = ' ';
    private Color barCOLOR;
    private Color labelCOLOR;

    public BarChart(Color color, Dimension d) {
        super(color, d);
        data = new ArrayList<Double>();
        dataLabels = new ArrayList<String>();
        this.barWidth = 3;
        this.barGap = 1;
        barCOLOR = Colors.On_Blu;
        labelCOLOR = Colors.BGre;
    }

    public BarChart(Color color, Dimension dim, double[] data) {
        this(color, dim);
        for (double d : data) {
            addData(d);
        }
    }

    public BarChart addData(double d, String label) {
        data.add(d);
        int size = data.size();
        if (label != null) {
            while (this.dataLabels.size() + 1 < size) {
                this.dataLabels.add(null);
            }
            this.dataLabels.add(label);
            int labelLen = label.length();
            if (labelLen > this.barWidth) {
                this.barWidth = labelLen;
            }
        }

        if (topValue < d) {
            topValue = d;
        }
        return this;
    }

    public BarChart(Color color, int height, int width) {
        this(color, new Dimension.Simple(height, width));
    }

    public BarChart(int height, int width) {
        this(Colors.FGBGColors.NO_COLOR_SPECIFIED, new Dimension.Simple(height, width));
    }

    public BarChart addData(double d) {
        return addData(d, null);
    }

    public void clearData() {
        this.data.clear();
        this.dataLabels.clear();
        topValue = Double.MIN_VALUE;
        this.barWidth = 3;
        this.barGap = 1;
        scale = 0;
    }

    public BarChart setPlotColor(Color color) {
        this.barCOLOR = color;
        return this;
    }

    private void calcLayout() {
        Rectangle meAsRectangle = this;
        int useableWidth = meAsRectangle.getWidth();
        int dataLen = this.data.size();
        int calcBarGap = dataLen < 1 ? 0 : (useableWidth - dataLen * this.barWidth) / dataLen;
        if (calcBarGap > this.barGap) {
            this.barGap = calcBarGap;
        }

        this.numBar = useableWidth / (this.barGap + this.barWidth);

        this.scale = this.topValue / (double) (meAsRectangle.getHeight() - 3);

    }

    private void renderBars(Color color, IScreenLineSegmentsBuffer segments) {

        Rectangle meAsRectangle = this;

        int dataLen = this.data.size();
        String barLine = StringUtils.repeat(this.cellChar, this.barWidth);

        int bottomRow = meAsRectangle.getBottomMostRow();

        Color numberCOLOR = barCOLOR.mergeWith(labelCOLOR);

        // render the bars
        for (int i = 0; i < this.numBar && i < dataLen; i++) {
            double d = this.data.get(i);
            int height = (int) Math.round(d / this.scale);
            int offset = i * (this.barWidth + this.barGap) + 1 + this.barGap;

            for (int h = 0; h < height; h++) {
                segments.add(new SegmentImpl(barCOLOR, bottomRow - h - 2, offset, 1, barLine));
            }
        }

        dataLen = this.dataLabels.size();
        for (int i = 0; i < this.numBar && i < dataLen; i++) {
            String label = this.dataLabels.get(i);
            if (label == null)
                label = "";
            label = StringUtils.center(label, this.barWidth);
            int offset = i * (this.barWidth + this.barGap) + 1 + this.barGap;
            segments.add(new SegmentImpl(labelCOLOR, bottomRow - 1, offset, 1, label));
        }

        // render the numbers
        for (int i = 0; i < this.numBar && i < dataLen; i++) {
            double d = this.data.get(i);
            int offset = i * (this.barWidth + this.barGap) + 1 + this.barGap;

            segments.add(new SegmentImpl(numberCOLOR, bottomRow - 2, offset + 1, 1, shortenFloatVal(d)));
        }

    }

    static private String shortenFloatVal(double x) {
        String s = String.format("%.2f", x);
        if (s.length() - 3 > 3) {
            s = String.format("%.2e", x);
        }
        if (x < 0) {
            s = String.format("%.2f", x);
        }
        return s;
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        calcLayout();
        renderBars(color, segments);
    }


}
