package dtb.terminalIO.components.chart;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.braille.BrailleGrid;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.shape.ConnectedPoints;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment.SegmentImpl;
import dtb.terminalIO.util.StringUtils;

/**
 * A Component that renders a line chart given charting numerical data and
 * charting labels
 */
public class LineChart extends Composite {

    static final char   VDASH           = '┊';
    static final char   HDASH           = '┈';
    static final char   ORIGIN          = '└';

    double              y_topValue;
    double              y_bottomValue;
    double              x_leftValue;
    double              x_rightValue;
    TreeSet<ChartData>  all_data;
    double              y_scale;
    double              x_scale;
    int                 axisWidth;
    int                 axisHeight;
    int                 axisYLabelGap   = 1;
    int                 axisXLabelGap   = 2;
    String[]            labelY;
    Color               plotColor       = FGBGColors.NO_COLOR_SPECIFIED;
    Map<String, Series> nameToSeriesMap = new HashMap<String, Series>();

    BrailleGrid         grid;
    private int         labelYSpace;
    private boolean     autoLabels;
    private String      labelX;

    private Series      DEFAULT_series  = new Series(null, null);

    /**
     * A series is a list of data points to plot.
     * A chart can have multiple series that it can plot
     */
    public static class Series {
        public String   name;
        public Color    color;
        public int      count = 0;
        List<ChartData> data;
        double          y_topValue;
        double          y_bottomValue;
        double          x_leftValue;
        double          x_rightValue;

        public Series(String name, Color color) {
            super();
            this.name = name;
            this.color = color;
            this.count = 0;
            data = new ArrayList<ChartData>();
            this.y_topValue = Double.MIN_VALUE;
            this.y_bottomValue = Double.MAX_VALUE;
            this.x_rightValue = Double.MIN_VALUE;
            this.x_leftValue = Double.MAX_VALUE;
        }

        public void addData(ChartData cData) {
            if (cData.x == null) {
                if (this.data.size() == 0) {
                    cData.x = 0d;
                } else {
                    cData.x = this.x_rightValue + 1;
                }
            }

            this.data.add(cData);

            if (this.y_bottomValue > cData.y) {
                this.y_bottomValue = cData.y;
            }
            if (this.y_topValue < cData.y) {
                this.y_topValue = cData.y;
            }

            if (this.x_leftValue > cData.x) {
                this.x_leftValue = cData.x;
            }
            if (this.x_rightValue < cData.x) {
                this.x_rightValue = cData.x;
            }
        }

    }

    /**
     * A ChartData object represents a plottable point
     */
    public static class ChartData {
        public Series series; // points back to its series
        public Double y;
        public Double x;
        public String label;

        public ChartData(Series series, Double y, Double x, String label) {
            super();
            this.series = series;
            this.y = y;
            this.x = x;
            this.label = label;
        }

        public ChartData(Series series, Double y, Double x) {
            this(series, y, x, null);
        }

        public ChartData(Series series, Double y) {
            this(series, y, null, null);
        }

        public ChartData(Series series, Double y, String label) {
            this(series, y, null, label);
        }

        /**
         * Sort by X position
         */
        public static Comparator<ChartData> COMPARATOR_BY_XPOS = new Comparator<ChartData>() {
            @Override
            public int compare(ChartData o1, ChartData o2) {
                // warning! not testing for null!! might cause me grief in the future
                return Double.compare(o1.x, o2.x);
            }
        };
    }

    private static Dimension __brailleGridSizeFromLineChartSize(Dimension lineChartSize) {
	return new Dimension.Simple(lineChartSize.getHeight() - 3, lineChartSize.getWidth() - 3);
    }
    
    public LineChart(Color color, Dimension d) {
        super(color);
        this.setBounds(d);
        this.grid = new BrailleGrid(color, __brailleGridSizeFromLineChartSize(d));
        this.add(this.grid);
        this.all_data = new TreeSet<ChartData>(ChartData.COMPARATOR_BY_XPOS);
    }

    public void resize(Dimension d) {
	super.resize(d);
//Logging.info("LineChart.resize():" + d);
	this.grid.resize(__brailleGridSizeFromLineChartSize(d));
        this.clearData();
        this.clear();
    }
    
    public LineChart(Color color, Dimension dim, double[] data) {
        this(color, dim);
        for (double d : data) {
            addData(d);
        }
    }

    public LineChart(Color color, int height, int width) {
        this(color, new Dimension.Simple(height, width));
    }

    public LineChart(Dimension d) {
        this(FGBGColors.NO_COLOR_SPECIFIED, d);
    }

    public LineChart(int height, int width) {
        this(new Dimension.Simple(height, width));
    }

    private Series nameToSeries(String seriesName) {
        if (seriesName == null) {
            return DEFAULT_series;
        }
        Series series = nameToSeriesMap.get(seriesName);
        if (series == null) { // create
            series = new Series(seriesName, this.plotColor);
            nameToSeriesMap.put(seriesName, series);
        }
        return series;
    }

    public LineChart setSeriesColor(String seriesName, Color seriesColor) {
        Series series = nameToSeries(seriesName);
        series.color = seriesColor;
        return this;
    }

    boolean gridBoundsFixed = false;

    public void setGridBounds(double y_bottomValue, double x_leftValue, double y_topValue, double x_rightValue) {
        if (y_topValue < y_bottomValue)
            throw new IllegalArgumentException("assertion (y_topValue >= y_bottomValue) failed");
        if (x_rightValue < x_leftValue)
            throw new IllegalArgumentException("assertion (x_rightValue >= x_leftValue) failed");

        this.y_topValue = y_topValue;
        this.y_bottomValue = y_bottomValue;
        this.x_rightValue = x_rightValue;
        this.x_leftValue = x_leftValue;

        gridBoundsFixed = true;
    }

    public LineChart addData(String seriesName, double y, Double x, String label) {

        Series series = nameToSeries(seriesName);

        ChartData chartData = new ChartData(series, y, x, label);

        series.addData(chartData);

        this.all_data.add(chartData);

        if (!gridBoundsFixed) {
            if (y_bottomValue > series.y_bottomValue) {
                y_bottomValue = series.y_bottomValue;
            }
            if (y_topValue < series.y_topValue) {
                y_topValue = series.y_topValue;
            }

            if (this.x_leftValue > series.x_leftValue) {
                this.x_leftValue = series.x_leftValue;
            }
            if (this.x_rightValue < series.x_rightValue) {
                this.x_rightValue = series.x_rightValue;
            }
        }
        return this;
    }

    // for debugging ..
    String stats() {
        String s = String.format(
                " y_bottomValue = %6.2f   y_scale       = %6.2f \n" 
              + " y_topValue    = %5.2f   x_scale       = %6.2f \n"
              + " x_leftValue   = %6.2f   grid height   = %6d  %3d \n" 
              + " x_rightValue  = %5.2f   grid width    = %6d  %3d \n"
              + " calc(y_top)   = %6d   calc(x_right) = %6d\n"
              + " calc(y_bot)   = %6d   calc(x_left)  = %6d",
                y_bottomValue, y_scale, y_topValue, x_scale, x_leftValue, getGridRectangle().getHeight(), this.grid.getHeight(), x_rightValue,
                getGridRectangle().getWidth(), this.grid.getWidth(), 
                translate_to_y_plotValue(y_topValue), 
                translate_to_x_plotValue(x_rightValue),
                translate_to_y_plotValue(y_bottomValue), 
                translate_to_x_plotValue(x_leftValue)
                );
        return s;
    }

    public LineChart addData(String seriesName, double y, String label) {
        return addData(seriesName, y, null, label);
    }

    public LineChart addData(double d) {
        return addData(null, d, null);
    }

    public LineChart addData(double d, String label) {
        return addData(null, d, label);
    }

    public void clearData() {

        this.all_data.clear();
        DEFAULT_series = new Series(null, null);
        nameToSeriesMap.clear();

        y_bottomValue = Double.MAX_VALUE;
        y_topValue = Double.MIN_VALUE;
        y_scale = 0;

        x_leftValue = Double.MAX_VALUE;
        x_rightValue = Double.MIN_VALUE;

        x_scale = 0;

    }

    void renderBraille(Series series) {

        int dataLen = series.data.size();
        if (dataLen == 0) {
            return;
        }

        int gridLimitWidth = this.grid.getGridRectangle().getWidth();

        Color prevColor = null;
        PointPlotter pp = grid.asPointPlotter();

        // set color
        if (series.color != null) {
            prevColor = pp.setColor(series.color);
        }

        // plot points

        ConnectedPoints graphedLine = new ConnectedPoints(false);

        for (int i = 0; i < dataLen && i < gridLimitWidth; i++) {
            int y = translate_to_y_plotValue(series.data.get(i).y);
            int x = translate_to_x_plotValue(series.data.get(i).x);
            graphedLine.addPoint(x, y);
        }

        graphedLine.draw(pp);

        // todo:  should make the following optional - driven by series config
//        for (int i = 0; i < dataLen && i < gridLimitWidth; i++) {
//            int y = translate_to_y_plotValue(series.data.get(i).y);
//            int x = translate_to_x_plotValue(series.data.get(i).x);
//            this.grid.plotPosition(new Position.Simple(y, x), Colors.IYel, '*');
//        }

        // unset color
        if (series.color != null) {
            prevColor = pp.setColor(prevColor);
        }

    }

    void renderBraille() {

        renderBraille(DEFAULT_series);

        for (Series series : nameToSeriesMap.values()) {
            renderBraille(series);
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

    private void calcLabelX() {
        int l = 0;
        String _labelX = "";
        String gap = StringUtils.repeat(' ', this.axisXLabelGap);
        int dataLabelsLen = this.all_data.size();
        Iterator<ChartData> i_all_data = all_data.iterator();
        for (int i = 0; i < dataLabelsLen && l < this.axisWidth; i++) {
            // assume braille mode
            if (2 * l >= dataLabelsLen)
                break;

            String s = i_all_data.next().label;
            if (i_all_data.hasNext())
                i_all_data.next();
            int w = s.length();
            if (l + w <= this.axisWidth) {
                _labelX = _labelX + s + gap;
            }
            l += w + this.axisXLabelGap;
        }
        this.labelX = _labelX;
    }

    /**
     * Rectangle for the grid - translated for braille points
     */
    public Rectangle getGridRectangle() {
        return this.grid.getGridRectangle();
    }

    private void calcLabelY() {
        double span = this.y_topValue - this.y_bottomValue;
        int n = (this.axisHeight) / (this.axisYLabelGap + 1) + 1;
        // n = this.axisHeight;
        this.labelY = new String[n];
        int maxLen = 0;
        for (int i = 0; i < n; i++) {
            String s = shortenFloatVal(this.y_bottomValue + ((double) i) * span / ((double) (n)));
            if (s.length() > maxLen) {
                maxLen = s.length();
            }
            this.labelY[i] = s;
        }
        this.labelYSpace = maxLen;
    }

    private void calcLayout() {
        // set data labels if not provided
        // if (this.dataLabels == null || this.dataLabels.size() == 0 ||
        // this.autoLabels) {
        // this.autoLabels = true;
        // int dataLen = this.data.size();
        // this.dataLabels = new ArrayList<String>(dataLen);
        // for (int i = 0; i < dataLen; i++) {
        // this.dataLabels.add(i + "");
        // }
        // }

        Rectangle inner = getInnerRectangle();
        this.axisHeight = inner.getHeight() - 2;
        calcLabelY();
        this.axisWidth = inner.getWidth() - this.labelYSpace;
        calcLabelX();
        grid.setEmptyChar(' ');
        grid.resize(new Dimension.Simple(axisHeight, axisWidth - 1));
        grid.setBorder(false);
        this.moveChildTo(new Position.Simple(0, this.labelYSpace + 1), grid);

        int gridPlotHeight = this.grid.getGridRectangle().getHeight();
        double y_span = this.y_topValue - this.y_bottomValue;
        this.y_scale = y_span / (double) (gridPlotHeight - 1);

        int gridPlotWidth = this.grid.getGridRectangle().getWidth();
        double x_span = this.x_rightValue - this.x_leftValue;
        this.x_scale = x_span / (double) (gridPlotWidth - 1);
    }

    int translate_to_y_plotValue(double yvalue) {
        int y = (this.grid.getGridRectangle().getHeight()-1) - (int) Math.round(((yvalue - this.y_bottomValue) / this.y_scale) ) ;
        return y;
    }

    int translate_to_x_plotValue(double xvalue) {
        int x = (int) Math.round(((xvalue - this.x_leftValue) / this.x_scale) );
        return x;
    }

    
    
    private void plotAxes(Color color, IScreenLineSegmentsBuffer segments) {
        Rectangle inner = getInnerRectangle();
        int origY = inner.getHeight() - 1;
        int origX = inner.getCol() + this.labelYSpace;

        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < axisWidth - 1; i++) {
            sb.append(HDASH);
        }

        segments.add(new SegmentImpl(color, origY, origX, 1, ORIGIN + sb.toString()));

        // draw vertical y-axis
        for (int i = 1; i <= axisHeight; i++) {
            segments.add(new SegmentImpl(color, i, origX, 1, VDASH + ""));
        }

        for (int i = 0; i < labelY.length; i++) {
            int lenStr = labelY[i].length();
            segments.add(new SegmentImpl(color, origY - i * (this.axisYLabelGap + 1), this.labelYSpace - lenStr + 1, 1, labelY[i]));
        }

        segments.add(new SegmentImpl(color, origY + 1, origX, 1, this.labelX));

    }

    // BorderedComponent info = null;
    //TextComponent     tc   = null;

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        this.grid.clearPlots();
        calcLayout();
        plotAxes(color, segments);
        renderBraille();

        super.draw(color, segments);
    }

    public LineChart setPlotColor(Color plotColor) {
        this.plotColor = plotColor;
        this.grid.setPlotColor(plotColor);
        return this;
    }


}
