package com.github.sftwnd.crayfish.examples.derbyserverucp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *  Загрузчик базового приложения командной строки
 */

@Component
@Profile(value = "crayfish-example-derbyserver-ucp")
public class DerbyServerUcpRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DerbyServerUcpRunner.class);

    @Autowired
    //@Qualifier(value = "dataSource")
    DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        logger.info("{}[derbytest.com.github.sftwnd.crayfish.examples.derbyserver.DerbyJdbcTemplateTestRunner] has been started.", this.getClass().getSimpleName());
        Thread.sleep(2000L);
        try ( Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            try (Statement sttm = connection.createStatement()) { sttm.execute("DROP TABLE T1"); } catch (SQLException sex) {}
            try (Statement sttm = connection.createStatement()) {
                sttm.execute("CREATE TABLE T1(N INTEGER, V VARCHAR(100))");
                sttm.execute("ALTER TABLE T1 ADD CONSTRAINT T_PK PRIMARY_KEY(N)");
            } catch (SQLException sex) {

            }
        }
        try ( Connection connection1 = dataSource.getConnection();
              Connection connection2 = dataSource.getConnection();
              PreparedStatement psttm1 = connection1.prepareStatement("select * from T1");
              PreparedStatement psttm2 = connection2.prepareStatement("select * from T1")
            )
        {
            connection1.setAutoCommit(false);
            connection2.setAutoCommit(false);
            DatabaseMetaData metaData = connection1.getMetaData();
            logger.info("Connected by driver: {} {} to the database: {} {}", metaData.getDriverName(), metaData.getDriverVersion(), metaData.getDatabaseProductName(), metaData.getDatabaseProductVersion());
            try( PreparedStatement psttm = connection1.prepareStatement("insert into T1 values(?,?)")) {
                psttm.setInt(1, 123);
                psttm.setString(2, "V=123");
                psttm.execute();
            }
            connection1.commit();
            try ( ResultSet rset1 = psttm1.executeQuery();
                  ResultSet rset2 = psttm2.executeQuery() ) {
                while (rset1.next()) {
                    logger.info("[A1] CONNECTION1, ROW: {}, {}", rset1.getInt(1), rset1.getString(2));
                }
                while (rset2.next()) {
                    logger.info("[A2] CONNECTION2, ROW: {}, {}", rset2.getInt(1), rset2.getString(2));
                }
            }
        }
    }

}
