package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

public class SnapshotToken implements Serializable {
    private int amountOfFishies = 0;
    public void addFishies(int amount){
        amountOfFishies += amount;
    }

    public int getAmountOfFishies() {
        return amountOfFishies;
    }
}
