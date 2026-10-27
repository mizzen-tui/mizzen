package mizzen.examples;

import java.util.Arrays;
import java.util.List;

import mizzen.app.TUIApp;
import mizzen.components.ChildComponentManager;
import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.chart.ChartItem;
import mizzen.components.chart.DonutChart;
import mizzen.components.composite.Composite;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.input.Button;
import mizzen.components.input.RadioGroup;
import mizzen.components.input.TextInputInteger;
import mizzen.controlcodes.color.Colors;
import mizzen.events.EventBus;
import mizzen.events.OnIdleEventEventHandler;
import mizzen.events.keyboard.Keys;
import mizzen.geom.Dimension;

/**
 * Donut charts: the new one (your objects as slices - click one, or use the arrows) and, to compare,
 * the legacy one (a single animated value). The switch at the top picks which is shown.
 * <p/>
 * When the legacy charts are dropped, so is the switch and renderLegacy.
 */
public class DonutChartTest {

    /** a disk: what the new chart shows */
    static class Disk {
        final String mount;
        final long usedGb;

        Disk(String mount, long usedGb) {
            this.mount = mount;
            this.usedGb = usedGb;
        }
    }

    /** what is on show: its components, and what to undo when it goes */
    static class Shown {
        final List<Component<?>> components;
        final Runnable stop;

        Shown(List<Component<?>> components, Runnable stop) {
            this.components = components;
            this.stop = stop;
        }
    }


    public static void main(String args[]) {
        new TUIApp(args)
        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)->{

            RadioGroup<String> version = RadioGroup.horizontal("new", "legacy");
            HorizontalPanel top = new HorizontalPanel();
            top.add(new TextComponent(" Donut chart: "));
            top.add(version);
            tApp.getMainApplicationPanel().add(0, 0, top);

            Shown[] shown = { null };   // what this run shows - not a static: it would outlive the run (e.g. an app embedded twice)

            show(tApp, shown, renderNew(tApp));
            version.onChange((te) -> {
                show(tApp, shown, "legacy".equals(version.getValue()) ? renderLegacy(tApp) : renderNew(tApp));
            });
        })
        .start();
    }

    /** takes off what was shown, puts on the new */
    private static void show(TUIApp tApp, Shown[] shown, Shown next) {
        ChildComponentManager<?> main = tApp.getMainApplicationPanel();
        if (shown[0] != null) {
            shown[0].stop.run();
            for (Component<?> c : shown[0].components) {
                main.remove(c);
            }
        }
        shown[0] = next;
    }

    /** the new chart: disks by space used; click a slice (or arrows) and the status line says which */
    static Shown renderNew(TUIApp tApp) {
        List<Disk> disks = Arrays.asList(new Disk("/", 120), new Disk("/home", 340), new Disk("/var", 45),
                                         new Disk("/opt", 80), new Disk("/data", 410));

        Composite appPanel = (Composite) tApp.getMainApplicationPanel();
        DonutChart<Disk> donut = new DonutChart<Disk>(Colors.Gre, chartSize(appPanel))
                .sliceSize((d) -> d.usedGb)
                .sliceLabel((d) -> d.mount)
                .setItems(disks);
        donut.setTitle(" Disk use - click a slice, or TAB here and use the arrows ");

        TextComponent status = new TextComponent(" Nothing selected yet");
        donut.onChange((te) -> {
            ChartItem<Disk> slice = donut.getValue();
            status.setTextAndResize(String.format(" %s: %d GB (%.1f%% of all)", slice.label(), slice.item().usedGb, slice.percent()));
        });

        appPanel.add(2, 0, donut);
        appPanel.add(appPanel.getHeight() - 1, 0, status);

        // the terminal resized: the chart takes the new size (and redraws, still round), the status line goes to the new bottom
        appPanel.setOnResize((size) -> {
            donut.resize(chartSize(size));
            appPanel.remove(status);
            appPanel.add(size.getHeight() - 1, 0, status);
        });
        return new Shown(Arrays.<Component<?>>asList(donut, status), () -> appPanel.setOnResize((size) -> { }));
    }

    /** the chart: the panel less the switch row, a blank row and the status line */
    private static Dimension chartSize(Dimension panel) {
        return new Dimension.Simple(Math.max(8, panel.getHeight() - 4), Math.max(30, panel.getWidth()));
    }

    /** the legacy chart: one value going round, which can be typed in */
    static Shown renderLegacy(TUIApp tApp) {
        EventBus bus = tApp.getEventBus();
        int[] dataPoint = new int[] {0};
        mizzen.components.chart.legacy.DonutChart donutChart =
                new mizzen.components.chart.legacy.DonutChart(Colors.BRed, new Dimension.Simple(20, 60));
        donutChart.setData(dataPoint[0],100,String.format("%3d",dataPoint[0])+"%");

        HorizontalPanel userControls = new HorizontalPanel().setHorizontalSpacer(3);
        userControls.setEmptyChar('░');
        Button stopButton = new Button(Colors.BGre,"Stop");
        stopButton.setHotKey(Keys.CTRL_C_KEYSTROKE).setDisplayHotKey(true);
        stopButton.onChange( tApp.getTerminateAppHotKeyEventHandler()  );
        TextInputInteger dataPointTextBox = new TextInputInteger(3);
        dataPointTextBox.onChange((te)->{
            Integer newValue = dataPointTextBox.getValue();
            if (newValue != null && newValue >=0 && newValue <=360) {
                dataPoint[0] = newValue;
            }
        });
        userControls.add(stopButton);
        userControls.add(dataPointTextBox);

        tApp.getMainApplicationPanel().add(2, 0, donutChart);
        tApp.getMainApplicationPanel().add(1, 1, userControls);

        OnIdleEventEventHandler.RepeatingInterval animate = new OnIdleEventEventHandler.RepeatingInterval(100, (te)-> {
            int maxData = 360;
            dataPoint[0] = ( dataPoint[0] + 1 ) % maxData;
            donutChart.setData(dataPoint[0],maxData,String.format("%3d",dataPoint[0]));
        });
        bus.appendEventHandler(animate);
        return new Shown(Arrays.<Component<?>>asList(donutChart, userControls), () -> bus.removeEventHandler(animate));
    }
}
