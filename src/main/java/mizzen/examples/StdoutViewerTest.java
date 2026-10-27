package mizzen.examples;

import java.util.Collections;
import java.io.IOException;
import java.util.function.Consumer;

import mizzen.app.TUIApp;
import mizzen.components.BorderedComponent;
import mizzen.components.ButtonBar;
import mizzen.components.StdoutViewer;
import mizzen.components.TextComponent;
import mizzen.components.WindowFrame;
import mizzen.components.composite.Composite;
import mizzen.components.input.Button;
import mizzen.controlcodes.color.Colors;
import mizzen.controlcodes.color.Colors.FGBGColors;
import mizzen.events.OnIdleEventEventHandler;
import mizzen.events.OnTerminateEventHandler;
import mizzen.events.TerminalEvent;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.util.Logging;
import mizzen.util.StringSpinner;
import mizzen.util.linuxproc.LinuxProcessWatcher;
import mizzen.util.linuxproc.TerminalStreamOutputParser;

public class StdoutViewerTest {

    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);
        
        new TUIApp(args)
                .setCanvasSize(24, 100)
                .setEmptyChar(' ')
                .setDefaultMainApplicationPanel()
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

                    WindowFrame wf = new WindowFrame(Colors.Cya, "Test viewer", viewer);
                    wf.resize(new Dimension.Simple(18,52));

                    workArea.add(wf);
 
                    tApp.getMainApplicationPanel().add(1,0,workArea);
                    tApp.getMainApplicationPanel().add(1,workArea.getWidth(),statsArea);
 
                    // a file as it is now, read in on its own thread: the screen stays live while a big one streams in.
                    // (showFile, not follow: the large one is this app's own log, which grows while it is read.)
                    StdoutViewer.Feed[] reading = { null };
                    Consumer<String> populateTask = (fileName)-> {
                          if (reading[0] != null) {
                              reading[0].stop();
                          }
                          viewer.clearContent();
                          try {
                              reading[0] = viewer.showFile(fileName, tApp.getEventBus());
                          } catch (IOException e) {
                              viewer.follow(Collections.singletonList("Cannot read " + fileName + ": " + e.getMessage()), tApp.getEventBus());
                          }
                    };

                    ButtonBar bBar = new ButtonBar();

                    Button clearButton = new Button("Clear");
                    clearButton.onChange((te)->{
                        viewer.clearContent();
                    });

                    Button populateAgain = new Button("Medium content");
                    populateAgain.onChange((te)->{
                        populateTask.accept("pom.xml");
                    });

                    Button populateLarge = new Button("Large Content");
                    populateLarge.onChange((te)->{
                        populateTask.accept(Logging.getLogFile().toString());
                    });

                    Button populateSlowRunner = new Button("Slow Runnnert");
                    populateSlowRunner.onChange((te)->{
                        startSlowRunner(viewer, te);
                    });

                    bBar.addComponent(clearButton);
                    bBar.addComponent(populateAgain);
                    bBar.addComponent(populateLarge);
                    bBar.addComponent(populateSlowRunner);

                    tApp.getMainApplicationPanel().add(0,10, bBar);

                    // populateLarge.onClick(tApp.getEventBus());

                    populateAgain.onClick(tApp.getEventBus());

                    tApp.getEventBus().appendIdleEventHandler( new OnIdleEventEventHandler.OneTime((te)->{
                        populateTask.accept("pom.xml");
                    }) );
 
                    StringSpinner spinner = new StringSpinner(StringSpinner.SIMPLE_DOTS_SCROLLING);

                    tApp.getEventBus().appendIdleEventHandler( new OnIdleEventEventHandler.RepeatingInterval (500,(te)->{
                        statsData.setTextAndResize("STATS: " + Colors.BGre.asString()+spinner.spin() + statsData.getColor().asString() + "\n" + viewer.stats());
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

        String cmd = "scripts/slowRunner.sh";   // run from the project root

        LinuxProcessWatcher processWatcher = new LinuxProcessWatcher();

        viewer.clearContent();
        TerminalStreamOutputParser streamParser = new TerminalStreamOutputParser(viewer);

        processWatcher.setInvokeScript(cmd);

        OnTerminateEventHandler watcherCleanupHandler = new OnTerminateEventHandler(() -> processWatcher.stopRequest());

        mizzen.events.EventBus evtbus = te.getEventBus();

        evtbus.appendTerminateEventHandler(watcherCleanupHandler);

        viewer.enableTimedFlush(100, evtbus);

        try {
            processWatcher.invokeLongRunning(streamParser, 
                                                    (result)->{
                                                      streamParser.parseInput( Colors.BWhi.asString()+ "Done reading input : result code = "+ result+viewer.getColor().asString());
                                                      evtbus.removeEventHandler(watcherCleanupHandler);
                                                      },
                                             (e)->{  
                                                     evtbus.raiseEvent(viewer, TerminalEvent.TYPE.EXCEPTION , e);
                                                  }
                                           );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
