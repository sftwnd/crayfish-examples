package com.github.sftwnd.crayfish.examples.common.resource;

import com.github.sftwnd.crayfish.common.exception.Processor;
import com.github.sftwnd.crayfish.common.resource.IResourceProvider;
import com.github.sftwnd.crayfish.common.resource.LazyResourceProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.net.ConnectException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.sftwnd.crayfish.common.exception.ExceptionUtils.wrapUncheckedExceptions;

@Slf4j
public class LazyResourceProviderExampleMain {

    private static final Random random = new Random();
    private static final Set<Server> servers = Stream.of("A", "B", "C", "D").map(name -> new Server(name, false)).collect(Collectors.toSet());

    public static void main(String[] args) throws InterruptedException {
        startServers();
        Instant finish = Instant.now().plus(3, ChronoUnit.MINUTES);
        IResourceProvider<Connection> provider = getResourceProvider();
        while(Instant.now().isBefore(finish)) {
            wrapUncheckedExceptions(
                    () -> Optional.ofNullable(provider.provide())
                            .ifPresent(conn -> wrapUncheckedExceptions( () -> conn.process("processed..."))),
                    () -> {}
            );
            Thread.sleep(1500);
        }

    }

    private static IResourceProvider<Connection> getResourceProvider() {
        final LazyResourceProvider<Connector, Connection> result = new LazyResourceProvider<>(
                () -> new Connector(servers),
                Connector::connect,
                con -> Optional.ofNullable(con).map(Connection::isAlive).orElse(false)
        );
        AtomicInteger cnt = new AtomicInteger();
        startDaemonCycle(() -> {
            Thread.sleep(1000 + random.nextInt(1000));
            int value = cnt.incrementAndGet();
            if(value % 7 == 3) {
                logger.info("ResourceProvider errorStack: enabled");
                result.setErrorStack(true);
            } else if (result.isErrorStack()) {
                logger.info("ResourceProvider errorStack: disable");
                result.setErrorStack(false);
            }
        });
        return result;
    }

    private static void startServers() {
        AtomicInteger cnt = new AtomicInteger();
        servers.stream()
                .map(srv -> (Processor<Exception>) () -> {
                                Thread.sleep(5000 + random.nextInt(5000));
                                srv.setAlive(!srv.isAlive());
                            }
                ).forEach(LazyResourceProviderExampleMain::startDaemonCycle);
    }

    @AllArgsConstructor
    static class Connector {
        private Set<Server> servers;
        Connection connect() throws ConnectException {
            List<Server> servers = Optional.ofNullable(this.servers)
                    .map(Set::stream).orElse(Stream.empty())
                    .filter(Objects::nonNull).filter(s -> s.isAlive())
                    .collect(Collectors.toList());
            if (servers.size() > 0) {
                return servers.get(random.nextInt(1374981) % servers.size()).getConnection();
            }
            throw new ConnectException("There are no available servers");
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    static class Server {
        private String name;
        private boolean alive;
        public Connection getConnection() throws ConnectException {
            if (isAlive()) {
                if (random.nextInt(47*1371) % 47 == 0) {
                    throw new ConnectException("Connection process to server "+getName()+" has been terminated.");
                }
                return new Connection(this, "@"+ UUID.randomUUID().toString());
            }
            throw new ConnectException("Unable to connect on server "+getName());
        }
        public synchronized void setAlive(boolean alive) {
            this.alive = alive;
            if (isAlive()) {
                logger.info("SRV {} alive", getName());
            } else {
                logger.warn("SRV {} unavailable", getName());
            };
        }
        public synchronized void process(Connection conn, String text) throws ConnectException {
            if (isAlive()) {
                logger.info("SRV {}, CONN[{}]: {}", getName(), conn.getName(), text);
            } else {
                throw new ConnectException("Connection on server "+getName()+" is broken.");
            }
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    static class Connection {
        private @Nonnull
        Server server;
        private @Nonnull String name;
        public void process(String text) throws ConnectException {
            server.process(this, text);
        }
        public boolean isAlive() {
            return server.isAlive();
        }
    }

    private static void startDaemon(Processor<?> processor) {
        Thread thread = new Thread(() -> {
            logger.debug("Daemon: {} has been started", Thread.currentThread().getName());
            wrapUncheckedExceptions(processor::process, () -> {});
            logger.debug("Daemon: {} has been finished", Thread.currentThread().getName());
        });
        thread.setDaemon(true);
        thread.start();
    }

    private static void startDaemonCycle(Processor<Exception> processor) {
        startDaemon(interruptableCycle(processor));
    }

    private static Processor<Exception> interruptableCycle(Processor<Exception> processor) {
        return () -> {
            while(!Thread.currentThread().isInterrupted()) {
                wrapUncheckedExceptions(processor::process);
            }
        };
    }

}
