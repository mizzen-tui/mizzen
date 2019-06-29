package dtb.terminalIO.examples.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.TerminalEvent.TYPE;
import dtb.terminalIO.events.keyboard.KeyInputEventsProducer;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.util.StringUtils;
import dtb.terminalIO.util.Stty;

public class DrawingBoardDispatch {

    DrawingBoard drawingBoard;
    
    EventBus eventBus;
    
    Runnable terminateTask;
    
    public DrawingBoardDispatch(EventBus eventBus) {
        drawingBoard = new DrawingBoardImpl(eventBus);
        this.eventBus = eventBus;
        
        
        terminateTask = new Runnable() {
            
            @Override
            public void run() {
                eventBus.stopConsuming();
                drawingBoard.refresh();

                try {
                    Stty.setCooked();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        
        EventHandler terminateEventHandler = new EventHandler() {

            @Override
            public void handle(TerminalEvent<?> te) {
                
                terminateTask.run();

                if (te.getType().equals(TYPE.EXCEPTION)) {
                    System.out.println("\n");
                    ((Exception) te.getEventData()).printStackTrace();
                } else {
                    // next refresh() is necessary to get cursor to be below
                    // rendered canvas
                    drawingBoard.refresh();
                }
                return;
            }
        };
        
        eventBus.appendEventHandler(new ConditionalEventHandler.Impl(new IS_Event_TYPE(TYPE.EXCEPTION), terminateEventHandler));
        
        
    }

    static int DISPATCH_CONTINUE=1;
    static int DISPATCH_STOP=2;
    
    public int dispatch(String args[]) {

        if (args == null || args.length == 0) {
            return DISPATCH_CONTINUE;
        }

        if (args[0].equalsIgnoreCase("STOP!")) {
            return DISPATCH_STOP;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            drawingBoard.clear();
        }

        // System.out.println("Got here! "+ args[0]);
        
        // canvas_box MSGAREA 1 1 20 10
        if (args.length == 6 && args[0].equalsIgnoreCase("box")) {
            String name = args[1];
            Position pos = new Position.Simple(Integer.parseInt(args[2]), Integer.parseInt(args[3]));
            Dimension dim = new Dimension.Simple(Integer.parseInt(args[4]), Integer.parseInt(args[5]));
            drawingBoard.component(name, pos, dim, true);
        }

        // canvas_title MSGAREA "My first title"
        if (args.length == 3 && args[0].equalsIgnoreCase("title")) {
            String name = args[1];
            String title = args[2];
            drawingBoard.title(name, title);
        }

        // append_text MSGAREA "The rain in spain stays mainly on the plain"
        if (args.length == 3 && args[0].equalsIgnoreCase("append_text")) {
            String name = args[1];
            String text = args[2];
            text = text == null ? text : text.replace("\\n","\n");
            drawingBoard.appendText(name, text);
        }
        // canvas_box_clear MSGAREA
        if (args.length == 2 && args[0].equalsIgnoreCase("box_clear")) {
            String name = args[1];
            drawingBoard.componentClear(name);
        }
        
        // feed_pipe canvas_auto_refresh on
        if (args.length == 2 && args[0].equalsIgnoreCase("auto_refresh")) {
            boolean flag = "on".equalsIgnoreCase(args[1]) || "true".equalsIgnoreCase(args[1]) || "yes".equalsIgnoreCase(args[1]);
            drawingBoard.setAutoRefresh(flag);
        }
        // feed_pipe canvas_piped_text MSGAREA "\"ls -l\""
        if (args.length == 3 && args[0].equalsIgnoreCase("piped_text")) {
            String name = args[1];
            String command = args[2];
            try {
                String[] cmd = parseStrToARGS(command);
                //cmd = new String[] {"/bin/bash","-c","ls -l"};
                //drawingBoard.appendText(name, "\n"+Arrays.toString(cmd)+"\n");
                drawingBoard.pipeTextFromCommand(name, cmd);
            } catch (IOException e) {
                String error = String.format(Colors.IRed.asString()+"Failure for piped_text for command %s\nException\n%s",command, e);
                drawingBoard.appendText(name, error);
            }
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("refresh")) {
            drawingBoard.refresh();
        }
        
        
        return DISPATCH_CONTINUE;
    }

    public static String demoString() {
        String commands = "\n" + "canvas_clear\n" + 
                          "\n" + 
                          "canvas_box MSGAREA 1 1 10 60\n" + 
                          "canvas_title MSGAREA \"My first title\" \n" + 
                          "canvas_append_text MSGAREA \"The "+Colors.Blu.asString()+"rain "+ControlCode.RCol.asString()+"in spain \\nstays mainly on the plain\"\n" +
                          "canvas_append_text MSGAREA \"And where's that blasted plain?\"\n"+
                          "STOP!";

        return commands;
    }

//    Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(str);
//    while (m.find())
//        list.add(m.group(1)); // Add .replace("\"", "") to remove surrounding quotes.
//    
    
    public static void test1(String[] args) throws Exception {

        DrawingBoardDispatch dispatch = new DrawingBoardDispatch(new EventBus());
        
        StringBuffer sb = new StringBuffer();
        
        Consumer<String[]> buildStream = new Consumer<String[]>() {

            @Override
            public void accept(String[] t) {
                for (String s : t) {
                    if (s.contains(" ")) {
                        sb.append("\"");
                    }
                    sb.append(s);
                    if (s.contains(" ")) {
                        sb.append("\"");
                    }
                    sb.append(" ");
                }
                sb.append("\n");
                
            }
            
        };

        //buildStream.accept(new String[] { "clear" });

        //buildStream.accept(new String[] { "
            buildStream.accept(new String[] { "canvas_clear" });
            
            buildStream.accept(new String[] { "canvas_box","MSGAREA","1","1","10", "60" });
            
            
            //buildStream.accept(new String[] { "canvas_title MSGAREA "\"${Red}My first title\"" 

            buildStream.accept(new String[] { "canvas_title", "MSGAREA", "My first title" });
            
            //#sleep 1
            
            buildStream.accept(new String[] { "canvas_append_text","MSGAREA", "\\n   The rain in ${Blu}spain${RCol} stays mainly on the plain\\nNewline testS"});
            
            //#sleep 1
            
            buildStream.accept(new String[] { "canvas_append_text","MSGAREA","\\nAnd where is that blasted plain?"});
            
            // #sleep 1
            
            buildStream.accept(new String[] { "canvas_append_text", "MSGAREA", "\\nIn Spain .."});
            
            //#sleep 1
            
            buildStream.accept(new String[] { "canvas_append_text", "MSGAREA", "In Spain .." });
            
            //#sleep 1        
            buildStream.accept(new String[] { "canvas_printf", "MSGAREA", "Window size= $(tput lines) $(tput cols)"});
            
            //#sleep 1
            
            buildStream.accept(new String[] { "canvas_piped_text", "MSGAREA", "/bin/bash -c 'ls -l'"});

            buildStream.accept(new String[] { "canvas_ok_msg", "Here is a popup message"});
        
            buildStream.accept(new String[] { "canvas_STOP!" });
            
            dispatch.parseInputStream( new BufferedReader(new StringReader( sb.toString() ))  );
        
    }
    
    public static void test(String[] args) throws IOException {
        // System.out.println("Hello world");
        int i = 10;

        DrawingBoardDispatch dispatch = new DrawingBoardDispatch(new EventBus());

        dispatch.dispatch(new String[] { "clear" });

        dispatch.dispatch(new String[] { "box", "MSGAREA", "10", "5", "10", "30" });

        dispatch.dispatch(new String[] { "title", "MSGAREA", Colors.Yel.asBold().asString() + "Here is the title" });

        dispatch.dispatch(new String[] { "append_text", "MSGAREA", "1" });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", "2" });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", "3" });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", "4" });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", "5" });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", "6" });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", "7" });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", "8" });

        if (i==10) return;

        dispatch.dispatch(new String[] { "append_text", "MSGAREA", "\ntext1" });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", "[---]" });
        
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", "text2" });
        
        if (i==10) return;
        
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", " " });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", "text3" });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", " " });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", "text4" });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", " " });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", "text5" });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", " " });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", "text6" });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", " " });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", "textSEVEN" });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", " " });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", "textEIGHT" });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", " " });
      //  dispatch.dispatch(new String[] { "append_text", "MSGAREA", Colors.Red.asString() + "textNINE" });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", " " });
      //  dispatch.dispatch(new String[] { "append_text", "MSGAREA", Colors.BBlu.asString() + "text10\nand\nELEVEN" });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", " " });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", "12" });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", " " });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", "13 and 14 and\n15" });
        dispatch.dispatch(new String[] { "append_text", "MSGAREA", " " });
      //  dispatch.dispatch(new String[] { "append_text", "MSGAREA", Colors.BCya.asString() + "[DONE!]" });
        
        
        dispatch.parseInputStream( new BufferedReader(new StringReader(demoString() ))  );
        
    }

    
    private String convertEscapes(String in) {
        // TODO:  Need to make sure we are in fact looking at a color sequence
        //        use regex to do that!
        return in.replace("\\e", ControlCode.escape_CHR);
    }
    
    static Pattern argsPattern = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");
    
    private String[] parseStrToARGS(String msg) {
        
        StringUtils.ArgumentTokenizer tkn = new StringUtils.ArgumentTokenizer();
        List<String> argsList = tkn.tokenize(msg);
        
//        
//        List<String> argsList = new ArrayList<String>();
//        Matcher m = argsPattern.matcher(msg);
//        while (m.find()) {
//            String arg = m.group(1);
//            if (arg.startsWith("canvas_")) {
//                arg = arg.substring("canvas_".length());
//            }
//            if (arg.startsWith(quoteChar) && arg.endsWith(quoteChar)) {
//                arg = arg.substring(1, arg.length()-1);
//                arg = arg.replace("\\n", "\n");
//            }
//            argsList.add(arg);
//        }
        if (argsList.size()>0) {
            String cmd = argsList.get(0);
            if (cmd.startsWith("canvas_")) {
                cmd  = cmd.substring("canvas_".length());
                argsList.set(0, cmd);
            }
        }
        String args[] =  argsList.toArray(new String[]{});
        return args;
    } 
    
    //boolean tmp= true;
    boolean tmp= false;
    protected int parseInputStream(BufferedReader b_in) throws IOException {
        String msg;
       // Pattern argsPattern = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");
        while ((msg = b_in.readLine()) != null) {

            if (tmp) { 
                System.out.println("msg = "+msg);
                if (msg != null && msg.equalsIgnoreCase("STOP!")) {
                    return DISPATCH_STOP;
                }
                return DISPATCH_CONTINUE;
            }
            
            String line = convertEscapes(msg);
            if (line.length()==0 || !line.startsWith("canvas_")) {
                continue;
            }
            
            String args[] = parseStrToARGS(line);
            int result = dispatch(args);
            if (result == DISPATCH_STOP) {
                return result;
            }
        }
        return DISPATCH_CONTINUE;
    }
    
    // TODO:   This should be enqueing to a QUEUE  ...
    //         also break the parse from the dispatch logic
    //         and have a separate thread for all GUI activities
    //         this allows separate (piped /spawnded) tasks from
    //         processing input and enqueu renderig changes to the queue
    //         similat to an SwingUtils invokerLater .. idea.
    
    public void readFromNamedPipe(String pipeName) throws IOException {
        File pipeFile = new File(pipeName);
        int result = DISPATCH_CONTINUE;
        
        FileReader in = new FileReader(pipeFile);

        BufferedReader b_in = new BufferedReader(in);
        
        while (result == DISPATCH_CONTINUE) {
            result = parseInputStream(b_in);
            
            if (result == DISPATCH_CONTINUE) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
        }
        cleanupTasks();
    }
    
    public void cleanupTasks() {
        terminateTask.run();
    }
    

    public static void main(String[] args) throws Exception {
        //System.out.println("HELLOW WORLD!!!!!!!!!!!");
        if (args!=null && args.length == 2 && args[0].equalsIgnoreCase("PIPE")) {
            //System.out.println("Reading from pipe "+args[1]);
            EventBus eventBus = new EventBus();
            new KeyInputEventsProducer(eventBus);
            new DrawingBoardDispatch(eventBus).readFromNamedPipe(args[1]);
        } else {
            test1(args);
        }
    }

}
