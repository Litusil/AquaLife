package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class NameResolutionResponse implements Serializable {
    private InetSocketAddress source;
    private String requestId;

    public NameResolutionResponse(InetSocketAddress source, String requestId) {
        this.source = source;
        this.requestId = requestId;
    }

    public InetSocketAddress getSource() {
        return this.source;
    }

    public String getRequestId() {
        return requestId;
    }
}
