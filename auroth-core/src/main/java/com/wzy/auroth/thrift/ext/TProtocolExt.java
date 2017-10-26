package com.wzy.auroth.thrift.ext;

import com.facebook.nifty.core.RequestContext;
import com.facebook.nifty.core.RequestContexts;
import com.wzy.auroth.core.AurothConstants;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolDecorator;

import java.util.Map;

public class TProtocolExt extends TProtocolDecorator {

    private TProtocol delegate;

    /**
     * Encloses the specified protocol.
     *
     * @param protocol All operations will be forward to this protocol.  Must be non-null.
     */
    public TProtocolExt(TProtocol protocol) {
        super(protocol);
        this.delegate = protocol;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void writeMessageBegin(TMessage tMessage) throws TException {
        super.writeMessageBegin(tMessage);
        RequestContext currentContext = RequestContexts.getCurrentContext();
        if (currentContext != null) {
            Map<String, String> headers =
                    (Map<String, String>) currentContext.getContextData(AurothConstants.REQUEST_HEADERS);
            if (headers != null) {
                THeaderProtocol headerProtocol = new THeaderProtocol(this.delegate);
                headerProtocol.writeHeaders(headers);
            }
        }
    }
}
