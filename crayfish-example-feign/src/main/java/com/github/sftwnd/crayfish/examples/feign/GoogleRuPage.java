package com.github.sftwnd.crayfish.examples.feign;

import feign.Feign;
import feign.RequestLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class GoogleRuPage {

    private static final Logger logger = LoggerFactory.getLogger(GitHubExample.class);

    @Bean
    GoogleRu getGitHub() {
        return GoogleRu.connect();
    }

    interface GoogleRu {

        @RequestLine("GET /")
        String page();

        static GoogleRu connect() {
            return Feign.builder()
                    .logger(new feign.Logger.ErrorLogger())
                    .logLevel(feign.Logger.Level.BASIC)
                    .target(GoogleRu.class, "http://www.billing.ru");
        }

    }



    public static void main(String... args) {
        process(GoogleRu.connect());
    }

    public static void process(GoogleRu googleRu) {
        logger.info(googleRu.page());
    }

}
