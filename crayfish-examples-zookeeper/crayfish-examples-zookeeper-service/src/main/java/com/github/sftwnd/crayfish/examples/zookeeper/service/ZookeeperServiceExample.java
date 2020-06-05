package com.github.sftwnd.crayfish.examples.zookeeper.service;

import com.github.sftwnd.crayfish.zookeeper.service.ZookeeperHelper;
import com.github.sftwnd.crayfish.zookeeper.service.ZookeeperService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.curator.framework.imps.CuratorFrameworkState.STARTED;

@Slf4j
public class ZookeeperServiceExample {

    @SneakyThrows
    public static void main(String[] args) {
        try ( Server server = Server.start();
              ZookeeperService service = new ZookeeperHelper(ZookeeperService.builder(server.connectString()), Duration.ofSeconds(1))
        ) {
            CuratorFramework framework = service.provide();
            logger.info("--- ============================================ ---");
            logger.info(">>>");
            logger.info("CuratorFramework provided: {}", framework);
            logger.info(">>>");
            logger.info(">>> listPath(/zookeeper)");
            listPath(service, "/zookeeper");
            logger.info(">>> listPath(/zookeeper/config)");
            listPath(service, "/zookeeper/config");
            logger.info("--- ============================================ ---");
            logger.info(">>>");
            logger.info(">>> Zookeeper DOWN");
            server.stopServer(2000);
            logger.info(">>> listPath(/zookeeper) [Throws|NoStack]");
            listPath(service, "/zookeeper");
            listPath(service, "/zookeeper");
            Thread.sleep(750);
            logger.info("--- ============================================ ---");
            logger.info(">>>");
            logger.info(">>> Set error stack on exception");
            Optional.of(service).map(ZookeeperHelper.class::cast).ifPresent(helper -> helper.setErrorStack(true));
            logger.info(">>> listPath(/zookeeper) [Throws|ErrorStack]");
            listPath(service, "/zookeeper");
            Thread.sleep(750);
            logger.info("--- ============================================ ---");
            logger.info(">>>");
            logger.info(">>> Absorb Throws on operation");
            Optional.of(service).map(ZookeeperHelper.class::cast).ifPresent(ZookeeperHelper::absorbResourceProvideException);
            logger.info(">>> listPath(/zookeeper) [Absorb|ErrorStack]");
            listPath(service, "/zookeeper");
            Thread.sleep(750);
            logger.info("--- ============================================ ---");
            logger.info(">>>");
            logger.info(">>> Set error stack off exception");
            Optional.of(service).map(ZookeeperHelper.class::cast).ifPresent(helper -> helper.setErrorStack(false));
            logger.info(">>> listPath(/zookeeper) [Absorb|NoStack]");
            listPath(service, "/zookeeper");
            Thread.sleep(750);
            logger.info("--- ============================================ ---");
            logger.info(">>>");
            logger.info(">>> Zookeeper UP");
            server.startServer();
            Thread.sleep(750);
            logger.info(">>> listPath(/zookeeper)");
            listPath(service, "/zookeeper");
            logger.info("--- ============================================ ---");
        }
    }

    static void listPath(ZookeeperService service, String path) {
        try {
            CuratorFramework curatorFramework = service.provide();
            if (curatorFramework != null) {
                if (curatorFramework.getState() == STARTED) {
                    byte[] data = curatorFramework.getChildren().forPath(path).stream().collect(Collectors.joining(", ")).getBytes();
                    logger.info("Data for path {}: {}", path, new String(data));
                } else {
                    logger.error("CURATOR NOT STARTED...");
                }
            } else {
                logger.error("CURATOR NOT PROVIDED...");
            }
        } catch (Throwable throwable) {
            logger.error("listPath operation broken: {}", throwable.getLocalizedMessage());
        }
    }

    static class Server implements AutoCloseable {

        private TestingServer server ;
        private int port;

        @SneakyThrows
        public Server() {
            this.port = findServerPort(10);
            startServer();
        }

        static Server start() {
            return new Server();
        }

        @SneakyThrows
        private synchronized void startServer() {
            this.server = new TestingServer(port, true);
            logger.info("Zookeeper Server is up");
        }

        public String connectString() {
            return Optional.ofNullable(server).map(TestingServer::getConnectString).orElse(null);
        }

        @SneakyThrows
        public synchronized void stopServer(long sleep) {
            try {
                this.server.stop();
            } catch (Exception ex) {
                logger.warn("Unable to stop server correctly: {}", ex.getLocalizedMessage());
            } finally {
                Thread.sleep(sleep);
            }
            logger.info("Zookeeper Server is down");
        }

        @SneakyThrows
        public synchronized void restartServer(long sleep) {
            try {
                stopServer(sleep);
            } finally {
                this.server.start();
            }
        }

        @SneakyThrows
        @Override
        public void close() {
            if (server != null) {
                try {
                    server.stop();
                    server.close();
                } finally {
                    server = null;
                }
            }
        }

        private static int findServerPort(int tries) {
            ServerSocket server = null;
            try {
                server = new ServerSocket(0);
                server.setReuseAddress(true);
                return server.getLocalPort();
            } catch ( IOException e ) {
                if (tries > 0) {
                    return findServerPort(tries - 1);
                }
                throw new Error(e);
            } finally {
                if ( server != null ) {
                    try {
                        server.close();
                    }  catch ( IOException ignore ) {
                        // ignore
                    }
                }
            }
        }
    }

}
