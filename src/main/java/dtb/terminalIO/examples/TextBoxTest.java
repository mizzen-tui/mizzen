package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.function.BiConsumer;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.ChildComponentManager;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ComponentInspector;
import dtb.terminalIO.components.ComponentWithFocusManager;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.CheckBoxInput;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.input.HasValue;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.components.scroll.SearchFunctionForScrollableComponent;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.OnIdleEventEventHandler.RepeatingInterval;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.examples.BarChartTest.BarChartDemo;
import dtb.terminalIO.examples.GridBrailleTest.BrailleDemo;
import dtb.terminalIO.examples.plot.Test1GamesSeparateThread;
import dtb.terminalIO.examples.plot.TestSprites;
import dtb.terminalIO.examples.sampledata.ReadSampleFiles;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.Logging;

/**
 * 
 * <pre>
 * 
 * Test textboxes and Focusmanagers and basic form handling
 * 
 * </pre>
 * 
 * @author dbalme
 *
 */
public class TextBoxTest {

    public static void main(String[] args) throws IOException {
        
        Logging.setEnabled(true);
        
        new TUIApp(args)
        .setEmptyChar(' ')
        .setMainApplicationPanel(new Composite().setBounds(TestsCommon.terminalSize()).setEmptyChar(' ').setTitle("MainPanel").setBorder(true))
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {
            
            Component<?> demo = addBackGround_Test1Games(tApp, args);
            
            
            VerticalPanel panel1 = new VerticalPanel();
            
            
            VerticalPanel panel2 = new VerticalPanel();
            panel2.setTitle("Panel 2");

            TextInput ti = new TextInput(10);

            panel1.add(ti);

            ti.setText("Test");

            TextInput ti_2 = new TextInput(15);

            ti_2.setText("Test 2");
            panel1.add(ti_2);
            

            TextInput ti_3 = new TextInput(15);
            ti_3.setText("Test3");
            panel2.add(ti_3);
            
            TextInput ti_4 = new TextInput(15);
            ti_4.setText("Test4");
            panel2.add(ti_4);
            
            CheckBoxInput cb_1 = new CheckBoxInput();
            cb_1.setValue(false);
            panel1.add(cb_1);
            
            Button b_1 = new Button("Press Me", true);
            panel1.add(b_1);

            Button b_2 = new Button("Press Too");
            panel1.add(b_2);

            String largeText;
            try {
                largeText = ReadSampleFiles.readSampleFile("LoremIpsumLarge.txt");
            } catch (IOException e) {
                largeText = e.toString();
            }  
            
            //panel1.add(  new TextComponent(largeText).setColor(Colors.Cya) );
            panel1.add(  new BorderedComponent(new TextComponent(largeText)).setColor(Colors.Cya) );
            
            Composite topPanel = new Composite();
            topPanel.setTitle("Form input testing");
            HorizontalPanel horizontalPanel = new HorizontalPanel();
            horizontalPanel.setHorizontalSpacer(2);

            topPanel.add(2,1, horizontalPanel);

            ScrollableComponent sc_1 = new ScrollableComponent(Colors.Blu, new Dimension.Simple(Math.min(tApp.getCanvas().getHeight()-12, panel1.getHeight()+2), 60), panel1);

            sc_1.setTitle("Scrollable with form");
            
            SearchFunctionForScrollableComponent.addFinderToScrollable(sc_1);
            horizontalPanel.add(sc_1);
            horizontalPanel.add( new ComponentWithFocusManager(panel2));

            TextComponent eventDisplayer = new TextComponent("                                       ");

            ButtonBar bannerMenu = new ButtonBar().setHorizontalSpacer(1);
            bannerMenu.setNoBorderChars();

            Button toggleTreeviewButton = new Button(Colors.BIGre,"");
            toggleTreeviewButton.setHotKey( Keys.CTRL_T_KEYSTROKE );
            toggleTreeviewButton.setDisplayHotKey(true);
            bannerMenu.addComponent(toggleTreeviewButton);
            
            bannerMenu.addComponent(new TextComponent(" switch Treeview Components/Focusables  "));

            Button refreshTreeviewButton = new Button(Colors.BIGre," ");
            refreshTreeviewButton.setHotKey(Keys.CTRL_R_KEYSTROKE);
            refreshTreeviewButton.setDisplayHotKey(true);
            bannerMenu.addComponent(refreshTreeviewButton);
            
            bannerMenu.addComponent(new TextComponent(" refresh Treeview"));
            
            DropdownListBox<String>  ddlbBackground =  DropdownListBox.fromStrings(new String[]{"A","B","C"});
            bannerMenu.addComponent(ddlbBackground);
            
            Button quitButton = new Button(Colors.BIGre,"Quit");
            quitButton.setHotKey(Keys.CTRL_C_KEYSTROKE);
            quitButton.setDisplayHotKey(true);
            quitButton.onChange(tApp.getTerminateAppHotKeyEventHandler());
            bannerMenu.addComponent(quitButton);

            VerticalPanel deskTopPanel = new VerticalPanel().setVerticalSpacer(1);
            deskTopPanel.setTitle("DeskTopPanel");

            deskTopPanel.add(bannerMenu);
            deskTopPanel.add(topPanel);

            deskTopPanel.add(new TextComponent("\n\n")); // cheap way to get a spacer
            deskTopPanel.add(eventDisplayer);

            tApp.getMainApplicationPanel().add(3, 5, deskTopPanel); 

            FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),tApp.getEventBus());


            tApp.getEventBus().appendEventHandler(  mainFocusManager );

            ComponentInspector componentInspector = new ComponentInspector(Colors.BIGre, new Dimension.Simple((int)(tApp.getMainApplicationPanel().getHeight()*.55),
                                                                                                              (int)(tApp.getMainApplicationPanel().getWidth()*.35)), 
                    (Component<?>)tApp.getMainApplicationPanel(), mainFocusManager);
            horizontalPanel.add(componentInspector);

            DefaultFocusManager.applyIfDefaultFocusManager(mainFocusManager, (dfalt_fm)->{
                dfalt_fm.setPrevKeyStroke(Keys.SHIFT_TAB_KEYSTROKE);
                dfalt_fm.setNextKeyStroke(Keys.TAB_KEYSTROKE);
                dfalt_fm.setNavKeysFirst(false);
                dfalt_fm.addFocusable(componentInspector);
            });

            refreshTreeviewButton.onChange( componentInspector.refreshEventHandler() );
            toggleTreeviewButton.onChange(componentInspector.toggleViewEventHandler());

            EventBus eventBus = tApp.getEventBus();

            ti.onChange((te)-> eventDisplayer.setText(ti.getValue()));

            cb_1.onChange((te)-> eventDisplayer.setText("checkbox: "+ String.valueOf(cb_1.getValue())));

            eventBus.appendEventHandler( new HasValue.OnValueChangedEventHandler<Boolean>(b_1) {
                @Override
                public void valueChanged(TerminalEvent<?> te,HasValue<Boolean> source) {
                    Boolean newValue = source.getValue();
                    eventDisplayer.setText("(1) Pressed = "+newValue);
                }
            });

            eventBus.appendEventHandler( new HasValue.OnValueChangedEventHandler<Boolean>(b_2) {
                public void handle(TerminalEvent<?> te) {
                    super.handle(te);
                    ti_4.requestFocus(te);
                }

                @Override
                public void valueChanged(TerminalEvent<?> te,HasValue<Boolean> source) {
                    Boolean newValue = source.getValue();
                    eventDisplayer.setText("(2) Pressed = "+newValue);
                }
            });

            tApp.getEventBus().appendIdleEventHandler(new RepeatingInterval(250, componentInspector.refreshEventHandler()));

            BiConsumer<TUIApp, Dimension> baseResizeAction = tApp.getResizeAction();

            tApp.setOnResizeAction( (ptApp, newSize) ->{
                baseResizeAction.accept(ptApp, newSize);
                int appHeight = tApp.getMainApplicationPanel().getHeight();
                int appWidth = tApp.getMainApplicationPanel().getWidth();

                componentInspector.resize( new Dimension.Simple((int)(appHeight * (.50)  ),
                                                                (int)(appWidth  * (.50) - panel2.getWidth() - 4 )));

                sc_1.resize( new Dimension.Simple( componentInspector.getHeight(), sc_1.getWidth() ) );

            } );

            ((Composite)tApp.getMainApplicationPanel()).bringToBack(demo);

            FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, (Component<?>)tApp.getMainApplicationPanel());

            // necessary?
            tApp.getScreen().add(ControlCode.SHOW_CURSOR);

        }).start();
    }
    
    static Component<?> addBackGround_Test1Games(TUIApp tApp, String args[]) {
        Component<?> grid = Test1GamesSeparateThread.addTestSpritesPanel(tApp, tApp.getMainApplicationPanel(), args);
        return grid;
    }
    
    static Component<?> addBackGround_TestSprites(TUIApp tApp, String args[]) {
        Component<?> grid = TestSprites.addTestSpritesPanel(tApp, tApp.getMainApplicationPanel());
        return grid;
    }
    
    static Component<?> addBackGround_BarChart(TUIApp tApp) {

        ChildComponentManager<?> barChartDemoContainer = tApp.getMainApplicationPanel();

        BarChartDemo barChartDemo = new BarChartDemo();
        Rectangle clippedAreaForBarchart = new Rectangle.Simple(Position.ZERO, new Dimension.Simple(barChartDemoContainer.getHeight()-2, barChartDemoContainer.getWidth()-2));

        barChartDemo.setUpDemo(tApp.getEventBus(), clippedAreaForBarchart);
        barChartDemoContainer.add(barChartDemo.getBarChart(clippedAreaForBarchart));

        BiConsumer<TUIApp, Dimension> defaultOnResizeAction = 
                tApp.getResizeAction()!=null ? tApp.getResizeAction() : (t,s)->{};

        tApp.setOnResizeAction((tApp2, newSize)->{
            // do default behaviour
            defaultOnResizeAction.accept(tApp2, newSize);
            barChartDemo.resize( newSize );
        });

        return barChartDemo.bc;
    }

    static Component<?> addBackGround_GridBrailleDemo(TUIApp tApp) {
        
        ChildComponentManager<?> gridBrailleDemoContainer = tApp.getMainApplicationPanel();
        
        BrailleDemo brailleDemo = new BrailleDemo();
        Rectangle clippedAreaForBarchart = new Rectangle.Simple(Position.ZERO, new Dimension.Simple(gridBrailleDemoContainer.getHeight()-2, gridBrailleDemoContainer.getWidth()-2));

        // this call must happen before brailleDemo.setUpDemo(..)
        brailleDemo.setupMovementKeys(Keys.CTRL_T_KEYSTROKE,    // up
                                      Keys.CTRL_V_KEYSTROKE,    // down
                                      Keys.CTRL_F_KEYSTROKE,    // left
                                      Keys.CTRL_G_KEYSTROKE);   // right

        brailleDemo.setUpDemo(tApp.getEventBus(), clippedAreaForBarchart);
        
        gridBrailleDemoContainer.add(brailleDemo.getBrailleComponent());
        
        BiConsumer<TUIApp, Dimension> defaultOnResizeAction = 
                tApp.getResizeAction()!=null ? tApp.getResizeAction() : (t,s)->{};

        tApp.setOnResizeAction((tApp2, newSize)->{
            // do default behaviour
            defaultOnResizeAction.accept(tApp2, newSize);
            brailleDemo.resize( newSize );
        });

        return brailleDemo.getBrailleComponent();
    }
    
    
}
