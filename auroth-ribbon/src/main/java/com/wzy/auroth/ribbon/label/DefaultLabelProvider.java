package com.wzy.auroth.ribbon.label;

import com.wzy.auroth.ribbon.RibbonConstants;
import lombok.Setter;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;

public class DefaultLabelProvider implements LabelProvider {

    @Setter
    private Tracer tracer;

    /**
     * get label.
     *
     * @return label
     */
    @Override
    public String getLabel() {
        if (this.tracer != null) {
            Span currentSpan = tracer.getCurrentSpan();
            return currentSpan.getBaggageItem(RibbonConstants.SPAN_LABEL);
        }
        return null;
    }
}
