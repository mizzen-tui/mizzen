package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionVerticalRelative;
import dtb.terminalIO.util.Logging;

public class HorizontalPanelTest {

    
    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);
        
        Dimension terminalDimension = TestsCommon.terminalSize();
        
        new TUIApp(args)
        .setCanvasSize(terminalDimension.getHeight(), terminalDimension.getWidth())
        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {
            
            Position anchorPoint = new Position.Simple(3,10);

            Composite hPanel1 = new HorizontalPanel();

            hPanel1.add(new BorderedComponent("In Spain .. In Spain\n" + 
                                              "The Rain in Spain stays \n" + 
                                              "mainly on the plain!")).t("Some Random Text");


           Button b1 = new Button("Hello Button");
           b1.setBorder(true);
           hPanel1.add(b1);

           Button b_toggleDrawAlgorithm = new Button(Composite.DRAW_VIA_PARTS?"Toggle (Parts)":"Toggle (Traditional)");
           b_toggleDrawAlgorithm.onChange((te)->{
               Composite.DRAW_VIA_PARTS = !Composite.DRAW_VIA_PARTS;
               b_toggleDrawAlgorithm.setButtonText(Composite.DRAW_VIA_PARTS?"Toggle (Parts)":"Toggle (Traditional)");
               te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(b_toggleDrawAlgorithm));
           });
           b_toggleDrawAlgorithm.setBorder(true);
           hPanel1.add(b_toggleDrawAlgorithm);
           

           Button b3 = new Button("Button3");
           b3.setBorder(true);
           hPanel1.add(b3);

           hPanel1.setEmptyChar(' ');

           tApp.getMainApplicationPanel().add(anchorPoint, hPanel1.setTitle(" A Horizontal panel "));

           VerticalPanel vPanel1 = new VerticalPanel();

           vPanel1.setVerticalSpacer(1);

           vPanel1.add(new BorderedComponent("Silly Sally sold seashells\n" + 
                                             "At the sea shore")).t("I dont know why..").t("1-one\n2-two\n3-three\n!");

           vPanel1.setEmptyChar(' ');

           tApp.getMainApplicationPanel().add( new PositionVerticalRelative(hPanel1).offsetAdd(anchorPoint).offsetAdd(1, 0), 
                                               new BorderedComponent(vPanel1).setTitle(" A Vertical Panel "));

           HorizontalPanel hpButtons = new HorizontalPanel();
           hpButtons.setName("hp-SLOW");
           for (int i=0; i<20; i++) {
               Button b_i = new Button("B-"+i);
               b_i.setBorder(true);
               hpButtons.add(b_i);
           }
           
           tApp.getMainApplicationPanel().add( new PositionVerticalRelative(vPanel1).offsetAdd(anchorPoint).offsetAdd(1+vPanel1.getHeight(), 0), 
                                               new BorderedComponent(hpButtons).setTitle(" Lot'sa buttons Computed"));

           Composite hpButtons2 = new Composite();
           hpButtons2.setName("hp-FAST");
           for (int i=0; i<20; i++) {
               Button b_i = new Button("B-"+i);
               b_i.setBorder(true);
               hpButtons2.add(0, i*8, b_i);
           }

           tApp.getMainApplicationPanel().add( new PositionVerticalRelative(vPanel1).offsetAdd(anchorPoint).offsetAdd(1+vPanel1.getHeight()+hpButtons.getHeight()+3, 0), 
                                               new BorderedComponent(hpButtons2).setTitle(" Lot'sa buttons Not computed"));


        }).start();
        
        

    }
    
}
