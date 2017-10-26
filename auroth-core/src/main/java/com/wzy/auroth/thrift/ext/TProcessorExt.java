package com.wzy.auroth.thrift.ext;

import com.facebook.nifty.core.RequestContext;
import com.facebook.nifty.core.RequestContexts;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolDecorator;

import java.util.Map;

/**
 * TProcessor委派对象，扩展原有TProcessor功能添加，读取请求头的方法
 */
public class TProcessorExt implements TProcessor {

    private final TProcessor delegate;

    public TProcessorExt(TProcessor delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean process(TProtocol in, TProtocol out) throws TException {
        //读请求头
        TMessage message = in.readMessageBegin();
        //delegate headers
        THeaderProtocol headerProtocol = new THeaderProtocol(in);
        //请求发起判断
        if (message.type == TMessageType.CALL || message.type == TMessageType.ONEWAY) {
            //读取请求头
            //请求头
            Map<String, String> headers = headerProtocol.readHeaders();
            if (headers != null) {
                //放入请求上下文
                RequestContext currentContext = RequestContexts.getCurrentContext();
                currentContext.setContextData("requestHeaders", headers);
            }
        }

        //服务端方法执行
        return this.delegate.process(new StoredMessageProtocol(in, message), out);
    }

    /**
     *  Our goal was to work with any protocol.  In order to do that, we needed
     *  to allow them to call readMessageBegin() and get a TMessage in exactly
     *  the standard format, without the service name prepended to TMessage.name.
     */
    public static class StoredMessageProtocol extends TProtocolDecorator {
        TMessage messageBegin;
        public StoredMessageProtocol(TProtocol protocol, TMessage messageBegin) {
            super(protocol);
            this.messageBegin = messageBegin;
        }
        @Override
        public TMessage readMessageBegin() throws TException {
            return messageBegin;
        }
    }
}
