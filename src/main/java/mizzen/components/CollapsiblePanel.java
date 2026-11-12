package mizzen.components;

import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.Button;
import mizzen.controlcodes.color.Color;
import mizzen.geom.CanResize;
import mizzen.geom.Dimension;
import mizzen.util.StringUtils;

/**
 * A framed title with a (+)/(-) button that opens and closes the content under it. It starts closed.
 * <p/>
 * Its height follows: closed, the frame and the title (3 rows for a one-line title); open, that and the content. Given a
 * size with {@link #resize}, the width is kept and the content takes the frame's inner width; open, the content takes
 * the rows left under the title, so a parent can make it fill; closed, the height given is ignored. The (+) stays at
 * the right edge.
 * <pre>
 *     CollapsiblePanel notices = new CollapsiblePanel("2 warnings", lines);
 *     notices.resize(new Dimension.Simple(3, screenWidth));   // closed: 3 rows; open: 3 + the lines
 *     notices.setOpen(wasOpen);                               // after rebuilding it: keep the operator's choice
 * </pre>
 * Without colours given, the button is in the theme's button colour and the frame in the panel's colour.
 */
public class CollapsiblePanel extends AbstractComponentWrappper<VerticalPanel>  {

    static final String show_BUTTON_TEXT = "(+)";
    static final String hide_BUTTON_TEXT = "(-)";

    final ButtonBar titleBanner;                // package: for its test
    final Button openCloseButton;
    private final Component<?> panelToHide;
    private TextComponent spacer;               // pushes the button to the right edge; null when not needed
    private Dimension sizeGiven;                // the last size given to resize(..); null: its own (natural) size

    /** as the constructor with a colour first, in its theme's colours */
    public CollapsiblePanel(Component<?> titleComponent, Component<?> panelToHide) {
        this((Color) null, titleComponent, panelToHide);
    }

    /** as the constructor with a colour first, in its theme's colours */
    public CollapsiblePanel(String titleLabel, Component<?> panelToHide) {
        this((Color) null, titleLabel, panelToHide);
    }

    /** color, borderColor: null for the theme's (the frame then follows the panel's colour) */
    public CollapsiblePanel(Color color, Color borderColor, Component<?> titleComponent, Component<?> panelToHide) {
        super(color, new VerticalPanel());
        this.getWrappedComponent().setBorderColor(borderColor);

        this.panelToHide = panelToHide;

        titleBanner = new ButtonBar().setNoBorderChars();
        openCloseButton = new Button(show_BUTTON_TEXT);
        titleBanner.addComponent(titleComponent);
        titleBanner.addComponent(openCloseButton);
        fitTitle(panelToHide.getWidth());

        this.getWrappedComponent().add(titleBanner);
        this.getWrappedComponent().add(panelToHide);

        // a push button: its "change" is a press
        openCloseButton.onChange((te) -> setOpen( ! isOpen()));

        panelToHide.hide();
    }

    public CollapsiblePanel(Color color, Component<?> titleComponent, Component<?> panelToHide) {
        this(color, null, titleComponent, panelToHide);
    }

    public CollapsiblePanel(Color color, String titleLabel, Component<?> panelToHide) {
        this(color, null, new TextComponent(titleLabel), panelToHide);
    }

    public CollapsiblePanel(Color color, Color borderColor, String titleLabel, Component<?> panelToHide) {
        this(color, borderColor, new TextComponent(titleLabel), panelToHide);
    }

    /** is the content shown? */
    public boolean isOpen() {
        return getWrappedComponent().isChildVisible(panelToHide);
    }

    /** opens (shows the content) or closes it, as the (+)/(-) button does; the height follows */
    public CollapsiblePanel setOpen(boolean open) {
        if (open == isOpen()) {
            return this;
        }
        if (open) {
            panelToHide.show();
        } else {
            panelToHide.hide();
        }
        openCloseButton.setButtonText(open ? hide_BUTTON_TEXT : show_BUTTON_TEXT);
        if (sizeGiven != null) {
            // sized from outside, its size is fixed: give it the height for open or closed
            resize(new Dimension.Simple(heightFor(open, -1), sizeGiven.getWidth()));
        }
        return this;
    }

    // the frame (2), the title and, open, the content: the rows asked for, or else the content's own height
    private int heightFor(boolean open, int rowsAskedFor) {
        int closed = titleBanner.getHeight() + 2;
        if ( ! open) {
            return closed;
        }
        return rowsAskedFor >= 0 ? Math.max(closed, rowsAskedFor) : closed + panelToHide.getHeight();
    }

    // the title as wide as innerWidth: a spacer between the title and the button keeps the button at the right edge
    private void fitTitle(int innerWidth) {
        if (spacer != null) {
            titleBanner.remove(spacer);
            spacer = null;
        }
        int room = innerWidth - titleBanner.getWidth();
        if (room > 0) {
            spacer = new TextComponent(StringUtils.repeat(" ", room));
            titleBanner.insertComponent(1, spacer);
        }
    }

    private boolean resizing = false;
    @Override
    public void resize(Dimension d) {
        if (resizing) { // super(ancestor) can indirectly call child again - we stop that
            return;
        }
        resizing = true;
        try {
            sizeGiven = d;
            boolean open = isOpen();
            int height = heightFor(open, d.getHeight());
            int innerWidth = Math.max(0, d.getWidth() - 2);
            fitTitle(innerWidth);
            if (this.panelToHide instanceof CanResize) {
                int contentHeight = open ? height - (titleBanner.getHeight() + 2) : panelToHide.getHeight();
                ((CanResize) this.panelToHide).resize(new Dimension.Simple(contentHeight, innerWidth));
            }
            super.resize(new Dimension.Simple(height, d.getWidth()));
        } finally {
            resizing = false;
        }
    }

}
