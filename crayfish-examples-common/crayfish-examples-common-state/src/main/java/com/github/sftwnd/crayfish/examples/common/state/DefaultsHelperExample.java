package com.github.sftwnd.crayfish.examples.common.state;

import com.github.sftwnd.crayfish.common.state.DefaultsHelper;
import com.github.sftwnd.crayfish.common.state.DefaultsHolder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultsHelperExample {

    public static void main(String[] args) {
        DefaultsHolder<String> holder = new DefaultsHolder<>(() -> "Initial value");
        DefaultsHelper<String> helper = new DefaultsHelper<>(holder);
        logger.info("Call process()");
        process(holder);
        logger.info("Call helper.process()");
        helper.process("Process value", () -> process(holder));
    }

    private static void process(DefaultsHolder<?> holder) {
        logger.info("Holder value: {}", holder.getCurrentValue());
    }

}
