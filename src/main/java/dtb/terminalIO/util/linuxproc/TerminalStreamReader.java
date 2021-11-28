package dtb.terminalIO.util.linuxproc;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.function.Predicate;

public class TerminalStreamReader {

    private TerminalStreamOutputParser parser;
    Predicate<InputStream> isActive;
    //boolean isEOF;
    char buffer[] = new char[1024];
    boolean readAsych;

    public TerminalStreamReader(TerminalStreamOutputParser parser, Predicate<InputStream> isActive, boolean readAsych) {
        super();
        this.parser = parser;
        this.isActive = isActive;
        //this.isEOF = false;
        this.buffer = new char[1024];
        this.readAsych = readAsych;
    }

    public TerminalStreamReader(TerminalStreamOutputParser parser) {
        this(parser, (in)-> true, false);
    }

    public void readAll(InputStream inputStream) {
        if (this.readAsych) {
            readAllAsynch(inputStream);
        } else {
            readAllSynchronously(inputStream);
        }
    }

//    private void readAllAsynchOLD(InputStream inputStream) {
//
//        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
//
//            boolean isEOF = false;
//            while (isActive.test(inputStream) && !isEOF && !Thread.interrupted()) {
//                int available = inputStream.available();
//                if (available > 0 && reader.ready()) {
//                    int count = reader.read(buffer, 0, Math.min(available / 2, buffer.length));
//                    isEOF = (count < 0);
//                    if (isEOF) break; // todo - need to ensure that pending gets output at end of loop
//                    parser.parseInput(buffer,  count);
//                } else {
//                    Thread.sleep(100);
//                }
//            }
//
//        } catch (Exception e) {
//            if (isActive.test(inputStream)) {
//                throw new RuntimeException(e);
//            }
//        }
//
//    }

    private void readAllAsynch(InputStream inputStream) {

        
        char[] buffer = new char[1024];
        boolean isEOF = false;

        try (Reader reader = new InputStreamReader(inputStream)) {
            
            while (isActive.test(inputStream) && !isEOF && !Thread.interrupted()) {
                int available = 2; // inputStream.available();
                if (available > 0) {
                    int readIn = reader.read(buffer, 0, buffer.length);

                    isEOF = readIn == -1;
                    if (!isEOF) {
                        parser.parseInput(buffer,  readIn);
                    }

                } else {
                    Thread.sleep(50);
                }
            }
            
        } catch (Exception e) {
            if (isActive.test(inputStream)) {
                throw new RuntimeException(e);
            }
            
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                if (isActive.test(inputStream)) {
                    throw new RuntimeException(e);
                }
            }
        }


    }

    private  void readAllSynchronously(InputStream inputStream) {

        try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream)) {

            int data = inputStreamReader.read();
            while (data != -1) {
                parser.parseInput((char)data);
                data = inputStreamReader.read();
            }
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public Runnable asRunnable(InputStream inputStream) {
        return ()-> readAll(inputStream);
    }

}
