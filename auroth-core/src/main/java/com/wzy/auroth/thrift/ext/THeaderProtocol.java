package com.wzy.auroth.thrift.ext;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TField;
import org.apache.thrift.protocol.TMap;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TType;

import java.util.HashMap;
import java.util.Map;

public class THeaderProtocol {

    private TProtocol delegate;

    public THeaderProtocol(TProtocol protocol) {
        this.delegate = protocol;
    }

    public Map<String, String> readHeaders() throws TException {
        Map<String, String> headers = null;

        TField schemeField = this.delegate.readFieldBegin();

        if (schemeField.id == 0
                && schemeField.type == org.apache.thrift.protocol.TType.MAP) {
            TMap _map = this.delegate.readMapBegin();
            headers = new HashMap<>(2 * _map.size);
            for (int i = 0; i < _map.size; ++i) {
                String key = this.delegate.readString();
                String value = this.delegate.readString();
                headers.put(key, value);
            }
            this.delegate.readMapEnd();
        }
        this.delegate.readFieldEnd();
        return headers;
    }

    public void writeHeaders(Map<String, String> headers) throws TException{
        TField RTRACE_ATTACHMENT = new TField("rtraceAttachment", TType.MAP,
                (short) 0);
        this.delegate.writeFieldBegin(RTRACE_ATTACHMENT);
        {
            this.delegate.writeMapBegin(new TMap(TType.STRING, TType.STRING, headers
                    .size()));
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                this.delegate.writeString(entry.getKey());
                this.delegate.writeString(entry.getValue());
            }
            this.delegate.writeMapEnd();
        }
        this.delegate.writeFieldEnd();
    }
}
