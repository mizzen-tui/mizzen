package mizzen.examples;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import mizzen.app.TUIApp;
import mizzen.components.TextComponent;
import mizzen.components.scroll.ScrollableComponent;
import mizzen.controlcodes.color.Colors;
import mizzen.events.OnIdleEventEventHandler;
import mizzen.geom.Dimension;
import mizzen.util.ElapsedTime;
import mizzen.util.StringSpinner;

public class CharacterViewerTest {

    public static void main(String[] args) throws IOException {

        TUIApp app = new TUIApp(args).setDefaultMainApplicationPanel();


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

        app.getMainApplicationPanel().add(3, 5, scrollPanel);     // drawn, and given keys, as part of the main panel

        AtomicBoolean isProducerBusy = new AtomicBoolean(true);
        AtomicBoolean isConsumerBusy = new AtomicBoolean(true);

        StringSpinner spinner = new StringSpinner(StringSpinner.ARC);


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
            
            ElapsedTime et = ElapsedTime.onRunOf(()->{});
            boolean changed = false;
            synchronized (textSource) {
                counter[0]++;
                changed = counter[1] != textSource.length();
                if (changed) {
                    Dimension d = Dimension.UTIL.stringToDimension(textSource.toString());
                    dataToView.resize(d);
                    dataToView.setTextAndResize(textSource.toString());
                    et = ElapsedTime.onRunOf(()-> dataToView.getSegmentsBuffer().getSegments() );
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
        String rcol = Colors.RCol.asString();

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

        sb.append("\n\n      --- "+Colors.BIGre.asString() +"done" + Colors.RCol.asString()  + "----");

        handleLine.accept(sb.toString());
    }

}
