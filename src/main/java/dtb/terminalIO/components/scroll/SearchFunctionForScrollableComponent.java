package dtb.terminalIO.components.scroll;

import java.util.Collection;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ComponentWithHilightedText;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.input.HasValue;
import dtb.terminalIO.components.input.HasValue.OnValueChangedEventHandler;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.RequestHideCursorEvent;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.RequestReleaseFocusEvent;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.SegmentsSearchUtils;

public class SearchFunctionForScrollableComponent extends HorizontalPanel {

    private TextComponent find;
    private TextInput findTextInput;
    private ConditionalEventHandler findTextInputEventHandler;
    private String lastFindText = null;
    private OnValueChangedEventHandler<String> titleUpdater;
    private ConditionalEventHandler finishSearchInputEventHandler;
    private ScrollableComponent viewComp;
    private ComponentWithHilightedText srcOfText;
    private int maxSearchTextWidth = 10;
    private int minSearchLength = 3;

    public SearchFunctionForScrollableComponent(Color color, ScrollableComponent viewComp) {
        super(color);
        this.viewComp = viewComp;
        this.find = new TextComponent(SearchFunctionForScrollableComponent.this.getColor(),
                " " + SearchFunctionForScrollableComponent.this.getColor().bold().asString() + "^F"
                        + SearchFunctionForScrollableComponent.this.getColor().asString() + "ind");
        this.add(find);
    }

    public void hilite() {
        super.hilite();
        if (findTextInput != null) {
            this.findTextInput.setColor(textInputSelectedColor());
            this.findTextInput.setEmptyColor(textInputSelectedColor());
        }
    }

    private Color textInputSelectedColor() {
        return SearchFunctionForScrollableComponent.this.getColor().invert();
    }

    public void unHilite() {
        super.unHilite();
        if (findTextInput != null) {
            this.findTextInput.setColor(SearchFunctionForScrollableComponent.this.getColor());
            this.findTextInput.setEmptyColor(SearchFunctionForScrollableComponent.this.getColor());
        }
    }

    void setInFindMode(TerminalEvent<?> te) {
        setInFindMode(te, findTextInput == null);
    }

    private void removeFindTextInput(EventBus eventBus) {
        if (SearchFunctionForScrollableComponent.this.findTextInput != null) {
            TextInput textInput = SearchFunctionForScrollableComponent.this.findTextInput;
            SearchFunctionForScrollableComponent.this.findTextInput = null;
            SearchFunctionForScrollableComponent.this.lastFindText = textInput.getText();
            eventBus.raiseEvent(new RequestReleaseFocusEvent(textInput));
            eventBus.raiseEvent(new RequestHideCursorEvent(textInput));
            viewComp.removeEventHandler(findTextInputEventHandler);
            SearchFunctionForScrollableComponent.this.findTextInputEventHandler = null;
            if (SearchFunctionForScrollableComponent.this.finishSearchInputEventHandler != null) {
                viewComp.removeEventHandler(SearchFunctionForScrollableComponent.this.finishSearchInputEventHandler);
                SearchFunctionForScrollableComponent.this.finishSearchInputEventHandler = null;
            }
            SearchFunctionForScrollableComponent.this.remove(textInput);
            viewComp.refreshFooter();
            eventBus.raiseEvent(new RequestUpdateCanvasEvent(this));
        }
    }

    void setInFindMode(TerminalEvent<?> te, boolean enable) {

        if (enable) {

            attachComponentWithHilightedTextIfNeeded(viewComp, this);

            findTextInput = new TextInput(textInputSelectedColor(), maxSearchTextWidth) {
                @Override
                public void setFocus(boolean focus, EventBus eventBus) {
                    super.setFocus(focus, eventBus);
                    if (!focus) {
                        removeFindTextInput(eventBus);
                    }
                }
            };
            this.findTextInput.setEmptyColor(textInputSelectedColor());
            if (lastFindText != null) {
                // should I trigger the search here?
                findTextInput.setText(lastFindText);
            } else {
                findTextInput.setText("");
            }
            this.add(findTextInput);
            // FIXME: 1) doing a prepend causes hotkeys on viewComp1 to be blocked including
            // CTRL-F
            // so cannot get out of find mode.. TextInput accepting too many keys
            // need to fix
            // 2) Cursor never hides
            // 3) Cursor mis-positioned on terminal resize
            findTextInputEventHandler = viewComp.prependEventHandlerWithCanvasUpdate(findTextInput);
            findTextInput.requestFocus(te);

            finishSearchInputEventHandler = viewComp.prependEventHandlerWithCanvasUpdate(
                    new KeyInputEventHandler(Keys.ESC_KEYSTROKE, new EventHandler() {
                        @Override
                        public void handle(TerminalEvent<?> te) {
                            if (findTextInput != null) {
                                findTextInput.setText("");
                            }
                            setInFindMode(te, false);
                            srcOfText.clearHilights();
                            te.getEventBus().appendEventHandler(new OnIdleEventEventHandler.OneTime(
                                    (te1) -> removeFindTextInput(te1.getEventBus())));
                            lastFindText = null;
                        }
                    }));

            titleUpdater = new HasValue.TimerControlledOnValueChangedEventHandler<String>(findTextInput) {
                @Override
                public void valueChanged(HasValue<String> source) {
                    String newValue = source.getValue();
                    viewComp.setTitle(newValue);

                    Rectangle targetToView = null;

                    srcOfText.clearHilights();

                    if (newValue == null || newValue.length() < minSearchLength) {
                        return;
                    }

                    Collection<Rectangle> searchResult = SegmentsSearchUtils
                            .searchTextInSegmentsAll(srcOfText.getSegmentsBuffer(), newValue);

                    if (searchResult != null && searchResult.size() > 0) {
                        targetToView = searchResult.iterator().next();
                        if (targetToView != null) {
                            viewComp.scrollToView(targetToView);
                        }

                        for (Rectangle r : searchResult) {
                            srcOfText.addHilitedRegion(r);
                        }
                        viewComp.setTitle(newValue + " (" + searchResult.size() + ")");
                    }

                }
            };
            te.getEventBus().appendEventHandler(titleUpdater);

        } else {
            findTextInput.requestReleaseFocus(te);
            if (titleUpdater != null) {
                te.getEventBus().removeEventHandler(titleUpdater);
                titleUpdater = null;
                viewComp.setTitle("");
            }
        }
    }

    public static void addFinderToScrollable(ScrollableComponent viewComp, KeyStroke hotKey, int maxSearchTextWidth) {

        SearchFunctionForScrollableComponent finder = new SearchFunctionForScrollableComponent(viewComp.getColor(),
                viewComp);
        finder.maxSearchTextWidth = maxSearchTextWidth;

        viewComp.addFooterButton(finder, hotKey, (te) -> {
            finder.setInFindMode(te);
            viewComp.refreshFooter();
        });
    }

    private static void attachComponentWithHilightedTextIfNeeded(ScrollableComponent viewComp,
            SearchFunctionForScrollableComponent finder) {

        if (finder.srcOfText != null) {
            return; // all done
        }

        if (!(viewComp.getComponentBeingViewed() instanceof ComponentWithHilightedText)) {

            Component<?> componentBeingViewed = viewComp.getComponentBeingViewed();
            ComponentWithHilightedText hilitingComponent = new ComponentWithHilightedText(
                    componentBeingViewed.getColor());

            hilitingComponent.setFillOnClear(false);

            if (componentBeingViewed instanceof Focusable) {
                hilitingComponent.setFocusable((Focusable) componentBeingViewed);
            } else if (componentBeingViewed instanceof ToFocusable) {
                hilitingComponent.setFocusable(((ToFocusable) componentBeingViewed).asFocusable());
            }
            viewComp.setComponentBeingViewed(hilitingComponent);
            hilitingComponent.add(componentBeingViewed);
            viewComp.setComponentBeingViewed(hilitingComponent);

        }

        // at this point child being viewed is a ComponentWithHilightedText

        finder.srcOfText = (ComponentWithHilightedText) viewComp.getComponentBeingViewed();
    }

    public static void addFinderToScrollable(ScrollableComponent viewComp, KeyStroke hotKey) {
        addFinderToScrollable(viewComp, hotKey, 10);
    }

    public static void addFinderToScrollable(ScrollableComponent viewComp, int maxSearchTextWidth) {
        addFinderToScrollable(viewComp, Keys.CTRL_F_KEYSTROKE, maxSearchTextWidth);
    }

    public static void addFinderToScrollable(ScrollableComponent viewComp) {
        addFinderToScrollable(viewComp, Keys.CTRL_F_KEYSTROKE);
    }

}
