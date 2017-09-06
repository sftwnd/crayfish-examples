package com.github.sftwnd.crayfish.examples;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;


/**
 *  Загрузчик базового приложения командной строки
 */

@Component
@ComponentScan(basePackages = "crayfish")
public class ExampleRunner implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {

    }

}
