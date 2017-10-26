package com.wzy.auroth.sleuth;

import lombok.Setter;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanInjector;
import org.springframework.cloud.sleuth.SpanTextMap;
import org.springframework.cloud.sleuth.instrument.web.HttpSpanInjector;

public class ThriftSpanInjector implements SpanInjector<SpanTextMap> {

    @Setter
    private HttpSpanInjector httpSpanInjector;


    /**
     * Takes two arguments:
     * <ul>
     * <li>a Span instance, and</li>
     * <li>a “carrier” object in which to inject that Span for cross-process propagation.
     * </li>
     * </ul>
     * <p>
     * A “carrier” object is some sort of http or rpc envelope, for example HeaderGroup
     * (from Apache HttpComponents).
     * <p>
     * Attempting to inject to a carrier that has been registered/configured to this
     * Tracer will result in a IllegalStateException.
     *
     * @param span
     * @param carrier
     */
    @Override
    public void inject(Span span, SpanTextMap carrier) {
        this.httpSpanInjector.inject(span, carrier);
    }
}
