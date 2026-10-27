package mizzen.events.keyboard;

import mizzen.events.EventBus;
import mizzen.events.TerminalEvent;
import mizzen.events.TerminalEvent.IS_Event_TYPE;

public class InputBytesHelper {

    // convenience class for maintaining a growable bytebuffer. Maybe I should be
    // using ByteBuffer?
    static class BytesContainer {

        static final int SIZE_INCR = 4096;

        public byte[] bytes = new byte[SIZE_INCR];
        int byteIndex = 0;

        void appendByte(byte b) {
            if (byteIndex < bytes.length) {
                bytes[byteIndex] = b;
                byteIndex++;
            } else {
                expand();
                appendByte(b);
            }
        }

        byte[] capturedAsBytes() {
            byte[] result = new byte[byteIndex];
            for (int i = 0; i < byteIndex; i++) {
                result[i] = bytes[i];
            }
            return result;
        }

        public void expand() {
            byte[] tempArray = new byte[bytes.length + SIZE_INCR];
            System.arraycopy(bytes, 0, tempArray, 0, bytes.length);
            bytes = tempArray;
        }
    }

    public static void captureAndStoreKeyEvent(BytesContainer bytes, KeyEvent ke) {
        // special case enter
        if (ke.isKeyStroke(Keys.ENTER_KEYSTROKE)) {
            bytes.appendByte((byte) '\n');
            return;
        }

        KeySequence[] keySeq = ke.getKeyStroke().getKeys();

        for (int key : keySeq[0].keys) {
            if (key < 255) {
                bytes.appendByte((byte) key);
            }
        }
    }

    static class CaptureKeysEventBus extends EventBus.Delegate {

        IS_Event_TYPE evtTYPE = new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS);

        BytesContainer bytes = new BytesContainer();

        public CaptureKeysEventBus(EventBus delegate) {
            super(delegate);
        }

        public EventBus raiseEvent(TerminalEvent<?> te) {
            if (!evtTYPE.test(te)) {
                delegate.raiseEvent(te);
                return this;
            }

            KeyEvent ke = (KeyEvent) te;
            captureAndStoreKeyEvent(bytes, ke);
            return this;
        }

        byte[] capturedAsBytes() { 
            return bytes.capturedAsBytes();
        }
    
        String capturedAsString() {
            return new String(capturedAsBytes());
        }


    }

}
