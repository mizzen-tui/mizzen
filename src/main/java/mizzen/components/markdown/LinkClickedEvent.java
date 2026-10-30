package mizzen.components.markdown;

import mizzen.events.TerminalEvent;

/** A link in a MarkdownView was clicked: its target as the document wrote it (MarkdownView.onLink). */
public class LinkClickedEvent extends TerminalEvent<String> {
    private static final long serialVersionUID = 4120937716409825531L;

    public LinkClickedEvent(MarkdownView view, String target) {
        super(view, TerminalEvent.TYPE.LINK_CLICKED, target);
    }

    public String getTarget() {
        return getEventData();
    }
}
