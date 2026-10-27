package mizzen.examples;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Function;

import mizzen.app.TUIApp;
import mizzen.components.ButtonBar;
import mizzen.components.ChildComponentManager;
import mizzen.components.TextComponent;
import mizzen.components.chart.SparkLineComponent;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.controlcodes.color.Colors;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.OnIdleEventEventHandler;
import mizzen.events.keyboard.AsKeyboardHandler;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyInputEventHandler;
import mizzen.events.keyboard.Keys;
import mizzen.geom.Dimension;

public class SparklineTest {
    
    public static void main(String[] args) throws IOException {

        new TUIApp(args)
                .setCanvasSize(24, 100)
                .setEmptyChar(' ')
                .setDefaultMainApplicationPanel()
                .confirmOnExit()
                .setOnReadyStart((tApp)-> {
                    
                    buildSparklineTest(tApp.getEventBus(), tApp.getMainApplicationPanel(), tApp.getTerminateAppHotKeyEventHandler());

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
        EventHandler increase = (te) -> {
            if (sparkLine1.getHeight()<20) {
                sparkLine1.resize( new Dimension.Simple(sparkLine1.getHeight()+1, sparkLine1.getWidth()) );
                sparkLine1Label.setText("Sparkline : \nheight="+hiliteSparklineHeight.apply(sparkLine1.getHeight()));
            }
        };
        EventHandler decrease = (te) -> {
            if (sparkLine1.getHeight()>1) {
                sparkLine1.resize( new Dimension.Simple(sparkLine1.getHeight()-1, sparkLine1.getWidth()) );
                sparkLine1Label.setText("Sparkline : \nheight="+ hiliteSparklineHeight.apply(sparkLine1.getHeight()));
            }
        };
        bannerPanel.addButton(Colors.IGre,"UP increase", null, increase);
        bannerPanel.addComponent(new TextComponent("/"));
        bannerPanel.addButton(Colors.IGre,"DOWN decrease", null, decrease);
        // UP and DOWN only while focus is in this bar - as hot keys they would take the arrows
        // from every other component on the screen (e.g. a text box in another window)
        bannerPanel.focus().handleKeys(AsKeyboardHandler.UTIL.toKeyboardHandler(new KeyInputEventHandler(
                (te) -> (Keys.UP_KEYSTROKE.equals(((KeyEvent) te).getKeyStroke()) ? increase : decrease).handle(te),
                Keys.UP_KEYSTROKE, Keys.DOWN_KEYSTROKE)));
        bannerPanel.addComponent(new TextComponent(" sparkline height"));
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

            
        } ));
        
    }
    

}
