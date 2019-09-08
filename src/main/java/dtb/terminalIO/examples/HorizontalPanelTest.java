package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionVerticalRelative;

public class HorizontalPanelTest {

    
    public static void main(String[] args) throws IOException {

        
        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(TestsCommon.getTerminalScreenForTesting(args));

        Position anchorPoint = new Position.Simple(3,10);
        
        Composite hPanel1 = new HorizontalPanel();
        
        hPanel1.add(new BorderedComponent("In Spain .. In Spain\n" + 
                                        "The Rain in Spain stays \n" + 
                                        "mainly on the plain!")).t("Some Random Text");


       Button b1 = new Button("Hello Button");
       b1.setBorder(true);
       
       Button b2 = new Button("Button2");
       b2.setBorder(true);
       
       hPanel1.add(b1);
       
       hPanel1.add(b2);
                
       hPanel1.setEmptyChar(' ');
       
       master_canvas.addComponentSegments(anchorPoint, hPanel1.setTitle(" A Horizontal panel "));

       VerticalPanel vPanel1 = new VerticalPanel();
       
       vPanel1.setVerticalSpacer(1);
       
       vPanel1.add(new BorderedComponent("Silly Sally sold seashells\n" + 
                                         "At the sea shore")).t("I dont know why..").t("1-one\n2-two\n3-three\n!");

       vPanel1.setEmptyChar(' ');
       
       master_canvas.addComponentSegments( new PositionVerticalRelative(hPanel1).offsetAdd(anchorPoint).offsetAdd(1, 0), 
                                           new BorderedComponent(vPanel1).setTitle(" A Vertical Panel "));
       
       master_canvas.refresh();

    }
    
}
