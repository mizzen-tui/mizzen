package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.PositionVerticalRelative;

public class HorizontalPanelTest {

    
    public static void main(String[] args) throws IOException {

        
        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(TestsCommon.getTerminalScreenForTesting(args));

        
        Position anchorPoint = new Position.Simple(3,10);
        
        Composite bbar1 = new HorizontalPanel();
        
                bbar1.add(new BorderedComponent("In Spain .. In Spain\n" + 
                                             "The Rain in Spain stays \n" + 
                                             "mainly on the plain!")).t("Texxxxxt"); //.t("1-one\n2-two\n3-three\n!");


                
       Button b1 = new Button("Hello Button");
       b1.setBorder(true);
       
       Button b2 = new Button("Button2");
       b2.setBorder(true);
       
       bbar1.add(b1);
       
       bbar1.add(b2);
                
       bbar1.setEmptyChar(' ');
       

       master_canvas.addComponentSegments(anchorPoint, bbar1);
        

       Composite bbar2 = new VerticalPanel();
       
       bbar2.add(new BorderedComponent("In Spain .. In Spain\n" + 
                                    "The Rain in Spain stays \n" + 
                                    "mainly on the plain!")).t("Texxxxxt"); //.t("1-one\n2-two\n3-three\n!");

       
       bbar2.add(b1);
       
       bbar2.add(b2);
                
       bbar2.setEmptyChar(' ');
       
       master_canvas.addComponentSegments( new PositionVerticalRelative( bbar1).offsetAdd(anchorPoint), bbar2);
       
       
       master_canvas.refresh();

        
        
    }
    
    
    
}
