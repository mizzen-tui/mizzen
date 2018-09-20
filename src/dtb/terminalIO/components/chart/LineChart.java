package dtb.terminalIO.components.chart;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import dtb.terminalIO.Canvas;
import dtb.terminalIO.Colors;
import dtb.terminalIO.ControlCode;
import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.ScreenLineSegment.SegmentImpl;
import dtb.terminalIO.TerminalScreen;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.RectangleComponent;
import dtb.terminalIO.components.braille.BrailleGrid;
import dtb.terminalIO.components.plot.shape.ConnectedPoints;
import dtb.terminalIO.examples.TestsCommon;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.util.StringUtils;

/**
 * A Component that renders a line chart given charting numerical data and charfting labels
 */
public class LineChart extends Composite {

    static final char         VDASH                = '┊';
    static final char         HDASH                = '┈';
    static final char         ORIGIN               = '└';

    static Comparator<int[]>  brailleKeyComparator = new Comparator<int[]>() {
                                                       @Override
                                                       public int compare(int[] o1, int[] o2) {
                                                           int cmp = Integer.compare(o1[0], o2[0]);
                                                           if (cmp != 0)
                                                               return cmp;
                                                           return Integer.compare(o1[1], o2[1]);
                                                       }
                                                   };

    double          topValue;
    double          bottomValue;
    List<ChartData> data;
    double          scale;                                                  // data span per cell on y-axis
    int             axisWidth;
    int             axisHeight;
    int             axisYLabelGap  = 1;
    int             axisXLabelGap  = 2;
    List<String>    dataLabels;
    String[]        labelY;
    Color           plotColor      = FGBGColors.NO_COLOR_SPECIFIED;
    Map<String, Series> nameToSeriesMap = new HashMap<String, Series>();

    BrailleGrid     grid;
    private int     labelYSpace;
    private boolean autoLabels;
    private String  labelX;
    
    private Series DEFAULT_series = new Series(null,null);

    public static class Series {
	public String name;
	public Color color;
	public Series(String name, Color color) {
	    super();
	    this.name = name;
	    this.color = color;
	}
	
    }
    
    public static class ChartData {
	public Series series;
	public ChartData(Series series, Double value) {
	    super();
	    this.series = series;
	    this.value = value;
	}
	public Double value;
    }
    
    
    public LineChart(Color color, Dimension d) {
        super(color);
        data = new ArrayList<ChartData>();
        dataLabels = new ArrayList<String>();
        this.grid = new BrailleGrid(color, new Dimension.Simple(d.getHeight() - 3, d.getWidth() - 3));
        this.add(this.grid);
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

    public LineChart(int height, int width) {
        this(FGBGColors.NO_COLOR_SPECIFIED, new Dimension.Simple(height, width));
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
    
    public LineChart addData(String seriesName, double d, String label) {
	
	Series series = nameToSeries(seriesName);
	
        data.add(new ChartData(series, d));
        int size = data.size();
        if (label != null) {
            while (this.dataLabels.size() + 1 < size ) {
                this.dataLabels.add(null);
            }
            this.dataLabels.add(label);
        }
        if (bottomValue > d) {
            bottomValue = d;
        }
        if (topValue < d) {
            topValue = d;
        }
        return this;
    }

    public LineChart addData(double d) {
        return addData(null, d, null);
    }

    public LineChart addData(double d, String label) {
        return addData(null, d, label);
    }

    public void clearData() {
        this.data.clear();
        this.dataLabels.clear();
        bottomValue = Double.MAX_VALUE;
        topValue = Double.MIN_VALUE;
        scale = 0;
    }

    void renderBraille() {

        int dataLen = this.data.size();

        int gridLimitWidth = this.grid.getGridRectangle().getWidth();
        
        // plot points
        
        ConnectedPoints graphedLine = new ConnectedPoints(false);
        
        for (int i = 0; i < dataLen && i < gridLimitWidth; i++) {
            int y = (int) Math.round((this.data.get(i).value - this.bottomValue) / this.scale  + 0.5);
            graphedLine.addPoint(i, y);
        }
        
        graphedLine.draw(grid.asPointPlotter());
        
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
        int dataLabelsLen = this.dataLabels.size();
        for (int i = 0; i < dataLabelsLen && l < this.axisWidth ; i++ ) {
            // assume braille mode
            if (2*l >= dataLabelsLen ) break;
            String s = this.dataLabels.get(2*l);
            int w = s.length();
            if (l+w <= this.axisWidth) {
                _labelX = _labelX + s + gap; 
            }
            l += w + this.axisXLabelGap;
        }
        this.labelX = _labelX;
    }

    private void calcLabelY() {
        double span = this.topValue - this.bottomValue;
        int n = (this.axisHeight) / (this.axisYLabelGap+1) + 1;
        //n = this.axisHeight;
        this.labelY = new String[n];
        int maxLen = 0;
        for (int i = 0; i < n; i++) {
            String s = shortenFloatVal(this.bottomValue + ((double) i) * span / ((double) (n)));
            if (s.length() > maxLen) {
                maxLen = s.length();
            }
            this.labelY[i] = s;
        }
        this.labelYSpace = maxLen;
    }

    private void calcLayout() {
        // set data labels if not provided
        if (this.dataLabels == null || this.dataLabels.size() == 0 || this.autoLabels) {
            this.autoLabels = true;
            int dataLen = this.data.size();
            this.dataLabels = new ArrayList<String>(dataLen);
            for (int i = 0; i < dataLen; i++) {
                this.dataLabels.add( i + "");
            }
        }

        Rectangle inner = getInnerRectangle();
        this.axisHeight = inner.getHeight() - 2;
        calcLabelY();
        this.axisWidth = inner.getWidth() -  this.labelYSpace;
        calcLabelX();
        grid.setEmptyChar(' ');
        grid.resize(new Dimension.Simple(axisHeight, axisWidth-1));
        grid.setBorder(false);
        this.moveChildTo(new Position.Simple(0,this.labelYSpace+1),  grid);
        int gridPlotHeight = this.grid.getGridRectangle().getHeight();
        double span = this.topValue - this.bottomValue;
        this.scale = span / (double) (gridPlotHeight);
        
        
    }
    
    private void plotAxes(Color color, IScreenLineSegmentsBuffer segments) {
        Rectangle inner = getInnerRectangle();
        int origY = inner.getHeight() -1;
        int origX = inner.getCol() + this.labelYSpace;
        
        StringBuffer sb = new StringBuffer();

        for (int i=0; i<axisWidth-1; i++) {
            sb.append(HDASH);
        }
        
        segments.add( new SegmentImpl(color, origY, origX, 1, ORIGIN+sb.toString()) );

        // draw vertical y-axis
        for (int i=1; i<=axisHeight; i++) {
            segments.add( new SegmentImpl(color, i, origX, 1, VDASH+"") );
        }
        

        for (int i=0; i<labelY.length; i++) {
            int lenStr = labelY[i].length();
            segments.add( new SegmentImpl(color, origY -i*(this.axisYLabelGap+1), this.labelYSpace - lenStr +1, 1, labelY[i]) );
        }
        
        segments.add( new SegmentImpl(color, origY + 1, origX, 1, this.labelX ));
        
    }

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

    public static void main(String[] args) {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TerminalScreen screen = new TerminalScreen();
        // screen.setPrintStream( new PrintStream( outputStream ));
        // screen.setControlCodesEnabled(false);

        screen.add(ControlCode.HIDE_CURSOR);
        screen.refresh();

        Canvas master_canvas = new Canvas(screen);
        master_canvas.setEmptyChar(' ');
//        master_canvas.setEmptyColor(new FGBGColors(Colors.Bla));

        master_canvas.setSize(30, 130);

        master_canvas.addComponent(2, 4, new RectangleComponent(FGBGColors.DEFAULT, 5, 10));
        master_canvas.addComponent(4, 8, new RectangleComponent(FGBGColors.DEFAULT, 5, 15));

        LineChart lc = new LineChart(20, 60);
        lc.setBorder(true);
        lc.setPlotColor(new FGBGColors(Colors.BIYel));

        BarChart bc = new BarChart(15, 50);
        bc.setBorder(true);
        //bc.setEmptyChar('.');
       // bc.setPlotColor(new FGBGColors(Colors.BIYel));
        bc.setTitle("| Import Percentages |");

        double[] bc_DATA = new double[]{10.0,5.0,13.0,4.0, 6.0, 5.0, 6.0,7.0, 8.0};
        String[] bc_DATALabels = new String[]{"Lumber","Metal","Fish","Beef", "Lamb", "Liquor", "Milk", "Butter", "Cherries"};
        
        
        Consumer<Integer> dataProducer = (j) -> {
            lc.clearData();
            lc.clear();
            for (int i = j; i < (220 + j); i++) {
                double d = Math.sin(((float) i) / 5) * 10;
                // System.out.println(d);
                lc.addData(d, i+"");
                d = Math.cos(((float) i) / 5)*3;
                lc.addData(d);
            }
            
            bc.clearData();
            bc.clear();
            
            for (int i = 0; i < bc_DATA.length  ; i++) {
        	double d = Math.random()*20;
        	
                bc.addData(d, bc_DATALabels[i]);
            }
            master_canvas.addComponent(2, 15, bc);
            
            master_canvas.addComponent(5, 65, lc);
            master_canvas.t(31, 1, "j = " + j + "   ");
            master_canvas.refresh();
            master_canvas.t(lc.bottomValue + "  " + lc.topValue + "    ");
        };

        for (int j = 10; j < 10000; j++) {
            dataProducer.accept(j);
            TestsCommon.sleep(250);
        }

        screen.add(ControlCode.SHOW_CURSOR);
        screen.refresh();
        master_canvas.refresh();

    }

}
