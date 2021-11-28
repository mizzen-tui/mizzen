package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.AbstractComponent;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ComponentInspector;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.WindowFrame;
import dtb.terminalIO.components.WindowFrameFocusable;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.ListBox;
import dtb.terminalIO.components.input.MultiLineTextInput;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.controlcodes.color.Colors.FGBGColors;
import dtb.terminalIO.events.OnIdleEventEventHandler.RepeatingInterval;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.geom.Align;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.VAlign;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionRelativeFactory;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.TerminalDimensions;

public class WindowFrameTest {
    
    
    static boolean useFocusable = false;
    
    public static void main(String[] args) throws IOException {

        if (args.length>0) {
            if (args[0].equalsIgnoreCase("FOCUS")) {
                useFocusable = true;
            }
        }
        
        Logging.setEnabled(true);

        new TUIApp(args)
        //.setCanvasSize(24, 100)
        .setEmptyChar(' ')
        .setMainApplicationPanel(new Composite().setBounds(40, 130).setEmptyChar('X').setBorder(false))
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {

            MultiLineTextInput mTextBox = new MultiLineTextInput(); 
            mTextBox.setText("Silly Sally sold\nSeaShells at the seashore\nWhat a silly thing to do!");

            mTextBox.setColor(  new Colors.FGBGColors( Colors.BBlu, Colors.On_Bla  )  );

      //      mTextBox.resize(new Dimension.Simple(20,50));

            AbstractComponent<?> wf1 = makeWF(Colors.Whi, "Test Window1", mTextBox);
            wf1.setEmptyColor(Colors.On_IBla);

            AbstractComponent<?> wf2 = makeWF(Colors.Whi, "Test Window2", ListBox.fromStrings(new String[] {"Silly Sally",
                                                                                                            "Sold sea shells",
                                                                                                            "at the sea shore"}));
            wf2.setEmptyColor(Colors.On_Bla);

            /*
            Composite sparkLineContainer = new Composite().setBorder(false);
            sparkLineContainer.resize(new Dimension.Simple(24,100));
            SparklineTest.buildSparklineTest(tApp.getEventBus(), sparkLineContainer, null);
            WindowFrame wfSparkline = new WindowFrame(Colors.Blu, "SparkLine Demo", sparkLineContainer);

*/
            Composite windowContainer = new Composite();

            Component<?> wfFruit = makeWF(Colors.Whi, "Fruit", ListBox.fromStrings(new String[] {"Apples",
                                                                                                         "Pears",
                                                                                                         "Plums"}));
            
            Component<?> wfVegatables = makeWF(Colors.Whi, "Fruit", ListBox.fromStrings(new String[] {"Tomotoes",
                                                                                                             "Lettuce",
                                                                                                             "Cucumber",
                                                                                                             "Kale",
                                                                                                             "Potatoes",
                                                                                                             "Carrots",
                                                                                                             "Peppers",
                                                                                                             "Cabbage",
                                                                                                             "Olives",
                                                                                                             "Celery",
                                                                                                             "Onions"}));

            windowContainer.add(4, 10, wfFruit);
            windowContainer.add(8, 14, wfVegatables);

            Component<?> nestedWindows = makeWF(Colors.Whi, "Nested Windows", windowContainer);

            tApp.getMainApplicationPanel().add(2, 0, wf1);
            tApp.getMainApplicationPanel().add(2, 30, wf2);
            tApp.getMainApplicationPanel().setDropShadow(true, wf2);
           // tApp.getMainApplicationPanel().add(10, 30, wfSparkline);
            tApp.getMainApplicationPanel().add(20, 40, nestedWindows);

            Composite expandDownContainer = new Composite() {
                public void fullrecalcDim() {
                    super.fullrecalcDim();
                }
            };

            expandDownContainer.setOnResize( (size) ->{}  );
            Button redButton = new Button(new FGBGColors(Colors.BWhi, Colors.On_Blu),"Top widget");
            redButton.onChange((te)->{
                if (redButton.getButtonText().equals("Top widget")) {
                    redButton.setButtonText("Top --\nWidget");
                } else {
                    redButton.setButtonText("Top widget");
                }
                te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(redButton));
            });
            BorderedComponent blue = new BorderedComponent(redButton);
            expandDownContainer.add(0,0,blue);
            expandDownContainer.onResizeExpandRight(blue);

            BorderedComponent red = new BorderedComponent(new TextComponent(new FGBGColors(Colors.BWhi, Colors.On_Red),"Bottom widget"));
            //Position redPosition = new PositionVerticalRelative(blue);
            expandDownContainer.addBelow(red, blue);
            expandDownContainer.onResizeExpandDown(red);
            expandDownContainer.onResizeExpandRight(red, 30);

            BorderedComponent green = new BorderedComponent(new TextComponent(new FGBGColors(Colors.BWhi, Colors.On_Gre),"Bottom Right"));
            expandDownContainer.addRight(green, red);
            expandDownContainer.onResizeExpandDownAndRight(green);

//            CanResize resizing = new CanResize.Chain( new ExpandRight(expandDownContainer, blue), new ExpandDown(expandDownContainer, redPosition,  red));
//            expandDownContainer.setOnResize(resizing.asOnResizeService());

            AbstractComponent<?> expandDownWindows = makeWF(Colors.Whi, "Expand down tests", expandDownContainer);

            tApp.getMainApplicationPanel().add(13, 65, expandDownWindows);


            DefaultFocusManager mainFocusManager = (DefaultFocusManager)FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),
                    tApp.getEventBus(),
                    ()-> new DefaultFocusManager().setNavKeysFirst(false).setCycling(false).setName("MainApplicationPanel-FocusManager"));

            tApp.getEventBus().appendEventHandler( mainFocusManager );

            ComponentInspector inspector = new ComponentInspector(Colors.BIGre, 
                                                                  new Dimension.Simple(25,60), 
                                                                  (Component<?>)tApp.getMainApplicationPanel(), 
                                                                  mainFocusManager);

            AbstractComponent<?> wfInspector = makeWF(inspector.getColor(),"Components",inspector);

            mainFocusManager.addComponentAsFocusables(wfInspector);

            tApp.getEventBus().appendEventHandler( inspector.toggleViewEventHandler() );
            tApp.getEventBus().appendEventHandler( inspector.refreshEventHandler() );

            HorizontalPanel statsPanel = new HorizontalPanel();

            statsPanel.add( new TextComponent("Current focus:"));
            TextComponent stats = new TextComponent("None");
            statsPanel.add(stats);
            stats.setColor(Colors.IYel);

            Position statsPosition = PositionRelativeFactory.fromAlign(VAlign.BOTTOM, Align.LEFT,  tApp.getMainApplicationPanel(), statsPanel);
            tApp.getMainApplicationPanel().add(statsPosition, statsPanel);

            // every 1/4 second we update ComponentInspector with current component hierarchy and focus data
            tApp.getEventBus().appendIdleEventHandler(new RepeatingInterval(2000, (te)->{
                                                         inspector.refreshEventHandler().handle(te);
                                                         Object currentFocus = FocusManager.UTIL.findCurrentFocus(mainFocusManager);
                                                         if (currentFocus == null) {
                                                             return;
                                                         }
                                                         stats.setTextAndResize(HasName.UTIL.getName(currentFocus));
                                                      }));


            tApp.getMainApplicationPanel().add(22,44, wfInspector);

            tApp.getMainApplicationPanel().add(0,0,new TextComponent("WindowFrame class: " + usedWindoManagerClass().getSimpleName()));


            FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, (Component<?>)tApp.getMainApplicationPanel());

            tApp.getResizeAction().accept(tApp, TerminalDimensions.create());

        }).start();

    }

    static AbstractComponent<?> makeWF(Color color, String title, Component<?> child) {
        if (useFocusable) {
            AbstractComponent<?> component = new WindowFrameFocusable(color, title, child);
            return component;
        } else {
            AbstractComponent<?> component = new WindowFrame(color, title, child);
            return component;
        }
    }

    static Class<?>  usedWindoManagerClass() {
        return makeWF(Colors.DEFAULT_COLOR, "dummy", new TextComponent(" ")).getClass();
    }



}
