package mizzen.components;

import java.util.function.Function;

import mizzen.components.theme.Theme;
import mizzen.controlcodes.color.Color;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment.SegmentImpl;

/**
 * A badge: a coloured dot and a word or two - a state at a glance, or a name.
 * <pre>
 *     new Badge(Theme::successColor, "OK")           ● OK       (the dot in the theme's success colour)
 *     new Badge(Theme::errorColor, "ALERT")          ● ALERT
 *     new Badge("I E L T S   G U A R D I A N")       ● ...      (the dot in the theme's accent)
 * </pre>
 * The text is in the text colour (setThemeColor for another, setColor for one of its own). The dot ● is East Asian
 * Width A, as the box-drawing lines are: one column, except on a terminal set to draw those two wide.
 */
public class Badge extends BoundedComponent<Badge> {

    private static final String DOT = "●";

    private String text;
    private Function<Theme, Color> dotColor;

    /** text after a dot in the theme's accent colour */
    public Badge(String text) {
        this(Theme::accentColor, text);
    }

    /** text after a dot in that theme colour */
    public Badge(Function<Theme, Color> dotColor, String text) {
        super((Color) null, new Dimension.Simple(1, 2 + text.length()));
        this.dotColor = dotColor;
        this.text = text;
        setFillOnClear(false);
    }

    public Badge setText(String text) {
        this.text = text;
        resize(new Dimension.Simple(1, 2 + text.length()));
        markAsDirty();
        return this;
    }

    public String getText() {
        return text;
    }

    /** the dot's theme colour: e.g. Theme::successColor for OK, Theme::errorColor for an alert */
    public Badge setDotThemeColor(Function<Theme, Color> dotColor) {
        this.dotColor = dotColor;
        markAsDirty();
        return this;
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.add(new SegmentImpl(dotColor.apply(getTheme()), this, -1, DOT));
        segments.add(new SegmentImpl(color, new Position.Simple(getRow(), getCol() + 1), -1, " " + text));
    }
}
