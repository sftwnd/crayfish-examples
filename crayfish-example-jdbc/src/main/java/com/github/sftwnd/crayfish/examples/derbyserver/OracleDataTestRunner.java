package com.github.sftwnd.crayfish.examples.derbyserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.Calendar;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *  Загрузчик базового приложения командной строки
 */

@Component
@Profile(value = "crayfish-example-jdbc-data")
public class OracleDataTestRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(OracleDataTestRunner.class);

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    @Qualifier(value = "dataSource")
    DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        logger.info("{} has been started.", this.getClass().getSimpleName());
        final CountDownLatch latch = new CountDownLatch(1);

        Calendar now = Calendar.getInstance();
        logger.info("TimeZone: {}", now.getTimeZone());
        logger.info("Time: {}", now.getTime());

        new JdbcTemplate(dataSource).execute(new ConnectionCallback<Object>() {
            @Override
            public Object doInConnection(Connection connection) throws SQLException, DataAccessException {

                try (ResultSet rset = connection.createStatement().executeQuery("select rownum as r, q.last_ddl_time as d, to_timestamp(q.timestamp, 'YYYY-MM-DD:HH24:MI:SS') as t from all_objects q\n")) {
                    while(rset.next()) {
                        Date date = rset.getDate("D");
                        Timestamp timestamp = rset.getTimestamp("T");
                        logger.info("D: {} [{}], T:{} [{}]", new Timestamp(date.getTime()), date.getTime(), timestamp, timestamp.getTime());
                        if (rset.getInt("R") >= 100) {
                            logger.info("...");
                            break;
                        }
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
