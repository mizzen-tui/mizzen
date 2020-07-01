package dtb.terminalIO.examples.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.CanvasComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HasTitleAndBorder;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.traverse.HasChildren;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.TerminalDimensions;

public class DrawingBoardImpl implements DrawingBoard {

    //private Canvas canvas;
    //private TerminalScreen screen;
    private CompositeWithNamedComponents components;
    private boolean autoRefresh = true;
    private EventBus eventBus;
    private Consumer<Component<?>> refreshAction;

    
    static class CompositeWithNamedComponents<T extends Composite> implements Component<T>, Rectangle {

        Composite componentManager;
        Map<String,Component<?>> nameComponentMap;
        private ParentContainer parentContainer;
        private Position canvasOffset;

        
        protected CompositeWithNamedComponents(Color color) {
            componentManager = new Composite(color);
            nameComponentMap = new HashMap<String,Component<?>>();
        }

        protected CompositeWithNamedComponents() {
            this(FGBGColors.NO_COLOR_SPECIFIED);
        }
        
        protected Component<?> getNamedComponent(String name) {
            Component<?> component = nameComponentMap.get(name);
            if (component == null) {
                throw new IllegalArgumentException("Component named "+name+" not found");
            }
            return component;
        }

        public void add(String name, Component<?> component) {
            componentManager.add(component);
            nameComponentMap.put(name, component);
        }
        
        public void add(String name, Position offset,  Component<?> component) {
            componentManager.add(offset, component);
            nameComponentMap.put(name, component);
        }
        
        @Override
        public void setParentContainer(ParentContainer parentContainer) {
            if (parentContainer != null && this.getParentContainer() != null && this.getParentContainer() != parentContainer) {
                throw new IllegalStateException("Parent container already set to another component");
            }
            this.parentContainer = parentContainer;

            Component.UTIL.markParentDirtyIfChildDirty(this);
            
        }
        
        public ParentContainer getParentContainer() {
            return this.parentContainer;
        }
        
        @Override
        public Position getCanvasOffset() {
            return this.canvasOffset;
        }
        
        @Override
        public void setCanvasOffset(Position canvasOffset) {
            this.canvasOffset = canvasOffset;
        }
        
        
        @Override
        public int getCol() {
            return componentManager.getCol();
        }

        @Override
        public int getRow() {
            return componentManager.getRow();
        }

        @Override
        public int getWidth() {
            return componentManager.getWidth();
        }

        @Override
        public int getHeight() {
            return componentManager.getHeight();
        }

        @Override
        public void moveTo(int row, int col) {
            componentManager.moveTo(row, col);
        }

        @Override
        public IScreenLineSegmentsBuffer getSegmentsBuffer() {
            return componentManager.getSegmentsBuffer();
        }

        @Override
        public Color getColor() {
            return componentManager.getColor();
        }

        @Override
        public void clear() {
            componentManager.clear();
            
        }

        public boolean isDirty() {
            return componentManager.isDirty();
        }
        
        
        @Override
        public void markAsDirty() {
            componentManager.markAsDirty();
        }

        @Override
        public T setColor(Color drawingcolor) {
            return (T) componentManager.setColor(drawingcolor);
        }

        public void replace(String name, Component<?> c1, Component<?> c2) {
            // TODO Auto-generated method stub
            if (this.nameComponentMap.get(name) != c1) {
                throw new IllegalArgumentException("replace(): name does not match component");
            }
            this.componentManager.replace(c1, c2);
            this.nameComponentMap.put(name, c2);
        }
        
    }
    
    public DrawingBoardImpl(EventBus eventBus) {
        this.eventBus = eventBus;
        init(System.out, null);
    }
    public DrawingBoardImpl(EventBus eventBus, Consumer<Component<?>> refreshAction) {
        this.eventBus = eventBus;
        init(System.out, refreshAction);
    }
    
    void  init(PrintStream output, Consumer<Component<?>> refreshAction) {
        
        this.components = new CompositeWithNamedComponents();

        if (refreshAction == null) {
            TerminalScreen screen = new TerminalScreen();
            screen.setPrintStream(output);
            Dimension size = null;
            size = TerminalDimensions.create();
            Canvas canvas = new Canvas(screen);
            canvas.setEmptyChar(' ');
            canvas.setSize(size.getHeight() - 1, size.getWidth() - 1);
            
            refreshAction = new Consumer<Component<?>>() {
                @Override
                public void accept(Component<?> drawingBoardComponents) {
                    canvas.clearCanvas();
                    canvas.addComponentSegments(components);
                    canvas.refresh();
                }
                
            };
        }
        
        this.refreshAction = refreshAction;
    }
    
    public void refresh() {
        this.components.markAsDirty();
        this.refreshAction.accept(this.components);
    }
    
    void autoRefresh() {
        if (autoRefresh) {
            refresh();
        }
    }
    
    
    @Override
    public void component(String name, Position pos, Dimension d) {
        TextComponent component = new TextComponent(FGBGColors.NO_COLOR_SPECIFIED, d, "");
        this.components.add(name, pos, component);
        autoRefresh();
    }
    
    @Override
    public void component(String name, Position pos, Dimension dim, boolean hasBorder) {
        if (!hasBorder) {
            // builds a simple TextComponent
            component(name, pos, dim);
            return;
        }

//        Composite c = new Composite();
//        c.setBorder(true);
//        c.setBounds(dim);
//        c.resize(dim);
//        this.components.add(name, pos, c);
//        autoRefresh();

        CanvasComponent childComponent = new CanvasComponent(dim);
//        TextComponent childComponent = new TextComponent(FGBGColors.NO_COLOR_SPECIFIED, dim);
        BorderedComponent c = new BorderedComponent(FGBGColors.NO_COLOR_SPECIFIED, childComponent);
        c.resize(dim);
        this.components.add(name, pos, c);
        autoRefresh();
    }
    
//    @Override
//    public void scrollableComponent(String name, Position pos, Dimension dim) {
//
//        CanvasComponent childComponent = new CanvasComponent(new Dimension.Simple(dim.getHeight()-2, dim.getWidth()));
////        TextComponent childComponent = new TextComponent(FGBGColors.NO_COLOR_SPECIFIED, dim);
//        ScrollableComponent sc = new ScrollableComponent(FGBGColors.NO_COLOR_SPECIFIED, dim, componentBeingViewed)
//        BorderedComponent c = new BorderedComponent(FGBGColors.NO_COLOR_SPECIFIED, childComponent);
//        c.resize(dim);
//        this.components.add(name, pos, sc);
//        autoRefresh();
//    }
    
    

    @Override
    public void component(String name, Position pos, Dimension dim, String title) {
        Composite c = new Composite();
        c.setBounds(dim);
        c.setTitle(title);
        this.components.add(name, pos, c);
        autoRefresh();
    }

    @Override
    public void title(String name, String title) {
        Component<?> c = this.components.getNamedComponent(name);
        if (c instanceof HasTitleAndBorder<?>) {
            ((HasTitleAndBorder<?>) c).setTitle(title);
        } else {
            if (c instanceof BoundedComponent) {
                BorderedComponent bc = new BorderedComponent((BoundedComponent<?>)c);
                bc.setTitle(title);
                this.components.replace(name, c, bc);
            } else  {
                Composite composite = new Composite(c.getColor());
                composite.add(c);
                composite.setTitle(title);
                this.components.replace(name, c, composite);
            }
        }
        autoRefresh();
    }

    @Override
    public void textAt(String name, String position) {
        // TODO Auto-generated method stub
        
    }

    
    public void TTappendText(String name, String text) {
        appendText(name, this.components.getNamedComponent(name), text);
        autoRefresh();
    }

    @Override
    public void appendText(String name, String text) {
        appendText(name, this.components.getNamedComponent(name), text);
        autoRefresh();
    }
    
    public void componentClear(String name) {
        
        Logging.info("componentClear "+ name);
        
        Component c = this.components.getNamedComponent(name);
        if (c instanceof TextComponent) {
            TextComponent tc = (TextComponent) c;
            tc.clear();
            autoRefresh();
            Logging.info("TextComponent "+ name);
            return;
        }
        
        if (c instanceof Composite) {
            ((Composite) c).clearAndRemoveAllChildren();
            Logging.info("clearAndRemoveAllChildren()" + name);
            autoRefresh();
            return;
        }
        
        if (c instanceof BorderedComponent) {
            Component<?> child_c = ((BorderedComponent)c).getComponent();
            child_c.clear();
            autoRefresh();
            Logging.info("BorderedComponent "+ name + " child class = " + child_c.getClass().getSimpleName());

            return;
        }
        if (c instanceof CanvasComponent) {
            ((CanvasComponent)c).clear();
            autoRefresh();
            Logging.info("CanvasComponent "+ name);
            return;
        }
    }
    
    protected void appendText(String name, Component<?> c, String text) {
        if (c instanceof TextComponent) {
            TextComponent tc = (TextComponent) c;
            String tc_text = tc.getText();
            tc.setText(tc_text + text);
            return;
        }
        if (c instanceof Composite) {
            ((Composite)c).append(new TextComponent(text));
            return;
        }
        if (c instanceof BorderedComponent) {
            Component<?> child_c = ((BorderedComponent)c).getComponent();
            appendText(name, child_c, text);
            return;
        }
        if (c instanceof CanvasComponent) {
            ((CanvasComponent)c).addTextHandleColors(text);
            return;
        }
    }
    

    @Override
    public void pipeTextFromCommand(String name, String[] cmd) throws IOException {
        String newLine = System.getProperty("line.separator");
//        appendText(name, newLine + "Executing : "+commandText);
//        String[] cmd = commandText.split(" ");
//        cmd = new String[] {"/bin/bash","-c","ls -l"};
        
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                try {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                //String line = null;
                
                reader.lines().forEach( (line) -> { 
                                         TTappendText(name, newLine + line);
                	                     //System.out.println("OUTSIDE " + line); 
//                	                     new OnIdleEventEventHandler.OneTime((e)-> {
//                	                         //System.out.println("INSIDE " + line);
//                	                         TTappendText(name, newLine + line);
//                	                         });
                	                     
                }  );
                
//                
//                while ( (line = reader.readLine()) != null) {
//                   eventBus.addEventHandler(  new OnIdleEventEventHandler.OneTime((e)-> appendText(name, newLine + e.getEventData()), eventBus) );
//                   //appendText(name, newLine + line);
//                }
                } catch (IOException e) {
                    System.out.println(e); // toodo:  throw exception on eventbus - need access to eventbus
                }
            }
        }).start();
        
//        
//        ProcessBuilder pb = new ProcessBuilder(cmd);
//        Process process = pb.start();
//
//        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//        String line = null;
//        while ( (line = reader.readLine()) != null) {
//           appendText(name, newLine + line);
//        }        
    }

    @Override
    public void clear() {
        this.components.clear();
        autoRefresh();
    }

    @Override
    public void setAutoRefresh(boolean autoRefresh) {
        this.autoRefresh = autoRefresh;
    }

}
