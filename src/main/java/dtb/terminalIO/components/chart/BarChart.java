package dtb.terminalIO.components.chart;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.RectangleComponent;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.BgColor;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.controlcodes.ControlCode.FgColor;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.examples.TestsCommon;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment.SegmentImpl;
import dtb.terminalIO.util.StringUtils;

/**
 * A Barchart Component
 */
public class BarChart extends Composite {

    private List<Double> data;
    private List<String> dataLabels;
    private String[] labelY;
    private Color plotColor = FGBGColors.NO_COLOR_SPECIFIED;

    private int labelYSpace;
    private boolean autoLabels;
    private String labelX;

    private int barWidth;
    private int barGap;
    private int numBar;
    private double topValue;
    private double scale;
    private char cellChar = ' ';
    private BgColor barCOLOR;
    private Color labelCOLOR;
    private FgColor dataCOLOR;

    public BarChart(Color color, Dimension d) {
	super(color);
	setBounds(d.getHeight(), d.getWidth());
	data = new ArrayList<Double>();
	dataLabels = new ArrayList<String>();
	this.barWidth = 3;
	this.barGap = 1;
	barCOLOR = Colors.On_Blu;
	labelCOLOR = new FGBGColors(Colors.BGre);
	dataCOLOR = Colors.BYel;
	// this.plotter = new Plotter(color, new Dimension.Simple(d.getHeight()
	// - 3, d.getWidth() - 3));
	// this.add(this.plotter);
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
	this(FGBGColors.NO_COLOR_SPECIFIED, new Dimension.Simple(height, width));
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

    public BarChart setPlotColor(Color fgbgColors) {
	// throw new IllegalStateException("Not implemented yet!");
	return this;
    }

    private void calcLayout() {
	Rectangle inner = getInnerRectangle();
	int useableWidth = inner.getWidth();
	int dataLen = this.data.size();
	int calcBarGap = (useableWidth - dataLen * this.barWidth) / dataLen;
	if (calcBarGap > this.barGap) {
	    this.barGap = calcBarGap;
	}

	this.numBar = useableWidth / (this.barGap + this.barWidth);

	this.scale = this.topValue / (double) (inner.getHeight() - 3);

    }

    private void renderBars(Color color, IScreenLineSegmentsBuffer segments) {

	Rectangle inner = getInnerRectangle();

	int dataLen = this.data.size();
	String barLine = StringUtils.repeat(this.cellChar, this.barWidth);

	int bottomRow = inner.getBottomMostRow();

	Color c = new FGBGColors(this.barCOLOR);

	// render the bars
	for (int i = 0; i < this.numBar && i < dataLen; i++) {
	    double d = this.data.get(i);
	    int height = (int) Math.round(d / this.scale);
	    int offset = i * (this.barWidth + this.barGap) + 1 + this.barGap;

	    for (int h = 0; h < height; h++) {
		segments.add(new SegmentImpl(c, bottomRow - h - 2, offset, 1, barLine));
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
	    int height = (int) Math.round(d / this.scale);
	    int offset = i * (this.barWidth + this.barGap) + 1 + this.barGap;

	    segments.add(new SegmentImpl(c, bottomRow - 2, offset + 1, 1, shortenFloatVal(d)));
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
	// this.plotter.clearPlots();
	calcLayout();
	// plotAxes(color, segments);
	renderBars(color, segments);
	super.draw(color, segments);
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
	// master_canvas.setEmptyColor(new FGBGColors(Colors.Bla));

	master_canvas.setSize(25, 100);

	master_canvas.addComponentSegments(2, 4, new RectangleComponent(FGBGColors.DEFAULT, 5, 10));
	master_canvas.addComponentSegments(4, 8, new RectangleComponent(FGBGColors.DEFAULT, 5, 15));

	BarChart bc = new BarChart(20, 75);
	bc.setBorder(true);
	// bc.setEmptyChar('.');
	bc.setPlotColor(new FGBGColors(Colors.BIYel));

	double[] bc_DATA = new double[] { 10.0, 5.0, 13.0, 4.0, 6.0, 5.0, 6.0, 7.0, 8.0 };
	String[] bc_DATALabels = new String[] { "Lumber", "Metal", "Fish", "Beef", "Lamb", "Liquor", "Milk", "Butter",
		"Cherries" };

	// bc.barWidth = 5;
	// bc.barGap = 2;

	bc.setTitle("| Import Percentages |");

	Consumer<Integer> dataProducer = (j) -> {
	    bc.clearData();
	    bc.clear();

	    for (int i = 0; i < bc_DATA.length; i++) {
		double d = Math.random() * 20;

		bc.addData(d, bc_DATALabels[i]);
	    }
	    master_canvas.addComponentSegments(2, 15, bc);
	    master_canvas.t(bc.getBottomMostRow() + 3, 1, "j = " + j + "   ");
	    master_canvas.refresh();
	    master_canvas.t("  " + bc.topValue + "    ");
	};

	for (int j = 10; j < 1100; j++) {
	    dataProducer.accept(j);
	    TestsCommon.sleep(250);
	}

	screen.add(ControlCode.SHOW_CURSOR);
	screen.refresh();
	master_canvas.refresh();

	System.out.println(new String(outputStream.toByteArray()));

    }

}
