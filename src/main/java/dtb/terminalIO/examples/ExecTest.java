package dtb.terminalIO.examples;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.LinuxProcessViewer;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.ElapsedTime;
import dtb.terminalIO.util.linuxproc.ExecUtil;
import dtb.terminalIO.util.linuxproc.LinuxProcessWatcher;

/**
 * Demo's running child processes and displaying output in ScrollableComponents
 * @author dbalme
 *
 */
public class ExecTest {

    static String execTop() {
        try {
            return ExecUtil.executeWithShell("top -n 1 -b");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return "EXCEPTION:  " + e;
        }
    }

    static boolean simple = true;

    private static String fileNameToTail(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("file".equalsIgnoreCase(args[i])) {
                return args[i + 1];
            }
        }
        return null;
    }

    private static class TimeAndCount {

        int count;
        ElapsedTime et;

        public static TimeAndCount onRun(Runnable r, Supplier<Integer> s) {
            TimeAndCount tR = new TimeAndCount();
            tR.et = ElapsedTime.onRunOf(r);
            tR.count = s.get();
            return tR;
        }

        public static TimeAndCount onRun(Runnable r, IScreenLineSegmentsBuffer.Provider buffer) {
            return onRun(r, () -> buffer.getSegmentsBuffer().getSegments().size());
        }

        public static TimeAndCount onRun(IScreenLineSegmentsBuffer.Provider buffer) {
            return onRun(() -> buffer.getSegmentsBuffer().getSegments(),
                    () -> buffer.getSegmentsBuffer().getSegments().size());
        }

        public String toString() {
            return String.format("%10s  : %5d  %2.10f", et.toString(), count, ((float) et.elapsed() / count));
        }
    }

    private static String fileNameToView(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("view".equalsIgnoreCase(args[i])) {
                return args[i + 1];
            }
        }
        return null;
    }
    
    public static void slowRunnerTest2(String[] args) throws Exception {
        
        TUIApp app = new TUIApp();
        app.initFromArgs(args).setDefaultMainApplicationPanel();;
        
        app.getCanvas().t("testing LinuxProcessViewer ..\n");

        Dimension sizeOfWindow = new Dimension() {
            @Override
            public int getWidth() {
                return app.getCanvas().getWidth() / 2 - 6;
            }

            @Override
            public int getHeight() {
                return app.getCanvas().getHeight() - 17;
            }
        };

        Dimension sizeOfPanel = new Dimension() {
            @Override
            public int getWidth() {
                return sizeOfWindow.getWidth() * 2 + 6+4;
            }

            @Override
            public int getHeight() {
                return sizeOfWindow.getHeight()  + 8;
            }
        };

        Position view1OffsetOnCanvas = new Position.Simple(2, 1);

        LinuxProcessViewer lprocViewer = new LinuxProcessViewer(sizeOfWindow);
        lprocViewer.setBorderColor(Colors.Gre);
        lprocViewer.addFinder();

        LinuxProcessViewer lprocViewer2 = new LinuxProcessViewer(sizeOfWindow);
        lprocViewer2.setBorderColor(Colors.Blu);
        lprocViewer2.addFinder();

        HorizontalPanel hPanel = new HorizontalPanel();
        hPanel.setBounds(sizeOfPanel);

        hPanel.setHorizontalSpacer(6);

        hPanel.add(lprocViewer);
        hPanel.add(lprocViewer2);

        VerticalPanel mainPanel = new VerticalPanel();

        mainPanel.setVerticalSpacer(1);

        mainPanel.add(new TextComponent("CTRL-C to quit:     Demo's running a child-process and viewing output in Scrollable windows"));

        mainPanel.add(hPanel);
        
        app.enableMouseHandling(mainPanel);
        
        app.setUpdateCanvas((tApp)->{
            if (!hPanel.isDirty()) {
                return false; // false means no gui update
            }

            tApp.clearCanvas();

            TimeAndCount elapsed = TimeAndCount.onRun(()->tApp.getCanvas().addComponentSegments(view1OffsetOnCanvas, mainPanel), lprocViewer.textContentView);

            tApp.getCanvas().t(lprocViewer.getBottomMostRow()+5, 0, "elapsed = "+elapsed);

            return true; // true means updated
        });

        BiConsumer<TUIApp, Dimension> resizeAction = (tApp, size)->{
            
            if ( ! size.contains(tApp.getCanvas())) {
                tApp.getCanvas().clearCanvas();
                tApp.getCanvas().refresh();
            }
            
            // tApp.getCanvas().setSize(Math.min(size.getHeight(),25), Math.min(size.getWidth(),80));
            tApp.getCanvas().setSize(size.getHeight()-1, size.getWidth()-1);
            tApp.getCanvas().clearCanvas(); 
            lprocViewer.resize(sizeOfWindow);
            lprocViewer2.resize(sizeOfWindow);
            hPanel.resize(sizeOfPanel);
            hPanel.fullrecalcDim();
            hPanel.markAsDirty();
        };
        
        app.setOnResizeAction(resizeAction);
        
        resizeAction.accept(app, TestsCommon.terminalSize());
        
        app.setOnReadyStart((tApp)->{
            DefaultFocusManager focusManager = new DefaultFocusManager(Keys.TAB_KEYSTROKE);
            focusManager.addFocusable(lprocViewer, tApp.getEventBus() );

            focusManager.addFocusable(lprocViewer2, tApp.getEventBus() );
            focusManager.setName("mainFocusManager");
            tApp.getEventBus().appendEventHandler(focusManager);
            String fileToTail = fileNameToTail(args);

            LinuxProcessWatcher linuxProc = null;

            if (fileToTail != null) {
                try {
                    linuxProc = LinuxProcessWatcher.tailFile(fileToTail);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {

                String fileToView = fileNameToView(args);

                if (fileToView != null) {
                    try {
                        linuxProc = LinuxProcessWatcher.viewTextFile(fileToView);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    linuxProc = new LinuxProcessWatcher();
                    linuxProc.setInvokeScript("./slowRunner.sh testme");
                }
            }

            if (linuxProc != null)  {
                lprocViewer.setLinuxProcessWatcher(linuxProc);
                lprocViewer.invoke(tApp.getEventBus());
            }
            lprocViewer2.setLinuxProcessWatcher( new LinuxProcessWatcher().setInvokeScript("./slowRunner.sh testme")  );
            lprocViewer2.invoke(tApp.getEventBus());

            FocusManager.UTIL.verifyPathsToFocusables(focusManager, (Component<?>)tApp.getMainApplicationPanel());

            // following is test code when I want to see structure of focusables hierarchy
            // throw new IllegalStateException(FocusManager.UTIL.traverseAllFocusablesToString(focusManager));

        });
        
        app.start();
        // Logging.setEnabled(true);
        
    }

    public static void main(String[] args) throws Exception {
        slowRunnerTest2(args);
    }

}
