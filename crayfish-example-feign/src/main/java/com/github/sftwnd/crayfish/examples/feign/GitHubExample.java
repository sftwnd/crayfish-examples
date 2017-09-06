package com.github.sftwnd.crayfish.examples.feign;

import feign.Feign;
import feign.Param;
import feign.RequestLine;
import feign.Response;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import feign.gson.GsonDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GitHubExample {

    private static final Logger logger = LoggerFactory.getLogger(GitHubExample.class);

    @Bean
    GitHub getGitHub() {
        return GitHub.connect();
    }

    interface GitHub {

        class Repository {
            String name;
        }

        class Contributor {
            String login;
        }

        @RequestLine("GET /users/{username}/repos?sort=full_name")
        List<Repository> repos(@Param("username") String owner);

        @RequestLine("GET /repos/{owner}/{repo}/contributors")
        List<Contributor> contributors(@Param("owner") String owner, @Param("repo") String repo);

        /** Lists all contributors for all repos owned by a user. */
        default List<String> contributors(String owner) {
            return repos(owner).stream()
                    .flatMap(repo -> contributors(owner, repo.name).stream())
                    .map(c -> c.login)
                    .distinct()
                    .collect(Collectors.toList());
        }

        static GitHub connect() {
            Decoder decoder = new GsonDecoder();
            return Feign.builder()
                    .decoder(decoder)
                    .errorDecoder(new GitHubErrorDecoder(decoder))
                    .logger(new feign.Logger.ErrorLogger())
                    .logLevel(feign.Logger.Level.BASIC)
                    .target(GitHub.class, "https://api.github.com");
        }
    }


    static class GitHubClientError extends RuntimeException {
        private String message; // parsed from json

        @Override
        public String getMessage() {
            return message;
        }
    }

    public static void main(String... args) {
        process(GitHub.connect());
    }

    public static void process(GitHub gitHub) {
        logger.info("Let's fetch and print a list of the contributors to this org.");
        List<String> contributors = gitHub.contributors("netflix");
        for (String contributor : contributors) {
            logger.info(contributor);
        }

        logger.info("Now, let's cause an error.");
        try {
            gitHub.contributors("netflix", "some-unknown-project");
        } catch (GitHubClientError e) {
            logger.error(e.getLocalizedMessage());
        }
    }

    static class GitHubErrorDecoder implements ErrorDecoder {

        final Decoder decoder;
        final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

        GitHubErrorDecoder(Decoder decoder) {
            this.decoder = decoder;
        }

        @Override
        public Exception decode(String methodKey, Response response) {
            try {
                return (Exception) decoder.decode(response, GitHubClientError.class);
            } catch (IOException fallbackToDefault) {
                return defaultDecoder.decode(methodKey, response);
            }
        }
    }
}
