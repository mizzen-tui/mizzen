package mizzen.components.scroll;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import mizzen.components.ComponentWithFocus;
import mizzen.components.BoundedComponent;
import mizzen.components.ButtonBar;
import mizzen.components.Component;
import mizzen.components.HilitingComponent;
import mizzen.components.TextComponent;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.input.Button;
import mizzen.components.input.HasValue.TimerControlledOnValueChangedEventHandler;
import mizzen.components.input.HasValue;
import mizzen.components.input.TextInput;
import mizzen.components.scroll.WindowViewComponent.CustomComponentDrawFunction;
import mizzen.controlcodes.color.Color;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.OnIdleEventEventHandler.RepeatingIntervalWhileCond;
import mizzen.events.OnIdleEventEventHandler;
import mizzen.events.RequestHideCursorEvent;
import mizzen.events.RequestUpdateCanvasEvent;
import mizzen.events.TerminalEvent;
import mizzen.events.focus.RequestReleaseFocusEvent;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyInputEventHandler;
import mizzen.events.keyboard.KeyboardHandler;
import mizzen.events.keyboard.Keys;
import mizzen.geom.CanResize;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.SegmentsSearchUtils.TextInSegmentSearchResult;
import mizzen.textsegments.SegmentsSearchUtils;
import mizzen.util.ElapsedTime;
import mizzen.util.ListComparator;
import mizzen.util.StringSpinner;

public class SearchFunctionForScrollableComponent extends ComponentWithFocus<ButtonBar, SearchFunctionForScrollableComponent> {




    private List<Rectangle> searchResult;
    private TextInSegmentSearchResult currentSearchResult;
    private RepeatingIntervalWhileCond asynchSearchEventHandler;
    private Button findButton;
    private TextComponent busyIndicator;
    private StringSpinner spinner;
    private Button findPrevButton;
    private HorizontalPanel prevNextPanel;
    private TextComponent findStatsDetails;
    private int currentFindIDX = -1;
    private Button findNextButton;
    private TextInput findTextInput;
    private String lastFindText = null;
    private TimerControlledOnValueChangedEventHandler<String> updateTitleOnTextChangedHandler;
    private ConditionalEventHandler finishSearchInputEventHandler;
    private SearchableView viewComp;
    private HilitingComponent srcOfText;
    private Component<?> componentBeingViewed;
    private int maxSearchTextWidth = 10;
    private int minSearchLength = 3;
    private CanResize onViewCompResized = CanResize.NOOP;
    private Runnable copyAndHiliteResultIntoViewer = () -> {};
    
    
    public static interface SearchableView extends Dimension {

        void refreshFooter();

        int getWidth();

        boolean scrollToView(Rectangle targetToView);

        void setTitle(String message);

        Component<?> getComponentBeingViewed();

        void setComponentBeingViewed(Component<?> component);
        
        WindowViewComponent getViewPort();

        Color getColor();

        void appendOnResizeRule(CanResize resizeRule);

        void addFooterComponent(Component<?> component);
        
    }
    
    public static interface AsSearchableView {
        SearchableView asSearchableView();
    }

    public SearchFunctionForScrollableComponent(Color color, SearchableView viewComp) {
        super(color, new ButtonBar(color).setNoBorderChars());
        // its buttons and search box are TAB stops; ESC typed in the search box ends the search
        focus().whenChildFocused((child, eventBus) -> eventBus.raiseEvent(new RequestUpdateCanvasEvent(this)))
               .handleKeys(new KeyboardHandler() {
                   @Override
                   public boolean onKey(KeyEvent ke) {
                       if (!wantsKey(ke)) {
                           return false;
                       }
                       finishSearchInputEventHandler.handle(ke);
                       return true;
                   }

                   @Override
                   public boolean wantsKey(KeyEvent ke) {
                       return finishSearchInputEventHandler != null && finishSearchInputEventHandler.canHandle(ke) != null;
                   }
               });
        getWrappedComponent().setName("SearchFunctionForScrollableComponent-buttonBar");
        this.viewComp = viewComp;
        this.findButton = new Button(SearchFunctionForScrollableComponent.this.getColor(),
                                      " " + SearchFunctionForScrollableComponent.this.getColor().bold().asString() + "^F"
                                      + SearchFunctionForScrollableComponent.this.getColor().asString() + "ind");
        
        this.findButton.setHotKey(Keys.CTRL_F_KEYSTROKE);

        this.findButton.onChange((te) -> {
            setInFindMode(te);
            viewComp.refreshFooter();
        });

        busyIndicator = new TextComponent(" ");
        spinner = new StringSpinner(StringSpinner.STAR2);
        this.getWrappedComponent().addComponent(busyIndicator);
        
        this.getWrappedComponent().addComponent(findButton);
        
        findPrevButton = new Button(findButton.getColor(),"Prev");

        findPrevButton.setHotKey(Keys.CTRL_P_KEYSTROKE);
        findPrevButton.onChange((te)->{
            scrollToSearchItem(currentFindIDX - 1);
        });
        
        findNextButton = new Button(findButton.getColor(),"Next");
        findNextButton.setHotKey(Keys.CTRL_N_KEYSTROKE);
        findNextButton.onChange((te)->{
            scrollToSearchItem(currentFindIDX + 1);
        });

        findStatsDetails = new TextComponent(findButton.getColor(), "0000 of 0000");
        prevNextPanel = new HorizontalPanel(findButton.getColor());

        prevNextPanel.add(findPrevButton);
        prevNextPanel.add(new TextComponent(findButton.getColor(),"|"));

        prevNextPanel.add(findStatsDetails);
        prevNextPanel.add(new TextComponent(findButton.getColor(),"|"));
        
        prevNextPanel.add(findNextButton);
        prevNextPanel.add(new TextComponent(findButton.getColor(),"|"));
        
        onViewCompResized = new CanResize() {

            @Override
            public void resize(Dimension d) {
                maxSearchTextWidth = Math.max(10, viewComp.getWidth() / 4);
                if (findTextInput != null) {
                    findTextInput.setLength(maxSearchTextWidth);
                    getWrappedComponent().childResized(findTextInput);
                    viewComp.refreshFooter();
                }
            }
        };

        onViewCompResized.resize(viewComp);
        
    }

    
    
    public void hilite() {
        this.getWrappedComponent().hilite();
        if (findTextInput != null) {
            this.findTextInput.setColor(textInputSelectedColor());
            this.findTextInput.setEmptyColor(textInputSelectedColor());
        }
    }
    
    public CanResize getResizeRule() {
        return onViewCompResized;
    }

    /** the search field never narrows below this */
    static final int MIN_SEARCH_FIELD_WIDTH = 5;

    /**
     * Narrows the open search field by up to by columns (not below MIN_SEARCH_FIELD_WIDTH) - for a
     * footer short of room; how many columns it gave (0 when the field isn't open).
     */
    int narrowSearchField(int by) {
        if (findTextInput == null || by <= 0) {
            return 0;
        }
        int width = findTextInput.getWidth();
        int narrower = Math.max(MIN_SEARCH_FIELD_WIDTH, width - by);
        if (narrower == width) {
            return 0;
        }
        findTextInput.setLength(narrower);
        getWrappedComponent().childResized(findTextInput);
        return width - narrower;
    }

    public void unHilite() {
        this.getWrappedComponent().unHilite();
        if (findTextInput != null) {
            this.findTextInput.setColor(SearchFunctionForScrollableComponent.this.getColor());
            this.findTextInput.setEmptyColor(SearchFunctionForScrollableComponent.this.getColor());
        }
    }

    int hilitesSelectedIDX = -1;
    List<Rectangle> previoushiliteRegionsInView = null;
    ListComparator<Rectangle> hilitesComparator = new ListComparator<Rectangle>(Rectangle.DEFAULT_COMPARATOR_FULL);

    private List<Rectangle> hilitesInView() {
        hilitesSelectedIDX = -1;
        if (searchResult == null || searchResult.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Rectangle> resultsInView = new ArrayList<Rectangle>();
        Position offsetIntoSource = viewComp.getViewPort().getOffsetIntoSource();
        int height = viewComp.getHeight() - 2;  // assuming border
        int width = viewComp.getWidth() - 2;  // assuming border
        Position endOffsetIntoSource =  offsetIntoSource.offsetAdd(height,width);
   
        int idx1 = Collections.binarySearch(searchResult, offsetIntoSource, Position.UTIL.POS_COMPARE);
        if (idx1 < 0) {
            idx1 = -(idx1) - 1;
        }
        int idx2 = Collections.binarySearch(searchResult, endOffsetIntoSource, Position.UTIL.POS_COMPARE);
        if (idx2 < 0) {
            idx2 = -(idx2) - 1;
        }
        Rectangle viewPortAsRect = new Rectangle.Simple(offsetIntoSource.getRow(), offsetIntoSource.getCol(), height, width);
        
        int j = 0;
        for (int i = idx1; i<idx2; i++) {
            Rectangle current = searchResult.get(i);
            if (viewPortAsRect.intersectWith(searchResult.get(i))!=null) {
                resultsInView.add(current);
                if (i == currentFindIDX) {
                    hilitesSelectedIDX = j;
                }
                j++;
            } 
        }
        
        return resultsInView;
    }
    

    private void copyAndHiliteResultIntoViewer() {
        
          int lasthilitesSelectedIDX = hilitesSelectedIDX;
          List<Rectangle> hilitedRegionsInView = hilitesInView();

          // has the hilites list changed since last time?
          if (hilitesComparator.compare(hilitedRegionsInView, previoushiliteRegionsInView)!=0) {
                // yes.. then rebuild them
                srcOfText.clearHilights();
                int j = 0;
                for (Rectangle r : hilitedRegionsInView)  {
                   srcOfText.addHilitedRegion(r);
                   if (j == hilitesSelectedIDX) {
                       srcOfText.setSelectedIndex(j);
                   }
                   j++;
                }
          } else {
              if (lasthilitesSelectedIDX != hilitesSelectedIDX) {
                  srcOfText.setSelectedIndex(hilitesSelectedIDX);
                  srcOfText.markAsDirty();
              }
          }
          
          previoushiliteRegionsInView = hilitedRegionsInView;
          
    }
    
    
    public static void addFinderToScrollable(SearchableView viewComp, int maxSearchTextWidth) {

        SearchFunctionForScrollableComponent finder = new SearchFunctionForScrollableComponent(viewComp.getColor(),
                                                                                               viewComp);
        finder.maxSearchTextWidth = maxSearchTextWidth;

        viewComp.addFooterComponent(finder);
        
        viewComp.appendOnResizeRule(finder.getResizeRule());
    }

    public static void addFinderToScrollable(SearchableView viewComp) {
        addFinderToScrollable(viewComp, 10);
    }

    public static void addFinderToScrollable(AsSearchableView viewComp) {
        addFinderToScrollable(viewComp.asSearchableView());
    }

    private Color textInputSelectedColor() {
        return SearchFunctionForScrollableComponent.this.getColor().invert();
    }
    

    private void setInFindMode(TerminalEvent<?> te) {
        setInFindMode(te, findTextInput == null);
    }

    private void removeFindTextInput(EventBus eventBus) {
        if (SearchFunctionForScrollableComponent.this.findTextInput != null) {
            TextInput textInput = SearchFunctionForScrollableComponent.this.findTextInput;
            SearchFunctionForScrollableComponent.this.findTextInput = null;
            SearchFunctionForScrollableComponent.this.lastFindText = textInput.getText();
            eventBus.raiseEvent(new RequestReleaseFocusEvent(textInput));
            eventBus.raiseEvent(new RequestHideCursorEvent(textInput));
            SearchFunctionForScrollableComponent.this.finishSearchInputEventHandler = null;     // ESC no longer ends a search
            this.getWrappedComponent().remove(textInput);
            addRemoveNavButtonsOnSearchResults(null);
            viewComp.refreshFooter();
            eventBus.raiseEvent(new RequestUpdateCanvasEvent(this));
        }
    }
    
    private void scrollToSearchItem(int idx) {

        if (idx < 0) {
            return;
        }

        if (searchResult == null) {
            return;
        }

        if (idx >= searchResult.size()) {
            return;
        }

//        srcOfText.setSelectedIndex(idx); // TODO :  this will be broken with new implementation

        viewComp.scrollToView(searchResult.get(idx));

        currentFindIDX = idx;
        // ensure that the hiliting component marked as dirty so
        // that it redraws itself on next render
        if ( ! this.srcOfText.isDirty()) {
            this.srcOfText.markAsDirty();
        }

        findStatsDetails.setTextAndResize(String.format("%4d of %4d", (currentFindIDX+1), searchResult.size()));
        viewComp.refreshFooter();

    }
    
    private void updateFindStatsDetails() {
        findStatsDetails.setTextAndResize(String.format("%4d of %4d", (currentFindIDX+1), searchResult.size()));
        viewComp.refreshFooter();
    }


    private void setInFindMode(TerminalEvent<?> te, boolean enable) {

        if (enable) {

            attachComponentWithHilightedTextIfNeeded(viewComp, this);

            findTextInput = new TextInput(textInputSelectedColor(), maxSearchTextWidth);
            this.findTextInput.setName("findTextInput");
            this.findTextInput.setEmptyColor(textInputSelectedColor());
            if (lastFindText != null) {
                findTextInput.setText(lastFindText);
            } else {
                findTextInput.setText("");
            }
            this.getWrappedComponent().addComponent(findTextInput);

            finishSearchInputEventHandler = RequestUpdateCanvasEvent.UTIL.wrapEventHandlerWithCanvasUpdate(
                                                                new KeyInputEventHandler(Keys.ESC_KEYSTROKE, 
                                                                                         new EventHandler() {
                                                                                            @Override
                                                                                            public void handle(TerminalEvent<?> te) {
                                                                                                if (findTextInput != null) {
                                                                                                    findTextInput.setText("");
                                                                                                }
                                                                                                setInFindMode(te, false);
                                                                                                srcOfText.clearHilights();
                                                                                                te.getEventBus().asynch( (te1) -> removeFindTextInput(te1.getEventBus()));
                                                                                                lastFindText = null;
                                                                                            }
                                                                                        }));
            // (ESC reaches it through onKey while the search box is open)

            updateTitleOnTextChangedHandler = new TimerControlledOnValueChangedEventHandler<String>(findTextInput) {
                @Override
                public void valueChanged(TerminalEvent<?> te,  HasValue<String> source) {
                    //Logging.info("----------------------------------------------------");
                    ElapsedTime elapsedTime = new ElapsedTime();
                    String newValue = source.getValue();
                    setStatsMessage(newValue);

                    srcOfText.clearHilights();

                    //Logging.info("SearchFunction after clear hilites: " + elapsedTime.recalcElapsed());

                    if (newValue == null || newValue.length() < minSearchLength) {
                        setStatsMessage("");
                        addRemoveNavButtonsOnSearchResults(null);
                        te.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(SearchFunctionForScrollableComponent.this) );
                        return;
                    }

                    if (SearchFunctionForScrollableComponent.this.asynchSearchEventHandler != null) {
                        te.getEventBus().removeEventHandler(SearchFunctionForScrollableComponent.this.asynchSearchEventHandler);
                        SearchFunctionForScrollableComponent.this.asynchSearchEventHandler = null;
                    }

                    //viewComp.componentBeingViewed
                    IScreenLineSegmentsBuffer bufferToSearch = (componentBeingViewed!=null?componentBeingViewed:srcOfText).getSegmentsBuffer();

                    SearchFunctionForScrollableComponent.this.currentSearchResult = SegmentsSearchUtils.searchTextInSegmentsWithTimeLimit(null, bufferToSearch, newValue, 20);

                    //Logging.info("SearchFunction after searchTextInSegmentsAll: " + elapsedTime.recalcElapsed());

                    searchResult = currentSearchResult.currentSearchResult();
                    addRemoveNavButtonsOnSearchResults(searchResult);

                    
                    ListComparator<Rectangle> hilitesComparator = new ListComparator<Rectangle>(Rectangle.DEFAULT_COMPARATOR_FULL);
                    
                    
                    copyAndHiliteResultIntoViewer = new Runnable() {
                        List<Rectangle> previoushiliteRegionsInView = null;
                        @Override
                        public void run() {
                          List<Rectangle> hilitedRegionsInView = hilitesInView();
                          
                          if (hilitesComparator.compare(hilitedRegionsInView, previoushiliteRegionsInView)!=0) {
                                srcOfText.clearHilights();
                                for (Rectangle r : hilitedRegionsInView)  {
                                   srcOfText.addHilitedRegion(r);
                                }
                                
                          }
                          
                          previoushiliteRegionsInView = hilitedRegionsInView;
                          
                        }};

                    if (searchResult != null && searchResult.size() > 0) {

                        copyAndHiliteResultIntoViewer.run();

                        scrollToSearchItem(0);  // <-- TODO: should change this hilite closest to current position

                        //Logging.info("SearchFunction after addHilitedRegion: " + elapsedTime.recalcElapsed());

                        if (SearchFunctionForScrollableComponent.this.currentSearchResult.isComplete()) {
                            setStatsMessage("Search result: " + newValue + " (" + searchResult.size() + ")");
                            busyIndicator.setText(" ");
                        } else {
                            setStatsMessage("Search result: " + newValue + " (" + searchResult.size() + ") .. ");
                        }

                        te.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(SearchFunctionForScrollableComponent.this) );
                    } else {
                        setStatsMessage("");
                        te.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(SearchFunctionForScrollableComponent.this) );
                    }

                    if (currentSearchResult.isComplete()) {
                        return;
                    }

                    int[] asynchSearchEventHandler_invokeCounter = new int[] {0};

                    asynchSearchEventHandler = new OnIdleEventEventHandler.RepeatingIntervalWhileCond(200,
                            ()-> {
                                    if (SearchFunctionForScrollableComponent.this.currentSearchResult.isComplete()) {
                                        copyAndHiliteResultIntoViewer.run();
                                        return false;
                                    } else {
                                        return true;
                                    }
                                 },
                            (te2)-> {
                                try {
                                    SearchFunctionForScrollableComponent.this.currentSearchResult = SegmentsSearchUtils.searchTextInSegmentsWithTimeLimit(SearchFunctionForScrollableComponent.this.currentSearchResult, bufferToSearch, newValue, 75);
                                    List<Rectangle> partialResult = SearchFunctionForScrollableComponent.this.currentSearchResult.currentSearchResult();
                                    if (partialResult == null || partialResult.size() == 0) {
                                        return;
                                    }

                                    if (searchResult == null || searchResult.size()==0) {
                                        searchResult = partialResult;
                                        scrollToSearchItem(0);
                                    } else {
                                        searchResult.addAll(partialResult);
                                    }

                                    asynchSearchEventHandler_invokeCounter[0]++;
                                    // slow down screen updates
                                    if (asynchSearchEventHandler_invokeCounter[0] % 3 == 0 || SearchFunctionForScrollableComponent.this.currentSearchResult.isComplete() ) {
                                            copyAndHiliteResultIntoViewer.run();
                                            updateFindStatsDetails();
                                            addRemoveNavButtonsOnSearchResults(searchResult);

                                            if (SearchFunctionForScrollableComponent.this.currentSearchResult.isComplete()) {
                                                setStatsMessage("Search result: " + newValue + " (" + searchResult.size() + ")");
                                            } else {
                                                setStatsMessage("Search result: " + newValue + " (" + searchResult.size() + ") .. ");
                                                busyIndicator.setText(spinner.spin());
                                            }
                                            te2.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(this));
                                    }
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }) ;

                    te.getEventBus().appendIdleEventHandler(asynchSearchEventHandler);

                    //Logging.info("SearchFunction valueChanged(): "+ elapsedTime.recalcElapsed());

                }
            };

            updateTitleOnTextChangedHandler.setTimerDelayMillis(750);
            updateTitleOnTextChangedHandler.setMaxStarvationTime(20000);

            te.getEventBus().appendEventHandler(updateTitleOnTextChangedHandler);
 
            te.getEventBus().asynch((te2)-> {
                                            onViewCompResized.resize(viewComp);
                                            te2.getEventBus().asynch((t)->findTextInput.requestFocus(t));
                                          });
        } else {
            findTextInput.requestReleaseFocus(te);
            copyAndHiliteResultIntoViewer = ()->{};
            if (searchResult != null) {
                searchResult.clear();
            }
        }
        te.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(this));
    }

    private void addRemoveNavButtonsOnSearchResults(Collection<Rectangle> searchResult) {
        //
        if (searchResult != null && searchResult.size()>1) {
            if (!getWrappedComponent().containsChild(prevNextPanel)) {
                getWrappedComponent().insertComponent(0,prevNextPanel);
            }
            viewComp.refreshFooter();
        } else {
            if (getWrappedComponent().containsChild(prevNextPanel)) {
                getWrappedComponent().remove(prevNextPanel);
            }
            viewComp.refreshFooter();
        }
    }

    private void setStatsMessage(String message) {
        viewComp.setTitle(message);
    }

    private static void attachComponentWithHilightedTextIfNeeded(SearchableView viewComp, SearchFunctionForScrollableComponent finder) {

        if (finder.srcOfText != null) {
            return; // all done
        }

        if (!(viewComp.getComponentBeingViewed() instanceof HilitingComponent)) {

            Component<?> componentBeingViewed = viewComp.getComponentBeingViewed();
            
            // important!!  you must do this operation first!
            // 1) Need to remove componentBeingViewed from viewComp
            //    - But we need to pass a non-null do-nothing-component to viewComp otherwise 
            //      it will be upset 
            viewComp.setComponentBeingViewed(new BoundedComponent.DONOTHING(1,1));

            // 2) create a hiliting component with componentBeingViewed as its child
            HilitingComponent hilitingComponent = new HilitingComponent(componentBeingViewed.getColor(), componentBeingViewed);

            hilitingComponent.setFillOnClear(false);

            // attach to viewComp
            viewComp.setComponentBeingViewed(hilitingComponent);
            finder.componentBeingViewed = componentBeingViewed;

            CustomComponentDrawFunction defaultDrawFunction = viewComp.getViewPort().getSegmentsDrawFunction();

            viewComp.getViewPort().setSegmentsDrawFunction((viewPortSegments, clippedSegments)-> {
                
                finder.copyAndHiliteResultIntoViewer();
                
                defaultDrawFunction.draw(viewPortSegments, clippedSegments);
                
            });

            
            //int idx = Collections.binarySearch(finder.searchResult, new Rectangle.Simple(1,1,2,2), Rectangle.DEFAULT_COMPARATOR);

        }

        // at this point child being viewed is a HilitingComponent

        finder.srcOfText = (HilitingComponent) viewComp.getComponentBeingViewed();
    }
    
    

    

}
