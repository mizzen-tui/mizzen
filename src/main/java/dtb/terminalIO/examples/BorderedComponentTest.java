package dtb.terminalIO.examples;

import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.RectangleComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

public class BorderedComponentTest {


    public static void main(String[] args) {

        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(TestsCommon.getTerminalScreenForTesting(args));
        master_canvas.setEmptyChar('~');

        int[] count = new int[] {0,0};

        RectangleComponent r1 = new RectangleComponent(new Colors.FGBGColors(Colors.Yel),10, 30) {
            @Override
            protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
                count[0] ++;
                super.draw(color, segments);
                count[1] ++;
            }
        };
        r1.setFillOnClear(true);
        r1.setEmptyChar('*');
        r1.setEmptyColor(new Colors.FGBGColors(Colors.BYel, Colors.On_Cya));
        
        BorderedComponent componentBox2 = new BorderedComponent(new Colors.FGBGColors(Colors.IBlu, Colors.On_Gre),(BoundedComponent<?>) r1);
        componentBox2.setColor(new Colors.FGBGColors(Colors.IBlu));
                //.setTitle(new FGBGColors(Colors.Blu, Colors.On_IYel), "Rectangle Test");

        StringBuffer sbDEBUG = new StringBuffer();
        sbDEBUG.append("-------\nDEBUG LOG\n");

        sbDEBUG.append("canvas textcolor = ").append(master_canvas.getTextColor()).append("\n");
        sbDEBUG.append("canvas emptycolor = ").append(master_canvas.getEmptyColor()).append("\n");

        master_canvas.addComponentSegments(5, 50, componentBox2);

        master_canvas.refresh();
        sbDEBUG.append("\nAFTER REFRESH\ncanvas textcolor = ").append(master_canvas.getTextColor()).append("\n");
        sbDEBUG.append("canvas emptycolor = ").append(master_canvas.getEmptyColor()).append("\n");


        System.out.println("count = "+count[0]);

        System.out.println("r1            = " + r1);
        System.out.println("componentBox2 = " + componentBox2);


        System.out.println(sbDEBUG);

    }


}
