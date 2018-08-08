package dtb.terminalIO.components.chart;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;

import dtb.terminalIO.Canvas;
import dtb.terminalIO.Colors;
import dtb.terminalIO.ControlCode;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.TerminalScreen;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.RectangleComponent;
import dtb.terminalIO.examples.TestsCommon;
import dtb.terminalIO.geom.Dimension;

public class LineChart extends Composite {


    static final char VDASH = '┊' ;
    static final char HDASH = '┈';   
    static final char ORIGIN = '└';
    
    static Comparator<int[]> brailleKeyComparator = new Comparator<int[]>() {
        @Override
        public int compare(int[] o1, int[] o2) {
            int cmp = Integer.compare(o1[0], o2[0]);
            if (cmp != 0) return cmp;
            return Integer.compare(o1[1], o2[1]);
        }
    };
    
    
    static Map<int[],String> braillePatterns = new TreeMap<int[], String>(brailleKeyComparator);;
    static {
        braillePatterns.put(new int[]{0, 0}, "⣀");
        braillePatterns.put(new int[]{0, 1}, "⡠");
        braillePatterns.put(new int[]{0, 2}, "⡐");
        braillePatterns.put(new int[]{0, 3}, "⡈");

        braillePatterns.put(new int[]{1, 0}, "⢄");
        braillePatterns.put(new int[]{1, 1}, "⠤");
        braillePatterns.put(new int[]{1, 2}, "⠔");
        braillePatterns.put(new int[]{1, 3}, "⠌");

        braillePatterns.put(new int[]{2, 0}, "⢂");
        braillePatterns.put(new int[]{2, 1}, "⠢");
        braillePatterns.put(new int[]{2, 2}, "⠒");
        braillePatterns.put(new int[]{2, 3}, "⠊");

        braillePatterns.put(new int[]{3, 0}, "⢁");
        braillePatterns.put(new int[]{3, 1}, "⠡");
        braillePatterns.put(new int[]{3, 2}, "⠑");
        braillePatterns.put(new int[]{3, 3}, "⠉");
    }
    
    static char[] lSingleBraille = new char[]{'\u2840', '⠄', '⠂', '⠁'};
    static char[] rSingleBraille = new char[]{'\u2880', '⠠', '⠐', '⠈'};

    
    double topValue;
    double bottomValue;
    List<Double> data;
    double scale ;        // data span per cell on y-axis
    int axisWidth;
    FGBGColors plotColor = FGBGColors.NO_COLOR_SPECIFIED;
    
    Plotter plotter;

    public LineChart(FGBGColors color, Dimension d) {
        super(color);
        data = new ArrayList<Double>();
        this.plotter = new Plotter(color, new Dimension.Simple(d.getHeight()-3, d.getWidth() -3));
        this.add(this.plotter);
        this.axisWidth = plotter.getWidth();
    }

    public LineChart(FGBGColors color, Dimension dim, double[] data) {
        this(color, dim);
        for ( double d : data) {
            addData(d);
        }
    }
    
    public LineChart(FGBGColors color, int height, int width) {
        this(color, new Dimension.Simple(height, width));
    }

    public LineChart(int height, int width) {
        this(FGBGColors.NO_COLOR_SPECIFIED, new Dimension.Simple(height, width));
    }
    
    public LineChart addData(double d) {
        data.add(d);
        if (bottomValue > d) {
            bottomValue = d;
        }
        if (topValue < d) {
            topValue = d;
        }
        double span = this.topValue - this.bottomValue;
        this.scale = span / (double) (this.plotter.getHeight()-1);
        return this;
    }
    
    public void clearData() {
        this.data.clear();
        bottomValue = Double.MAX_VALUE;
        topValue = Double.MIN_VALUE;
        scale = 0;
    }

    
    
    void renderBraille() {
         Function<Double, int[]> getPos = (d) -> {
             int cnt4 = (int) Math.round((d - this.bottomValue) / (this.scale/4)  + 0.5);
             return new int[]{cnt4/4, cnt4 % 4};  
         };
         
         int dataLen = this.data.size();
         
         int yOffset = -1;
         // plot points
         for (int i=0; 2*i+1 < dataLen && i < this.axisWidth; i++) {
             int[] plot0 = getPos.apply(this.data.get(2*i));
             int[] plot1 = getPos.apply(this.data.get(2*i+1));
             
             if (plot0[0] == plot1[0]) { // same braille cell
                 int y = this.plotter.getHeight() - plot0[0] + yOffset ;
                 int x = 1 + i;
                 plotter.addPlot(y, x, braillePatterns.get(new int[] {plot0[1], plot1[1]}).charAt(0), plotColor);
             } else {
                 int y = this.plotter.getHeight() - plot0[0] + yOffset ;
                 int x = 1 + i;
                 plotter.addPlot(y, x, lSingleBraille[plot0[1]], plotColor );
                 
                 y = this.plotter.getHeight() - plot1[0] + yOffset ;
                 x = 1 + i;
                 plotter.addPlot(y, x, rSingleBraille[plot1[1]], plotColor );
             }
         }
    }
    
    @Override
    protected void draw(FGBGColors color, IScreenLineSegmentsBuffer segments) {
        this.plotter.clearPlots();
        renderBraille();
        super.draw(color, segments);
    }
    
    public LineChart setPlotColor(FGBGColors plotColor) {
        this.plotColor = plotColor;
        return this;
    }
    
    public static void main(String[] args) {
//        for (int i=0; i<4; i++) {
//            System.out.println(i + " " + lSingleBraille[i]);
//            System.out.println(i + " " + rSingleBraille[i]);
//        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TerminalScreen screen = new TerminalScreen();
        // screen.setPrintStream( new PrintStream( outputStream ));
        //screen.setControlCodesEnabled(false);
        
        screen.add(ControlCode.HIDE_CURSOR);
        screen.refresh();

        Canvas master_canvas = new Canvas(screen);
        master_canvas.setEmptyChar(' ');
        
        master_canvas.setSize( 30, 80 );

        master_canvas.addComponent(2,4, new RectangleComponent(FGBGColors.DEFAULT, 5,10));
        master_canvas.addComponent(4,8, new RectangleComponent(FGBGColors.DEFAULT, 5,15));

        LineChart lc = new LineChart(20, 50);
        lc.setBorder(true);
        lc.setPlotColor( new FGBGColors(Colors.BIYel));

        
        Consumer<Integer> dataProducer = (j) -> {
            lc.clearData();
            lc.clear();
            for (int i=j ; i<(220+j); i++) {
                double d = Math.sin(((float) i) / 5);
                //System.out.println(d);
                lc.addData(d);
                d = Math.cos(((float) i) / 5);
               // lc.addData(d);
             }
            master_canvas.addComponent(2,15, lc);
            master_canvas.t(1,1,"j = "+j+"   ");
            master_canvas.refresh();
        };
        
        for (int j=10; j<10000; j++) {
           dataProducer.accept(j);
           TestsCommon.sleep(100);
        }
        
//        master_canvas.addComponent(2,15, lc);
        
        screen.add(ControlCode.SHOW_CURSOR);
        screen.refresh();
        master_canvas.refresh();

        // System.out.println(  outputStream.toString() );
    }


    
    
}
