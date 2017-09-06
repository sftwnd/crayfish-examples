package derbytest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 *  Загрузчик базового приложения командной строки
 */

@Component
@DependsOn("DerbyJdbcTemplateTest")
@Profile(value = "crayfish-examples")
public class DerbyJdbcTemplateTestRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DerbyJdbcTemplateTestRunner.class);

    @Override
    public void run(String... args) throws Exception {
        logger.info("{}[derbytest.DerbyJdbcTemplateTestRunner] has been started.", this.getClass().getSimpleName());
    }

}
