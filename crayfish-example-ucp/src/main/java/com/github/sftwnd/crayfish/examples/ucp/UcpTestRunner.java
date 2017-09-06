package com.github.sftwnd.crayfish.examples.ucp;

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
@Profile(value = "crayfish-example-ucp")
public class UcpTestRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(UcpTestRunner.class);

    @Autowired
    DataSource dataSource;

    private static final String sessionQuery = "select s.program, s.module, s.action, s.terminal\n" +
                                               "  from (select sys_context('userenv','sessionid') as sid from dual) d\n" +
                                               "  join v$session s\n" +
                                               "    on s.audsid = d.sid";

    @Override
    public void run(String... args) throws Exception {
        oracle.jdbc.OracleConnection c;
        logger.info("{} has been started.", this.getClass().getSimpleName());
        try ( Connection connection = dataSource.getConnection() ) {
            connection.setAutoCommit(true);
            try (PreparedStatement psttm = connection.prepareStatement(sessionQuery)) {
                try (ResultSet rset = psttm.executeQuery()) {
                    while (rset.next()) {
                        logger.info("ROW found {");
                        for (int i=1; i<=rset.getMetaData().getColumnCount(); i++) {
                            logger.info("\tv$session.{}: {}", rset.getMetaData().getColumnName(i).toLowerCase(), rset.getObject(i));
                        }
                        logger.info("}");

                    }
                }
            }
        }
        logger.info("{} has been completed.", this.getClass().getSimpleName());
    }

}
