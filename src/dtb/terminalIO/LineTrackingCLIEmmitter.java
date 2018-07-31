package dtb.terminalIO;

/**
 * A CLIEmmitter that tracks the # of lines emitted (output) so far
 */
public class LineTrackingCLIEmmitter implements CLIEmitter {

    int lineCounter = 0;
    boolean nextOutputIncsLineCounter = true;
    
    TerminalContext ctx;
    
    public LineTrackingCLIEmmitter(TerminalContext ctx)
    {
        this.ctx = ctx;
    }
    
    public LineTrackingCLIEmmitter()
    {
        this(TerminalContext.DEFAULT);
    }
    
    private void output(Renderer r)
    {
        r.render(this.ctx);
    }

    protected void outputStr(String line)
    {
    	if (nextOutputIncsLineCounter)
    	{
    		nextOutputIncsLineCounter = false;
    		lineCounter ++;
    		// ctx.write(DEBUGCTR());
    	}
        ctx.write(line);
    }
    
    String DEBUGCTR()
    {
    	return String.format("[%4d]: ",lineCounter);
    }
    
    protected void CR()
    {
    	output(ControlCode.ERASE_END_OF_LINE);
        outputStr("\n");
        nextOutputIncsLineCounter = true;
    }
    
    private void resetLineCounter()
    {
        lineCounter = 0;
        nextOutputIncsLineCounter = true;
    }
    
    public void moveUpToLastKnownTopRow()
    {
    	if (lineCounter<1)
    	{
    		resetLineCounter();
    		return;
    	}
        output(new ControlCode.CursorUp(lineCounter-1));
        resetLineCounter();
    }
    
    public void clearLastTrackedLines()
    {
    	clearLastTrackedLines(true);
    }
    
    public void clearLastTrackedLines(boolean erasePreviousContent)
    {
    	if (lineCounter<1)
    	{
            resetLineCounter();
    		return;
    	}
    	output(ControlCode.LF);         // move to start of line - goal is to get cursor to the left
    	if (erasePreviousContent)
    	   output(ControlCode.ERASE_LINE); // erase current line
    	if (lineCounter>1)
    	{
    		if (erasePreviousContent)
               output(new ControlCode.CursorUpAndClearLines(lineCounter ));
    		else 
    			output(new ControlCode.CursorUp(lineCounter));
    	}
        resetLineCounter();
    }
    
    @Override
    public void print(String s)
    {
        if (s==null)
        {
            outputStr("NULL");
            return;
        }
        // split is causing me grief - that's why all these checks - I'm probably being stupid
        if (!s.contains("\n"))
        {
            outputStr(s);
            return;
        }
        if ("\n".equals(s))
        {
            CR();
            return;
        }
        String[] lines = s.split("\n");
        for (int i=0; i<lines.length; i++)
        {
            outputStr(lines[i]);
            if (i<lines.length)
            {
                CR();
            }
        }
    }

    @Override
    public void println(String s)
    {
        print(s+"\n");
    }
}