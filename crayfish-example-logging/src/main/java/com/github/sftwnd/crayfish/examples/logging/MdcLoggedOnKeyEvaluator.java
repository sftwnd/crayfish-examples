package com.github.sftwnd.crayfish.examples.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.boolex.EventEvaluatorBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MdcLoggedOnKeyEvaluator extends EventEvaluatorBase<ILoggingEvent> {

    List<String> keys = new ArrayList<String>();

    public void addKey(String markerStr) {
        keys.add(markerStr);
    }

    /**
     * Return true if event passed as parameter contains one of the specified
     * user-markers.
     */
    public boolean evaluate(ILoggingEvent event) throws NullPointerException, EvaluationException {

        Map<String, String> mdc = event.getMDCPropertyMap();

        if (keys.isEmpty() && mdc.isEmpty()) {
            return true;
        } else {
            for(String key:keys) {
                if (mdc.containsKey(key)) {
                    return true;
                }
            }
        }
        return false;
    }

}
