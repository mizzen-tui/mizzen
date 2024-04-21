package dtb.terminalIO.examples;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Consumer;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.StdoutViewer;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.WindowFrame;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.controlcodes.color.Colors.FGBGColors;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.OnTerminateEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.StringSpinner;
import dtb.terminalIO.util.linuxproc.LinuxProcessWatcher;
import dtb.terminalIO.util.linuxproc.TerminalStreamOutputParser;
import dtb.terminalIO.util.linuxproc.TerminalStreamReader;

public class StdoutViewerTest {

    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);
        
        new TUIApp(args)
                .setCanvasSize(24, 100)
                .setEmptyChar(' ')
                .setMainApplicationPanel(new Composite().setBounds(20, 100).setEmptyChar(' ').setBorder(true))
                .confirmOnExit()
                .setOnReadyStart((tApp)-> {

                    StdoutViewer viewer = new StdoutViewer(Colors.BGre);

                    Composite workArea = new Composite().setBorder(true);
                    Dimension workAreaSize = new Dimension.SupplierAsDimension(() -> {
                                                    return new Dimension.Simple(tApp.getMainApplicationPanel().getHeight()-4,
                                                                                ((int)(tApp.getMainApplicationPanel().getWidth()*.6)));
                                                    });

                    workArea.resize(workAreaSize);
                    workArea.setFillOnClear(true);
                    workArea.setEmptyChar('░');
                    workArea.setEmptyColor(Colors.IBlu);
                    
                    viewer.setEmptyChar('.');
                    viewer.setEmptyColor(Colors.IYel);
                    viewer.setFillOnClear(false);
                    
                    Composite statsArea = new Composite().setBorder(false);
                    Dimension statsAreaSize  = new Dimension.SupplierAsDimension(() -> new Dimension.Simple(workArea.getHeight(), 
                                                                                                            tApp.getMainApplicationPanel().getWidth()-workArea.getWidth()-2) );
                    statsArea.resize(statsAreaSize);
                    statsArea.setFillOnClear(true);
                    statsArea.setEmptyChar('.');
                    statsArea.setEmptyColor(new FGBGColors(Colors.BIBla, Colors.On_Bla));
                    
                    TextComponent statsData = new TextComponent("Stats ..");
                    
                    statsArea.add(0, 1,  new BorderedComponent(statsData));
                    

                    //ScrollableComponent wf = new ScrollableComponent(Colors.Cya, new Dimension.Simple(18,52), viewer);
                    
                    WindowFrame wf = new WindowFrame(Colors.Cya, "Test viewer", viewer);
                    wf.resize(new Dimension.Simple(18,52));
                    
                    workArea.add(wf);
                    
                    tApp.getMainApplicationPanel().add(1,0,workArea);
                    tApp.getMainApplicationPanel().add(1,workArea.getWidth(),statsArea);
                    
                    
                    Consumer<String> populateTask = (fileName)-> {
                          viewer.clearContent();
                          viewer.disableTimedFlush(tApp.getEventBus());
                          TerminalStreamOutputParser streamParser = new TerminalStreamOutputParser(viewer);
                          
                          try {
                              new TerminalStreamReader(streamParser).readAll(new FileInputStream(fileName));
                          } catch (FileNotFoundException e) {
                              // TODO Auto-generated catch block
                              e.printStackTrace();
                          }
                    };

                    ButtonBar bBar = new ButtonBar();
                    
                    Button clearButton = new Button("Clear");
                    clearButton.onChange((te)->{
                        viewer.clearContent();
                    });

                    Button populateAgain = new Button("Medium content");
                    populateAgain.onChange((te)->{
                        populateTask.accept("/home/dbalme/workspace/dtbTerminalIO/makeACME_lib_version.sh");
                    });

                    Button populateLarge = new Button("Large Content");
                    populateLarge.onChange((te)->{
                        populateTask.accept("/home/dbalme/workspace/dtbTerminalIO/dtbTerminal.log");
                    });
                    
                    Button populateMvnContent = new Button("MVN Content");
                    populateMvnContent.onChange((te)->{
                        populateTask.accept("/home/dbalme/workspace/LabManager/mvnOutput.txt");
                    });
                    
                    Button populateSlowRunner = new Button("Slow Runnnert");
                    populateSlowRunner.onChange((te)->{
                        startSlowRunner(viewer, te);
                    });
                    
                    
                    bBar.addComponent(clearButton);
                    bBar.addComponent(populateAgain);
                    bBar.addComponent(populateLarge);
                    bBar.addComponent(populateMvnContent);
                    bBar.addComponent(populateSlowRunner);
                    
                    tApp.getMainApplicationPanel().add(0,10, bBar);
                    
                    FocusManager fm = FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(), 
                                                                                       tApp.getEventBus(),
                                                                                       ()-> new DefaultFocusManager().setPrevKeyStroke(Keys.SHIFT_TAB_KEYSTROKE)
                                                                                                                     .setNextKeyStroke(Keys.TAB_KEYSTROKE)
                                                                                                                     .setNavKeysFirst(false));
                    
                    tApp.getEventBus().appendEventHandler(fm);
                    
                    // populateLarge.onClick(tApp.getEventBus());
                    
                    populateAgain.onClick(tApp.getEventBus());
                    
                    
                    tApp.getEventBus().appendIdleEventHandler( new OnIdleEventEventHandler.OneTime((te)->{
                        
                        
                        populateTask.accept("/home/dbalme/workspace/dtbTerminalIO/makeACME_lib_version.sh");
                        
                        
                    }) );
 
                    StringSpinner spinner = new StringSpinner(StringSpinner.SIMPLE_DOTS_SCROLLING);
                    
                    tApp.getEventBus().appendIdleEventHandler( new OnIdleEventEventHandler.RepeatingInterval (500,(te)->{
                        statsData.setTextAndResize("STATS: " + Colors.BGre.asString()+spinner.spin() + statsData.getColor().asString() + "\n" + viewer.stats());
                        te.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(statsData) );
                    }) );
                    
                    
                    
                    tApp.appendOnResizeAction((app, size) -> {
                        workArea.resize(workAreaSize);
                        statsArea.resize(statsAreaSize);
                        tApp.getMainApplicationPanel().moveChildToPosition(new Position.Simple(1,workArea.getWidth()), statsArea);
                    });
                    
                    
                    // FocusManager.UTIL.verifyPathsToFocusables(fm, (Component<?>) tApp.getMainApplicationPanel());

                }).start();
    }
    
    
    static void startSlowRunner(StdoutViewer viewer, TerminalEvent<?> te) {
        
        String cmd = "/home/dbalme/workspace/dtbTerminalIO/slowRunner.sh";
        
        LinuxProcessWatcher processWatcher = new LinuxProcessWatcher();
        
        viewer.clearContent();
        TerminalStreamOutputParser streamParser = new TerminalStreamOutputParser(viewer);
        
        processWatcher.setInvokeScript(cmd);
        
        OnTerminateEventHandler watcherCleanupHandler = new OnTerminateEventHandler(() -> processWatcher.stopRequest());
        
        
        dtb.terminalIO.events.EventBus evtbus = te.getEventBus();
        
        evtbus.appendTerminateEventHandler(watcherCleanupHandler);

        viewer.enableTimedFlush(100, evtbus);
        
        
        try {
            processWatcher.invokeLongRunning(streamParser, 
                                                    (result)->{
                                                      streamParser.parseInput( Colors.BWhi.asString()+ "Done reading input : result code = "+ result+viewer.getColor().asString());
                                                      evtbus.raiseEvent( new RequestUpdateCanvasEvent(viewer));
                                                      evtbus.removeEventHandler(watcherCleanupHandler);
                                                      },
                                             (e)->{  
                                                     evtbus.raiseEvent(viewer, TerminalEvent.TYPE.EXCEPTION , e);
                                                  }
                                           );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        evtbus.raiseEvent( new RequestUpdateCanvasEvent(viewer));
        
        
    }
    
    
}
