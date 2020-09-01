package dtb.terminalIO.components;

import java.util.function.Consumer;

import dtb.terminalIO.components.plot.Plotter;
import dtb.terminalIO.controlcodes.Colors;
import junit.framework.TestCase;

public class TestPlotterClearPreviousPlots extends TestCase {
    
    public void testPlot1() {
        Plotter plotter = new Plotter(Colors.BBla, 10,80);
        
        Consumer<Integer> renderLine = (col)-> {
            plotter.clear();
            plotter.clearPlots();
            plotter.addPlot(10, col+0);
            plotter.addPlot(10, col+1);
            plotter.addPlot(10, col+2);
            plotter.addPlot(10, col+3);
            plotter.markAsDirty();
        };
        
        renderLine.accept(10);
        
        int size1 = plotter.getSegmentsBuffer().getSegments().size();
        
        renderLine.accept(11);

        int size2 = plotter.getSegmentsBuffer().getSegments().size();
        
        assertEquals(size1, size2);
        
        
    }

}
