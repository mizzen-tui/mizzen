package mizzen.components.plot;

import mizzen.components.AbstractComponentWithContainer;
import mizzen.components.ButtonBar;
import mizzen.components.TextComponent;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.input.DisplayableValue.SimpleDisplayValue;
import mizzen.components.input.RadioGroup;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;

/**
 * Controls for a PlotArea on one row: its scaling, zoom (+ -) and pan (< > ^ v). All three show by
 * default; leave out what the plot doesn't need. Get one from the plot:
 * <pre>
 *      (•) stretch  ( ) equal  ( ) none   zoom [+|-]   pan [<|>|^|v]
 *
 *     window.setFooterPanel(plot.navigationControls());
 *     window.setFooterPanel(plot.navigationControls().showScaling(false));    // zoom and pan only
 * </pre>
 * A pan button moves the view a tenth of what it shows (PlotArea.panByFraction), so a step looks
 * the same at any zoom; a zoom button shows 1.5 times less, or more.
 */
public class PlotNavigation extends AbstractComponentWithContainer<HorizontalPanel> {

    private static final double PAN_FRACTION = 0.1;
    private static final double ZOOM_FACTOR = 1.5;

    private final PlotArea plot;
    private final RadioGroup<PlotArea.Scaling> scaling;
    private final ButtonBar zoom = plainEnds(new ButtonBar());
    private final ButtonBar pan = plainEnds(new ButtonBar());

    private boolean showScaling = true;
    private boolean showZoom = true;
    private boolean showPan = true;

    PlotNavigation(Color color, PlotArea plot) {
        super(color, new HorizontalPanel(color, 0));
        this.plot = plot;

        scaling = RadioGroup.horizontal(new SimpleDisplayValue<PlotArea.Scaling>("stretch", PlotArea.Scaling.STRETCH),
                                        new SimpleDisplayValue<PlotArea.Scaling>("equal", PlotArea.Scaling.EQUAL),
                                        new SimpleDisplayValue<PlotArea.Scaling>("none", PlotArea.Scaling.NONE));
        scaling.onChange((te) -> plot.scaling(scaling.getValue()));

        zoom.addButton("+", null, (te) -> plot.zoom(ZOOM_FACTOR));
        zoom.addButton("-", null, (te) -> plot.zoom(1 / ZOOM_FACTOR));

        pan.addButton("<", null, (te) -> plot.panByFraction(-PAN_FRACTION, 0));
        pan.addButton(">", null, (te) -> plot.panByFraction(PAN_FRACTION, 0));
        pan.addButton("^", null, (te) -> plot.panByFraction(0, PAN_FRACTION));
        pan.addButton("v", null, (te) -> plot.panByFraction(0, -PAN_FRACTION));

        layOut();
    }

    PlotNavigation(PlotArea plot) {
        this(Colors.DEFAULT_COLOR, plot);
    }

    /** the stretch / equal / none choice (shown by default) */
    public PlotNavigation showScaling(boolean show) {
        this.showScaling = show;
        layOut();
        return this;
    }

    /** the + - buttons (shown by default) */
    public PlotNavigation showZoom(boolean show) {
        this.showZoom = show;
        layOut();
        return this;
    }

    /** the < > ^ v buttons (shown by default) */
    public PlotNavigation showPan(boolean show) {
        this.showPan = show;
        layOut();
        return this;
    }

    /** dividers between the buttons, but no ┤ ├ at the ends: those join a border line, and this row is not on one */
    private static ButtonBar plainEnds(ButtonBar bar) {
        return bar.setLeftMostBorderChar((char) 0).setRightMostBorderChar((char) 0);
    }

    /** the parts shown, left to right, each with its label, three spaces apart */
    private void layOut() {
        HorizontalPanel row = getWrappedComponent();
        row.clearAndRemoveAllChildren();
        String gap = " ";
        if (showScaling) {
            scaling.setValue(plot.getScaling());                   // the plot's scaling, even if set since
            row.add(new TextComponent(gap));
            row.add(scaling);
            gap = "   ";
        }
        if (showZoom) {
            row.add(new TextComponent(gap + "zoom "));
            row.add(zoom);
            gap = "   ";
        }
        if (showPan) {
            row.add(new TextComponent(gap + "pan "));
            row.add(pan);
        }
    }
}
