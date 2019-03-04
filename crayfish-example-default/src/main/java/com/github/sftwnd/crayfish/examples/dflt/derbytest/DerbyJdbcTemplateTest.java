package com.github.sftwnd.crayfish.examples.dflt.derbytest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Created by ashindarev on 12.02.17.
 */
@Configuration(value = "DerbyJdbcTemplateTest")
@Profile(value = "crayfish-examples")
public class DerbyJdbcTemplateTest {

    @Autowired
    DataSource dataSource;

    @Bean(name = "jdbcTemplate")
    JdbcTemplate getJdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }

}
