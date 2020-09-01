package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.ElapsedTime;
import dtb.terminalIO.util.StringSpinner;

public class CharacterViewerTest {

    public static void main(String[] args) throws IOException {

        TUIApp app = new TUIApp();

        app.initFromArgs(args);

        TextComponent dataToView = new TextComponent("\n\n Loading characters .. ");
        dataToView.setFillOnClear(false);

        Dimension scrollPanelDim = new Dimension() {

            @Override
            public int getWidth() {
                int width = Math.max(40, dataToView.getWidth() + 2);
                if (width > app.getDimensions().getWidth())
                    return app.getDimensions().getWidth() - 10;
                return width;
            }

            @Override
            public int getHeight() {
                int height = Math.max(15, dataToView.getHeight() + 2);
                if (height + 5 > app.getDimensions().getHeight()) {
                    return app.getDimensions().getHeight() - 5;
                }
                return height;
            }

        };

        ScrollableComponent scrollPanel = new ScrollableComponent(Colors.Blu, scrollPanelDim, dataToView);

        scrollPanel.setTitle(Colors.IGre, " ASCII VIEWER ");
        
        app.enableMouseHandling(scrollPanel);

        app.setUpdateCanvas((theApp) -> {
            theApp.getCanvas().clearCanvas();
            theApp.getCanvas().addComponentSegments(3, 5, scrollPanel);
            return true;
        });

        AtomicBoolean isProducerBusy = new AtomicBoolean(true);
        AtomicBoolean isConsumerBusy = new AtomicBoolean(true);

        StringSpinner spinner = new StringSpinner(StringSpinner.ARC);

        scrollPanel.asEventHandler(); // TODO: fixme!!!!!!  .. time passes ... why? .. oh .. I get it ..
        app.getEventBus().appendEventHandler(scrollPanel);

        BlockingQueue<String> stringQueue = new ArrayBlockingQueue<String>(1000);

        Runnable produceText = () -> {
            asciiCodesChart((s) -> {
                stringQueue.offer(s);
            });
            isProducerBusy.set(false);
        };

        StringBuilder textSource = new StringBuilder();
        
        Runnable consumeText = () -> {
            ElapsedTime et = new ElapsedTime();
            try {
                int i = 0;
                StringBuilder sb = new StringBuilder();
                while (isProducerBusy.get() || ! stringQueue.isEmpty()) {
                    String s = stringQueue.poll(15, TimeUnit.SECONDS);
                    if (s != null) {
                        sb.append(s);
                        i++;
                        if (i % 50 == 0) {
                            synchronized (textSource) {
                                textSource.append(sb.toString());
                                sb = new StringBuilder(sb.length());
                            }
                            // TestsCommon.sleep(10);
                        }
                    }
                }
                if (sb.length()>0) {
                    synchronized (textSource) {
                        textSource.append(sb.toString());
                    }
                }
                
            } catch (Exception e) {
                synchronized (textSource) {
                    textSource.append("\n\n" + e.getMessage() + "\n");
                }
            }
            isConsumerBusy.set(false);
        };

        app.start();
        
        int[] counter = new int[] {0,0, 100, 0};
        
        app.getEventBus().appendEventHandler( new OnIdleEventEventHandler.RepeatingInterval(counter[2], (te)->{
            
            counter[3]++;
            
            ElapsedTime et = ElapsedTime.onRun(()->{});
            boolean changed = false;
            synchronized (textSource) {
                counter[0]++;
                changed = counter[1] != textSource.length();
                if (changed) {
                    Dimension d = Dimension.UTIL.stringToDimension(textSource.toString());
                    dataToView.resize(d);
                    dataToView.setTextAndResize(textSource.toString());
                    et = ElapsedTime.onRun(()-> dataToView.getSegmentsBuffer().getSegments() );
                    counter[1] = textSource.length();
                }

            }
            scrollPanel.setFooterText("count: " + counter[3]);
            scrollPanel.setTitle(Colors.IGre, " ASCII VIEWER " /*+ et.elapsed()*/ + " "+ ((changed||isConsumerBusy.get())?spinner.spin()+" ":""));
            scrollPanel.resize(scrollPanelDim);
            app.doRefreshCanvasIfModified();
        }));

        new Thread(produceText).start();

        new Thread(consumeText).start();

    }

    private static void asciiCodesChart(Consumer<String> handleLine) {

        StringBuilder sb = new StringBuilder();
        String blueStr = Colors.Blu.asString();
        String whiteStr = Colors.Whi.asString();
        String yellowStr = Colors.IYel.asString();
        String rcol = ControlCode.RCol.asString();

        for (int ch = 33; ch < 12000; ch++) {

            String line = String.format("%s%4d    %s%1s %s ", whiteStr, ch, yellowStr, String.valueOf((char) ch), rcol);

            sb.append(line);

            if (ch % 5 == 0) {
                sb.append("\n\n");
                handleLine.accept(sb.toString());
                sb = new StringBuilder();
            } else {
                sb.append(blueStr + "|  " + rcol);
            }

        }
        
        sb.append("\n\n      --- "+Colors.BIGre.asString() +"done" + ControlCode.RCol.asString()  + "----");

        handleLine.accept(sb.toString());
    }

}
