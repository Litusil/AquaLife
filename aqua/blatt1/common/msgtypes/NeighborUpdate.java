package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class NeighborUpdate implements Serializable {
    private final InetSocketAddress rightNeighbor;
    private final InetSocketAddress leftNeighbor;

    public NeighborUpdate (InetSocketAddress rightNeighbor,InetSocketAddress leftNeighbor)
    {
        this.rightNeighbor = rightNeighbor;
        this.leftNeighbor = leftNeighbor;
    }

    public InetSocketAddress getRightNeighbor() {
        return rightNeighbor;
    }

    public InetSocketAddress getLeftNeighbor() {
        return leftNeighbor;
    }
}
