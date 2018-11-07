package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

public class GlobalSnapshot implements Serializable {
    private int count = 0;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
