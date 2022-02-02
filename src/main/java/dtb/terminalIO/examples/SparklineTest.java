package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Function;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.ChildComponentManager;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.chart.SparkLineComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;

public class SparklineTest {
    
    public static void main(String[] args) throws IOException {

        new TUIApp(args)
                .setCanvasSize(24, 100)
                .setEmptyChar(' ')
                .setMainApplicationPanel(new Composite().setBounds(20, 100).setEmptyChar(' ').setBorder(true))
                .confirmOnExit()
                .setOnReadyStart((tApp)-> {
                    
                    buildSparklineTest(tApp.getEventBus(), tApp.getMainApplicationPanel(), tApp.getTerminateAppHotKeyEventHandler());
                    
                    // traverse main app panel find focusable's and build a focusManager and attach to Eventbus
                    FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),tApp.getEventBus());
                    
                    tApp.getEventBus().appendEventHandler(  mainFocusManager );

                    // sanity check
                    FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, (Component<?>)tApp.getMainApplicationPanel());

                }).start();
    }
    
    public static void buildSparklineTest(EventBus eventBus, ChildComponentManager<?> mainPanel, KeyInputEventHandler terminateAppHotKeyEventHandler) {

        Function<Integer,String> hiliteSparklineHeight = (height)-> ((height==1||height==20)? Colors.IRed.asString() : Colors.IGre.asString()) + height + Colors.RCol.asString();

        SparkLineComponent sparkLine1 = new SparkLineComponent(Colors.BCya);
        SparkLineComponent sparkLine2 = new SparkLineComponent(Colors.BIGre);
        TextComponent sparkLine1Label = new TextComponent("Sparkline : \nheight="+hiliteSparklineHeight.apply(sparkLine1.getHeight()));
        TextComponent sparkLine2Label = new TextComponent("Sparkline : \nheight="+sparkLine2.getHeight());
        
        ButtonBar bannerPanel = new ButtonBar().setHorizontalSpacer(0);
        bannerPanel.setBorder(false);
        bannerPanel.setNoBorderChars();
        bannerPanel.addComponent(new TextComponent("Demonstrate Sparkline Component:    "));
        if (terminateAppHotKeyEventHandler != null) {
            bannerPanel.addButton(Colors.IGre,"CTRL-C",Keys.CTRL_C_KEYSTROKE, (te) -> terminateAppHotKeyEventHandler.handle(te)  );
            bannerPanel.addComponent(new TextComponent(" to quit "));
        }
        bannerPanel.addButton(Colors.IGre,"UP", Keys.UP_KEYSTROKE, (te)->{
            if (sparkLine1.getHeight()<20) {
                sparkLine1.resize( new Dimension.Simple(sparkLine1.getHeight()+1, sparkLine1.getWidth()) );
                sparkLine1Label.setText("Sparkline : \nheight="+hiliteSparklineHeight.apply(sparkLine1.getHeight()));
            }
        } ); 
        // need to bind the events below - chicken and egg situation ( forward declaration issue with sparkline1)
        bannerPanel.addComponent(new TextComponent("/"));
        bannerPanel.addButton(Colors.IGre,"DOWN", Keys.DOWN_KEYSTROKE, (te)->{
            if (sparkLine1.getHeight()>1) {
                sparkLine1.resize( new Dimension.Simple(sparkLine1.getHeight()-1, sparkLine1.getWidth()) );
                sparkLine1Label.setText("Sparkline : \nheight="+ hiliteSparklineHeight.apply(sparkLine1.getHeight()));
            }
        });
        bannerPanel.addComponent(new TextComponent(" increase/decrease sparkline height"));
        mainPanel.add(1, 1,bannerPanel);

        sparkLine1.resize(new Dimension.Simple(5, 40));
        sparkLine2.resize(new Dimension.Simple(1, 40));

        VerticalPanel vp = new VerticalPanel();
        vp.setName("vpSparkLineTest");
        TextComponent dataPoints = new TextComponent(" .. ");

        vp.add(new HorizontalPanel().add(new TextComponent("Data      : ")).add(dataPoints));
        vp.add(new HorizontalPanel().add(sparkLine1Label).add(sparkLine1));
        vp.add(new HorizontalPanel().add(sparkLine2Label).add(sparkLine2));
        vp.setVerticalSpacer(1);
        
        
        mainPanel.add(3, 1, vp);
        
        Random rint = new Random();
        
        
        eventBus.appendEventHandler( new OnIdleEventEventHandler.RepeatingInterval(100, (te)-> {
            
            Integer[] data = new Integer[30];

            for (int i = 0; i < data.length; i++) {
                data[i] = rint.nextInt(100);
            }

            sparkLine1.renderPoints(data);
            sparkLine2.renderPoints(data);

            dataPoints.setTextAndResize( Arrays.deepToString(data) );

            te.getEventBus().raiseEvent(  new RequestUpdateCanvasEvent(mainPanel)  );
            
        } ));
        
    }
    

}
