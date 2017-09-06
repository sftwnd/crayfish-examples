package com.github.sftwnd.crayfish.examples.derbyserverucp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 *  Загрузчик базового приложения командной строки
 */

@Component
@Profile(value = "crayfish-example-oracle-ucp")
public class OracleUcpRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(OracleUcpRunner.class);

    @Autowired
    //@Qualifier(value = "dataSource")
    DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        logger.info("{}[derbytest.com.github.sftwnd.crayfish.examples.derbyserver.DerbyJdbcTemplateTestRunner] has been started.", this.getClass().getSimpleName());
        Thread.sleep(2000L);
        try ( Connection connection = dataSource.getConnection();
              PreparedStatement psttm = connection.prepareStatement("select to_char(100/33) from dual");
            )
        {
            try ( ResultSet rset = psttm.executeQuery() ) {
                while (rset.next()) {
                    logger.info("Query result: {}", rset.getString(1));
                }
            }        }
    }

}
