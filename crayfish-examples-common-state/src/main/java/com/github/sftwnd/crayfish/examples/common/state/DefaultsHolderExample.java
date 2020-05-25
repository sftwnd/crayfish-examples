package com.github.sftwnd.crayfish.examples.common.state;

import com.github.sftwnd.crayfish.common.exception.Processor;
import com.github.sftwnd.crayfish.common.state.DefaultsHolder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

import static com.github.sftwnd.crayfish.common.exception.ExceptionUtils.wrapUncheckedExceptions;

@Slf4j
public class DefaultsHolderExample {

    public static void main(String[] args) {
        DefaultsHolder<String> holder = new DefaultsHolder<>(() -> "System");
        logger.info("Holder(\"System\") default value: {}", holder.getDefaultValue());
        logger.info("Holder(\"System\") current value: {}", holder.getCurrentValue());
        holder.setDefaultValue("Default");
        logger.info("holder.setDefaultValue(\"Default\")");
        logger.info("Holder(\"System\") default value: {}", holder.getDefaultValue());
        logger.info("Holder(\"System\") current value: {}", holder.getCurrentValue());
        holder.setCurrentValue("Current");
        logger.info("holder.setCurrentValue(\"Current\")");
        logger.info("Holder(\"System\") default value: {}", holder.getDefaultValue());
        logger.info("Holder(\"System\") current value: {}", holder.getCurrentValue());

        process(() -> {
                    logger.info("[Other thread] Holder(\"System\") default value: {}", holder.getDefaultValue());
                    logger.info("[Other thread] Holder(\"System\") current value: {}", holder.getCurrentValue());
                });

        holder.clearCurrentValue();
        holder.clearDefaultValue();
        logger.info("holder.clearCurrentValue()");
        logger.info("holder.clearCurrentValue()");
        logger.info("Holder(\"System\") default value: {}", holder.getDefaultValue());
        logger.info("Holder(\"System\") current value: {}", holder.getCurrentValue());

        logger.info("You are able to register/unregister holder on object/class by DefaultsHolder.[un]register(..)");
    }

    @SneakyThrows
    private static <E extends Exception> void process(Processor<E> processor) {
        CountDownLatch cdl = new CountDownLatch(1);
        new Thread(() -> wrapUncheckedExceptions(() -> { processor.process(); cdl.countDown(); })).start();
        cdl.await();
    }
}
