package dtb.terminalIO.examples.plot.image;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.AbstractWrappedComponentAndFocusable;
import dtb.terminalIO.components.AbstractWrappedComponentWithFocusManager;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ComponentInspector.InspectorButton;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.DefaultFocusableBehaviour;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.input.HasValue;
import dtb.terminalIO.components.plot.image.ImageViewerFactory;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.ImageRenderRequest;
import dtb.terminalIO.components.plot.image.animate.ImageAnimatorComponent;
import dtb.terminalIO.components.plot.image.animate.ImageIterator;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.mouse.Clickable;
import dtb.terminalIO.examples.plot.image.animate.AbstractImageAnimator;
import dtb.terminalIO.examples.plot.image.animate.BufferedAnimate;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.Logging;

public class RenderFromGraphics2D {

    public static void main(String args[]) {

        Logging.setEnabled(true);
        
        new TUIApp(args).setDefaultMainApplicationPanel().confirmOnExit().setOnReadyStart((tApp) -> {

            ButtonBar controls = new ButtonBar();
            tApp.getMainApplicationPanel().add(controls);

            ((Component<?>)tApp.getMainApplicationPanel()).setColor(Colors.On_IBlu);
            tApp.setEmptyColors(Colors.On_IBlu);

            HorizontalPanel mainPanel = new HorizontalPanel();

            Dimension.ComputedDelegate imageViewerOptionsSize = new Dimension.ComputedDelegate((Dimension)tApp.getCanvas(), (s)->(s.getHeight()-2)/4 - 2, (s)->(int)((s.getWidth()-2)*.24));

            Logging.info("imageViewerOptionsSize: size = " + imageViewerOptionsSize);

            AnimatorSelector animatorOptionsPanel = new AnimatorSelector(new Dimension.Simple(imageViewerOptionsSize));

            mainPanel.add(animatorOptionsPanel);

            animatorOptionsPanel.add(()->new AnimateRotatingSquare(new Dimension.Simple(600,200)), tApp.getEventBus());
            animatorOptionsPanel.add(()->new BufferedAnimate(), tApp.getEventBus());
            animatorOptionsPanel.add(()->new HourGlassAnimator(), tApp.getEventBus());
            animatorOptionsPanel.add(()->new RotateEllipseAnimator(), tApp.getEventBus());

            Dimension.ComputedDelegate imageViewerSize = new Dimension.ComputedDelegate((Dimension)tApp.getCanvas(), (s)->s.getHeight()-4, (s)->s.getWidth()-4 - animatorOptionsPanel.getWidth()-2);
            Logging.info("imageViewerSize: size = " + imageViewerSize);

            BorderedComponent mainAnimatorPanel = new BorderedComponent("animator");

            Consumer<ImageIterator> setAnimator = (imageAnimator)-> {
                ImageAnimatorComponent animatorController = new ImageAnimatorComponent(Colors.BGre, imageAnimator, new Dimension.Simple(imageViewerSize), tApp.getEventBus());

                if (mainAnimatorPanel.getComponent() != null && mainAnimatorPanel.getComponent() instanceof ImageAnimatorComponent) {
                    // stop and remove previous
                    ImageAnimatorComponent prevCtrlr = (ImageAnimatorComponent) mainAnimatorPanel.getComponent();
                    ImageRenderRequest prevRsqt = prevCtrlr.getImageRenderRequest();
                    ImageRenderRequest curRqst = animatorController.getImageRenderRequest();
                    curRqst.copyFrom(prevRsqt);
                    prevCtrlr.stop();
                }
                // set and start new controller
                mainAnimatorPanel.setComponent(animatorController);
            };

            // Supplier<ImageIterator> imageAnimator
            Supplier<ImageAnimatorComponent> currentAnimator = ()-> (ImageAnimatorComponent)mainAnimatorPanel.getComponent();
            Supplier<ImageRenderRequest> currentRequest   = ()-> currentAnimator.get().getImageRenderRequest();

            mainPanel.add(mainAnimatorPanel);

            animatorOptionsPanel.setFocus(true, tApp.getEventBus());

            setAnimator.accept(animatorOptionsPanel.getValue().get());

            animatorOptionsPanel.onChange((te)-> {
                setAnimator.accept(animatorOptionsPanel.getValue().get());
            });

            controls.setNoBorderChars();

            controls.addComponent(new TextComponent("2D animated Grpahics | "));

            //DropdownListBox<T>
            DropdownListBox<ImageViewerFactory.IMAGE_RENDER_TYPE> imageRendererTypeDDLB = new DropdownListBox<ImageViewerFactory.IMAGE_RENDER_TYPE>(ImageViewerFactory.imageRenderTypeOptions());
            controls.addComponent(imageRendererTypeDDLB);
            imageRendererTypeDDLB.setSelected(0);
            imageRendererTypeDDLB.onChange((te)->{
                currentRequest.get().setImageRenderType( imageRendererTypeDDLB.getValue());
            });

            controls.addComponent(new TextComponent(" | Color map: "));

            DropdownListBox<ImageViewerFactory.RGBToAnsiColorControlCode> colorMapDDLB = new DropdownListBox<ImageViewerFactory.RGBToAnsiColorControlCode>(ImageViewerFactory.colorMap());
            colorMapDDLB.setSelected(0);
            colorMapDDLB.setColor(Colors.BIBlu);
            controls.addComponent(colorMapDDLB);
            colorMapDDLB.onChange((te)->{
                currentRequest.get().setColorMap( colorMapDDLB.getValue() );
            });

            DropdownListBox<ImageViewerFactory.RGBPlotPredicate> plotPredicatesDDLB = new DropdownListBox<ImageViewerFactory.RGBPlotPredicate>(ImageViewerFactory.plotPointPredicateOptions());
            plotPredicatesDDLB.setSelected(0);
            controls.addComponent(plotPredicatesDDLB);
            plotPredicatesDDLB.onChange((te)->{
                currentRequest.get().setToPlotOrNot(plotPredicatesDDLB.getValue());
            });

            tApp.getMainApplicationPanel().add(1,0, mainPanel);


            tApp.appendOnResizeAction((t,size)->{
                imageViewerSize.setBaseSize(size);
                mainAnimatorPanel.resize(new Dimension.Simple((imageViewerSize)));

                imageViewerOptionsSize.setBaseSize(size);
                //animatorOptionsPanel.resize(new Dimension.Simple(imageViewerOptionsSize));
            });

            FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),
                                                                                             tApp.getEventBus(),
                                                                                             ()-> new DefaultFocusManager().setNavKeysFirst(false).setCycling(false).setName("mainFocusManager"));

            tApp.getEventBus().appendEventHandler( mainFocusManager );

            controls.addComponent(new InspectorButton(Colors.IGre, tApp, (DefaultFocusManager)mainFocusManager));
            
            FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, (Component<?>)tApp.getMainApplicationPanel());

            
        }).start();

    }

    static public class AnimatorSelector  extends AbstractWrappedComponentWithFocusManager<VerticalPanel> 
                                          implements HasValue<Supplier<ImageIterator>>{

        ChainedEventHandler chainedHandler; 
        ConditionalEventHandler chainedHandlerAsConditional;

        Map<Focusable, Supplier<ImageIterator>> focusableToAnimator = new HashMap<Focusable, Supplier<ImageIterator>>();
        Dimension sizeOfEachChild;

        public AnimatorSelector(dtb.terminalIO.controlcodes.color.Color color, Dimension sizeOfEachChild) {
            super(color, new VerticalPanel());
            this.sizeOfEachChild = sizeOfEachChild;
        }

        public AnimatorSelector(Dimension sizeOfEachChild) {
            this(Colors.DEFAULT_COLOR, sizeOfEachChild);
        }

        public AnimatorSelector add(Supplier<ImageIterator> imageAnimator, EventBus eventBus) {
            ImageAnimatorComponent animatorController = new ImageAnimatorComponent(Colors.BGre, imageAnimator.get(), this.sizeOfEachChild, eventBus);
            ClickableAnimator clickable = new ClickableAnimator(animatorController);
            this.getWrappedComponent().add(clickable);
            this.addFocusable(clickable);
            this.focusableToAnimator.put(clickable, imageAnimator);
            return this;
        }

        @Override
        public void childHasCurrentFocus(Focusable focusableChild, EventBus eventBus) {
            eventBus.raiseEvent( new HasValue.ValueChangedEvent<>(this) );
            eventBus.raiseEvent( new RequestUpdateCanvasEvent(this));
        }

        HasValue.OnValueChangedEventHandler<Supplier<ImageIterator>> onChangeHandler;

        public void onChange(EventHandler consumeChange) {
            onChangeHandler = HasValue.UTIL.bindConsumerAsOnValueChangedHandler(this, onChangeHandler, getChainedEventHandler(), consumeChange);
        }

        private ChainedEventHandler getChainedEventHandler() {
            if (chainedHandler == null) {
                chainedHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);
                chainedHandlerAsConditional = chainedHandler.asConditionalEventHandler();
                setConditionalEventHandler(chainedHandlerAsConditional);
            }
            return chainedHandler;
        }

        @Override
        public Supplier<ImageIterator> getValue() {
            if (getCurrentChildFocus() == null) {
                return null;
            }
            return focusableToAnimator.get(getCurrentChildFocus());
        }

    }

    public static class ClickableAnimator extends AbstractWrappedComponentAndFocusable<BorderedComponent> implements Clickable {

        Focusable focusable = null;
        ImageAnimatorComponent animatorController;
        ClickableAnimator(ImageAnimatorComponent animatorController) {
            super(animatorController.getColor(), new BorderedComponent(animatorController));
            focusable = new DefaultFocusableBehaviour.HiliteOnFocus(this.getWrappedComponent());
        }

        @Override
        public void onClick(EventBus eventBus) {

        }
        @Override
        protected Focusable asFocusable() {
            return focusable;
        }

    }
    
    static class AnimateRotatingSquare extends AbstractImageAnimator {

        AnimateRotatingSquare(Dimension size) {
            super(size);
        }

        AnimateRotatingSquare() {
            super();
        }

        private double angle = 0;
        private double scale = 1;
        private double delta = 0.01;
        Rectangle.Float r = new Rectangle.Float(20, 20, 200, 200);
        
        protected void paint(Graphics g) {
            
            int h = getHeight();
            int w = getWidth();
            
            
            Graphics2D g2d = (Graphics2D) g;
            
            g.clearRect(0, 0, this.getWidth(), this.getHeight());

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            g2d.translate(w / 2, h / 2);
            g2d.rotate(angle);
            g2d.scale(scale, scale);

            g2d.setColor(Color.WHITE);
            g2d.fill(r);
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(r);
            
            nextImageFrame();
        }

        private void nextImageFrame() {
            if (scale < 0.01) {
                delta = -delta;
              } else if (scale > 0.99) {
                delta = -delta;
              }

              scale += delta;
              angle += 0.01;
        }

    }
    
    static class HourGlassAnimator extends AbstractImageAnimator {

        private int off;
        private int hh,mm,elapsed;
        private int h = 200;
        private int tick;

        HourGlassAnimator() {
            super();
        }

        HourGlassAnimator(Dimension size) {
            super(size);
        }

        @Override
        public boolean hasNext() {
            return off < h;
        }


        @Override
        protected void paint(Graphics g) {
            int x = 10, y = 5, w = 100;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);

            g2.clearRect(x, y, w, h * 2+20);
            g2.setColor(Color.orange);
            
            GeneralPath polygon = new GeneralPath(GeneralPath.WIND_NON_ZERO, 4);
            
            int multiplier = 2;
            
            polygon.moveTo(x, y);
            polygon.quadTo(x, h, w / multiplier, h);
            polygon.quadTo(x, h, x, h * multiplier);
            polygon.lineTo(w, h * multiplier);
            polygon.quadTo(w, h, w / multiplier, h);
            polygon.quadTo(w, h, w, y);
            polygon.closePath();
            
            Rectangle2D mask = new Rectangle2D.Double(x, y + off, w, h - off);
            Rectangle2D mask2 = new Rectangle2D.Double(x, y + 2 * h - off, w, off);
            Area a1 = new Area(mask);
            
            Area a2 = new Area(polygon);
            a2.intersect(a1);
            g2.fill(a2);
            
            a1 = new Area(mask2);
            a2 = new Area(polygon);
            a2.intersect(a1);
            g2.fill(a2);
            
            g2.setStroke(new BasicStroke(1));
            g2.setColor(new Color(0xCCCCCC));
            g2.setColor(Color.BLUE);
            g2.setStroke(new BasicStroke(2));
            g2.draw(polygon);

            int millis = ((hh * 3600 + mm * 60) * 1000) - elapsed;
            String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                    TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
            g2.drawString(hms, 10, 210);
            
            
            off = off + 1;
            elapsed = elapsed + tick;
            
            
        }
        
    }
    
    static class RotateEllipseAnimator extends AbstractImageAnimator {

        boolean doDraw = true;
        int angle = 0;
        
        RotateEllipseAnimator() {
            super(new Dimension.Simple(400,400));
        }

        
        @Override
        public boolean hasNext() {
            return doDraw;
        }


        
        @Override
        protected void paint(Graphics g) {
                Graphics2D ga = (Graphics2D)g;

                if (angle <360) {
                       ga.rotate(Math.toRadians(angle), 200, 150);
                       ga.setPaint(Color.PINK);
                       ga.fillArc(100,100,200,100, 0, 360);

                       ga.rotate(Math.toRadians(angle), 200, 150);
                       ga.setStroke(new BasicStroke(2));
                       ga.setPaint(Color.black);
                       ga.drawArc(100,100,200,100, 0, 360);

                    angle += 45;
                    
                } else {
                    ga.clearRect(0, 0, getWidth(), getHeight());
                    angle = 0;
                }
                //doDraw = false;
        }
        
        
    }
    

}
