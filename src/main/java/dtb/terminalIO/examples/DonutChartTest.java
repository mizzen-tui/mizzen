package dtb.terminalIO.examples;

import java.util.Arrays;
import java.util.List;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ChildComponentManager;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.chart.ChartItem;
import dtb.terminalIO.components.chart.DonutChart;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.RadioGroup;
import dtb.terminalIO.components.input.TextInputInteger;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;

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

    private static Shown shown;

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

            show(tApp, renderNew(tApp));
            version.onChange((te) -> {
                show(tApp, "legacy".equals(version.getValue()) ? renderLegacy(tApp) : renderNew(tApp));
                te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent((Component<?>) tApp.getMainApplicationPanel()));
            });
        })
        .start();
    }

    /** takes off what was shown, puts on the new */
    private static void show(TUIApp tApp, Shown next) {
        ChildComponentManager<?> main = tApp.getMainApplicationPanel();
        if (shown != null) {
            shown.stop.run();
            for (Component<?> c : shown.components) {
                main.remove(c);
            }
        }
        shown = next;
    }

    /** the new chart: disks by space used; click a slice (or arrows) and the status line says which */
    static Shown renderNew(TUIApp tApp) {
        List<Disk> disks = Arrays.asList(new Disk("/", 120), new Disk("/home", 340), new Disk("/var", 45),
                                         new Disk("/opt", 80), new Disk("/data", 410));

        DonutChart<Disk> donut = new DonutChart<Disk>(Colors.Gre, new Dimension.Simple(18, 64))
                .sliceSize((d) -> d.usedGb)
                .sliceLabel((d) -> d.mount)
                .setItems(disks);
        donut.setTitle(" Disk use - click a slice, or TAB here and use the arrows ");

        TextComponent status = new TextComponent(" Nothing selected yet");
        donut.onChange((te) -> {
            ChartItem<Disk> slice = donut.getValue();
            status.setTextAndResize(String.format(" %s: %d GB (%.1f%% of all)", slice.label(), slice.item().usedGb, slice.percent()));
            te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(status));
        });

        tApp.getMainApplicationPanel().add(2, 0, donut);
        tApp.getMainApplicationPanel().add(21, 0, status);
        return new Shown(Arrays.<Component<?>>asList(donut, status), () -> { });
    }

    /** the legacy chart: one value going round, which can be typed in */
    static Shown renderLegacy(TUIApp tApp) {
        EventBus bus = tApp.getEventBus();
        int[] dataPoint = new int[] {0};
        dtb.terminalIO.components.chart.legacy.DonutChart donutChart =
                new dtb.terminalIO.components.chart.legacy.DonutChart(Colors.BRed, new Dimension.Simple(20, 60));
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
            te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(donutChart));
        });
        bus.appendEventHandler(animate);
        return new Shown(Arrays.<Component<?>>asList(donutChart, userControls), () -> bus.removeEventHandler(animate));
    }
}
