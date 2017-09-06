package com.github.sftwnd.crayfish.examples.derbyserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *  Загрузчик базового приложения командной строки
 */

@Component
@Profile(value = "crayfish-example-jdbc")
public class DerbyJdbcTemplateTestRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DerbyJdbcTemplateTestRunner.class);

    @Autowired
    @Qualifier(value = "dataSource")
    DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        logger.info("{}[derbytest.com.github.sftwnd.crayfish.examples.derbyserver.DerbyJdbcTemplateTestRunner] has been started.", this.getClass().getSimpleName());
        final CountDownLatch latch = new CountDownLatch(1);
        new JdbcTemplate(dataSource).execute(new ConnectionCallback<Object>() {
            @Override
            public Object doInConnection(Connection connection) throws SQLException, DataAccessException {
                try (ResultSet rset = connection.getMetaData().getTables(null, null, "%", null)) {
                    while(rset.next()) {
                        logger.info("TABLE: {}.{}", rset.getString("TABLE_SCHEM"), rset.getString("TABLE_NAME"));
                    }
                } finally {
                    latch.countDown();
                }
                return null;
            }
        });
        latch.await(10, TimeUnit.SECONDS);
    }

}
