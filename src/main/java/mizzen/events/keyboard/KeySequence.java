package mizzen.events.keyboard;

import java.util.Arrays;
import java.util.StringJoiner;

public class KeySequence {
    int[] keys;

    public KeySequence(int... keys) {
        this.keys = keys;
    }

    public KeySequence(int key) {
        this(new int[] { key });
    }

    public boolean startsWith(KeySequence prefix) {
        int plen = prefix.getLength();
        if (plen > getLength() || keys == null)
            return false;
        for (int i = 0; i < plen; i++) {
            if (this.keys[i] != prefix.keys[i]) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        StringJoiner sj = new StringJoiner(",");
        for (int i = 0; i < keys.length; i++) {
            sj.add(((int) keys[i]) + "");
        }
        sb.append("keys : (" + sj.toString() + ")");
        return sb.toString();
    }

    public int[] getKeys() {
        return keys;
    }

    public int getLength() {
        return this.keys == null ? 0 : this.keys.length;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(keys);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        KeySequence other = (KeySequence) obj;
        if (!Arrays.equals(keys, other.keys))
            return false;
        return true;
    }

}