package mizzen.examples;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import mizzen.app.TUIApp;
import mizzen.components.Component;
import mizzen.components.LinuxProcessViewer;
import mizzen.components.TextComponent;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.util.ElapsedTime;
import mizzen.util.linuxproc.ExecUtil;
import mizzen.util.linuxproc.LinuxProcessWatcher;

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
                return Math.max(0, app.getCanvas().getWidth() / 2 - 6);       // none in a narrow terminal
            }

            @Override
            public int getHeight() {
                return Math.max(0, app.getCanvas().getHeight() - 17);         // none in a short one
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

        // on the app's main panel, so the app's focus manager covers it (TAB / SHIFT-TAB, clicks)
        app.getMainApplicationPanel().add(view1OffsetOnCanvas, mainPanel);
        Component<?> appPanel = (Component<?>) app.getMainApplicationPanel();

        // the app's usual drawing, timed: this demo shows how long drawing the viewers takes
        app.setUpdateCanvas((tApp)->{
            if (!hPanel.isDirty()) {
                return false; // false means no gui update
            }

            tApp.clearCanvas();

            TimeAndCount elapsed = TimeAndCount.onRun(()->tApp.getCanvas().addComponentSegments(appPanel), lprocViewer.contentView);

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
        };
        
        app.setOnResizeAction(resizeAction);
        
        resizeAction.accept(app, TestsCommon.terminalSize());
        
        app.setOnReadyStart((tApp)->{
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
                    linuxProc.setInvokeScript("scripts/slowRunner.sh testme");
                }
            }

            if (linuxProc != null)  {
                lprocViewer.setLinuxProcessWatcher(linuxProc);
                lprocViewer.invoke(tApp.getEventBus());
            }
            lprocViewer2.setLinuxProcessWatcher( new LinuxProcessWatcher().setInvokeScript("scripts/slowRunner.sh testme")  );
            lprocViewer2.invoke(tApp.getEventBus());

        });
        
        app.start();
        // Logging.setEnabled(true);
        
    }

    public static void main(String[] args) throws Exception {
        slowRunnerTest2(args);
    }

}
