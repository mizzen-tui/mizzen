package mizzen.components.card;

import java.util.function.Function;

import mizzen.components.AbstractComponentWrappper;
import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.composite.Composite;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.theme.Theme;
import mizzen.controlcodes.color.Color;
import mizzen.geom.position.Position;

/**
 * A card: a panel on the theme's surface, framed in its line characters (rounded in Theme.ROUNDED), a column of room
 * inside each side, an optional title on its top line. What it holds takes the theme on the surface (Theme.surfaceTheme),
 * so text, inputs and badges in it are in the card's colours.
 * <pre>
 *     new Card("Site").add(new TextComponent("HTTP 200")).add(new TextComponent("store.example.com"));
 *
 *     ╭─ Site ─────────────╮
 *     │ HTTP 200           │
 *     │ store.example.com  │
 *     ╰────────────────────╯
 * </pre>
 * A theme with no surface of its own (Legacy) draws it as a bordered panel on the background.
 */
public class Card extends AbstractComponentWrappper<Composite> {

    private final VerticalPanel content = new VerticalPanel();
    private Function<Theme, Color> frame = Theme::textColor;           // the frame and title's colour (Tile: accent while focused)

    public Card() {
        this("");
    }

    public Card(String title) {
        super((Color) null, new OnSurface());
        Composite panel = getWrappedComponent();
        panel.setThemeColor((theme) -> frame.apply(theme));              // the frame and title: text on the surface
        panel.setBorder(true);
        if (title != null && !title.isEmpty()) {
            panel.setTitle(" " + title + " ");
        }
        panel.setFillOnClear(true);
        HorizontalPanel padded = new HorizontalPanel().setHorizontalSpacer(0)
                .add(new TextComponent(" ")).add(content).add(new TextComponent(" "));      // a column of room each side
        padded.setThemeColor(Theme::textColor);                         // what it holds: text, whatever the frame's colour
        panel.add(Position.ZERO, padded);                               // inside the frame
    }

    // the frame and title in another theme colour, worked out as they are drawn: a focused Tile's accent
    void frameColor(Function<Theme, Color> color) {
        frame = color;
        // the frame is drawn by the panel's border, which markAsDirty and Theme.repaint don't reach: it would redraw
        // only on a theme change. setBorderColor(null) marks it dirty, and null keeps it following the panel's colour.
        getWrappedComponent().setBorderColor(null);
    }

    /** a row, under the ones added before */
    public Card add(Component<?> row) {
        content.add(row);
        return this;
    }

    // the card's panel: given no theme of its own, the theme of where it is, on its surface - worked out at each look-up,
    // so a theme set later still reaches it
    private static final class OnSurface extends Composite {
        @Override
        public Theme getOwnTheme() {
            Theme own = super.getOwnTheme();
            if (own != null) {
                return own;
            }
            Object above = getParentContainer();
            return above instanceof Component ? Theme.lookUp((Component<?>) above).surfaceTheme() : null;
        }
    }
}
