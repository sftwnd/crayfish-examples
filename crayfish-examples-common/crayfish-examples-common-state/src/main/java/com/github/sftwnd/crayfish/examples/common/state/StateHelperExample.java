package com.github.sftwnd.crayfish.examples.common.state;

import com.github.sftwnd.crayfish.common.state.StateHelper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StateHelperExample {

    @SuppressWarnings("try")
    public static void main(String[] args) throws Exception {
        POJO pojo = new POJO("Initial state");
        logger.info("Initial pojo value:");
        process(pojo);

        StateHelper<String> helper = new StateHelper<>(
                "Helper state", pojo::getState, pojo::setState
        );
        logger.info("After helper create pojo value:");
        process(pojo);
        helper.close();
        logger.info("After helper close pojo value:");
        process(pojo);

        logger.info("Use Autoclose:");
        try (AutoCloseable x = new StateHelper<>("Autoclosable state", pojo::getState, pojo::setState )) {
            process(pojo);
        }
        logger.info("After Autoclose pojo value:");
        process(pojo);

        StateHelper.process("Process with new state value", pojo::getState, pojo::setState, () -> process(pojo));
        logger.info("After Process pojo value:");
        process(pojo);

    }

    @Data
    @AllArgsConstructor
    static class POJO {
        String state;
    }

    private static void process(POJO pojo) {
        logger.info("POJO state: {}", pojo.getState());
    }

}
