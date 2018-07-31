package dtb.terminalIO.examples;

import dtb.terminalIO.Canvas;
import dtb.terminalIO.Colors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.ScreenLineSegment;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.RectangleComponent;
import dtb.terminalIO.components.SegmentComponent;

public class BorderedComponentTest {

    
    public static void main(String[] args) {
        // TODO Auto-generated method stub

        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(TestsCommon.getTerminalScreenForTesting(args));
        master_canvas.setEmptyChar('~');
        
//        Component<?> componentBox1 = new BorderedComponent((BoundedComponent<?>) new DONOTHING(10,30))
//                .setTitle(new FGBGColors(Colors.Blu, Colors.On_IYel), "DONOTHING Test");
//
//        master_canvas.addComponent(5, 10, componentBox1);

        int[] count = new int[] {0,0};
        
        
        
        RectangleComponent r1 = new RectangleComponent(new FGBGColors(Colors.Yel),10, 30) {
            @Override
            protected void draw(FGBGColors color, IScreenLineSegmentsBuffer segments) {
                count[0] ++;
                super.draw(color, segments);
                count[1] ++;
            }
            
        };
        r1.setFillOnClear(true);
        r1.setEmptyChar('*');
        r1.setEmptyColor(new FGBGColors(Colors.BYel, Colors.On_Cya));
        //r1.setColor(new FGBGColors(Colors.BPur,Colors.On_Yel));
        
        BorderedComponent componentBox2 = new BorderedComponent(new FGBGColors(Colors.IBlu, Colors.On_Gre),(BoundedComponent<?>) r1);
        componentBox2.setColor(new FGBGColors(Colors.IBlu));
                //.setTitle(new FGBGColors(Colors.Blu, Colors.On_IYel), "Rectangle Test");

        StringBuffer sbDEBUG = new StringBuffer();
        sbDEBUG.append("-------\nDEBUG LOG\n");
        
        sbDEBUG.append("canvas textcolor = ").append(master_canvas.getTextColor()).append("\n");
        sbDEBUG.append("canvas emptycolor = ").append(master_canvas.getEmptyColor()).append("\n");
        
        
        master_canvas.addComponent(5, 50, componentBox2);

        
        master_canvas.refresh();
        sbDEBUG.append("\nAFTER REFRESH\ncanvas textcolor = ").append(master_canvas.getTextColor()).append("\n");
        sbDEBUG.append("canvas emptycolor = ").append(master_canvas.getEmptyColor()).append("\n");
        
        
        System.out.println("count = "+count[0]);
        
        System.out.println("r1            = " + r1);
        System.out.println("componentBox2 = " + componentBox2);
        
        
//        int counter = 0;
//        sbDEBUG.append("\nCanvas contents\n");
//        for (ScreenLineSegment scs : master_canvas.getSegmentsBuffer().getSegments()) {
//            if (scs.getText().equals("│")) {
//                counter++;
//                sbDEBUG.append(String.format("%2d %40s ",counter,"color=" +scs.getColors())).append("\t--> ").append(scs).append("\n");
//            }
//        }
        
        System.out.println(sbDEBUG);

    }
    
    
}
