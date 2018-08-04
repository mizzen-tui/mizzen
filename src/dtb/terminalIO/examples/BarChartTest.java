package dtb.terminalIO.examples;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

import dtb.terminalIO.Canvas;
import dtb.terminalIO.Colors;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.examples.BarChartTest.Plotter.Plot;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;

public class BarChartTest {

    public static void main(String[] args) {
        // TODO Auto-generated method stub

        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(TestsCommon.getTerminalScreenForTesting(args));

        master_canvas.t("HELLO WORLD\nWhat can be\ngoing on here!?");

       // Composite master_component = new Composite();
        
        Composite textC = new Composite()
                                .setBounds(25, 50 )
                                .setTitle(new FGBGColors(Colors.Red, Colors.On_IYel), " Poem ")
                                .add(new TextComponent(null, "The rain in Spain\n" + 
                                                             "stays mainly on the plain\n" + 
                                                             "and where's that blasted plain?\n"+
                                                             "\nNow let's test obscured\n"+
                                                             "text."));
        
        Composite text2 = new Composite()
                .setBounds(25, 50 )
                .setTitle(new FGBGColors(Colors.Red, Colors.On_IYel), " Poem ")
                .add(new TextComponent(null, "In Spain .. In Spain\n" + 
                                             "The Rain in Spain stays \n" + 
                                             "mainly on the plain!"));
        
        master_canvas.addComponent(2, 30, textC);
        
        master_canvas.addComponent(7, 35, text2);
        
                                
        Composite box = new Composite().setPosition(5, 40).add(new BoundedComponent.DONOTHING(10,20)).setBorder(true);
        
        Plotter plotter = new Plotter(FGBGColors.DEFAULT,12,20)
                .addPlot(1,2)
                .addPlot(2, 2)
                .addPlot(3, 3)
                .addPlot(4, 6,'+', new FGBGColors(Colors.BCya))
                .addPlot(5, 7);
        
        Composite box2 = new Composite()
                          .setPosition(10, 45).add(plotter).setBorder(true);
                
        
        
        
        master_canvas.addComponent(0, 0, box);
        
        master_canvas.addComponent(5, 5, box2);
        
        
        master_canvas.refresh();
        
        
        TimerTask repeatedTask = new TimerTask() {
            public void run() {
                for (int i=0; i<=100; i++ ) {
                    doRandomPlots(plotter, plotter);
                    //plotter.clear();
                    box2.clear();
                    master_canvas.addComponent(5, 5, box2);
                    master_canvas.refresh();
                    TestsCommon.sleep(50);
                }
            }
        };
        Timer timer = new Timer("Timer");
         
        long delay  = 50L;
        long period = delay;
        timer.scheduleAtFixedRate(repeatedTask, delay, period);
        
        
        
//        for (int i=0; i<=100; i++ ) {
//            doRandomPlots(plotter, plotter);
//            //plotter.clear();
//            box2.clear();
//            master_canvas.addComponent(5, 5, box2);
//            master_canvas.refresh();
//            TestsCommon.sleep(50);
//        }
//     
        
        

    }
    
    static void doRandomPlots(Dimension dimension, Plotter plotter) {
        Plot[] result = new Plot[10];
        plotter.clearPlots();
        for (int i=0; i < result.length; i++) {
            int randomRow = ThreadLocalRandom.current().nextInt(0, dimension.getHeight());
            int randomCol = ThreadLocalRandom.current().nextInt(0, dimension.getWidth());
            plotter.addPlot(randomRow, randomCol,'*');
        }
    }
    
    
    // todo: 1) addPlot should reject (either silently or noisily) and plots
    //          outside of the bounds
    //       2) add draw line - using algorithm - maybe not in this class - but something that controls this class
    //       3) draw box
    //       4  draw circle
    
    static class Plotter extends BoundedComponent<Plotter> {

        
        char defaultPlotChar = '*';
        
        Collection<Plot> plots = new ArrayList<Plot>();
        
        public Plotter(FGBGColors color, Dimension d) {
            super(color, d);
        }

        public Plotter(FGBGColors color, int row, int height) {
            this(color, new Dimension.Simple(row, height));
        }

        public Plotter addPlot(int row, int col, char plotChar, FGBGColors color) {
            plots.add( new Plot(row, col, plotChar, color) );
            return this;
        }
        
        public Plotter addPlot(int row, int col, char plotChar) {
            return addPlot(row, col, plotChar, this.getColor());

        }
        public Plotter addPlot(int row, int col) {
            return addPlot(row, col, defaultPlotChar, this.getColor());
        }
        
        @Override
        protected void draw(FGBGColors color, IScreenLineSegmentsBuffer segments) {
            
            for (Plot plot : plots) {
                segments.setCharAt(plot.pos.getRow(), plot.pos.getCol(), plot.plotChar, plot.plotColor);
            }
        }
        
        public void clearPlots() {
            plots.clear();
        }
 

        static class Plot {
            Position pos;
            char plotChar;
            FGBGColors plotColor;
            public Plot(Position pos, char plotChar, FGBGColors plotColor) {
                super();
                this.pos = pos;
                this.plotChar = plotChar;
                this.plotColor = plotColor;
            }
            
            
            public Plot(int row, int col, char plotChar, FGBGColors plotColor) {
                this(new Position.Simple(row, col), plotChar, plotColor);
            }
            
        }
        
        
    }

}
