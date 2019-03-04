package com.github.sftwnd.crayfish.examples.zookeeper;

import com.github.sftwnd.crayfish.common.concurrent.LockUtils;
import com.github.sftwnd.crayfish.common.exception.ExceptionUtils;
import com.github.sftwnd.crayfish.zookeeper.DefaultZookeeperConfig;
import com.github.sftwnd.crayfish.zookeeper.DefaultZookeeperService;
import com.github.sftwnd.crayfish.zookeeper.ZookeeperConfig;
import com.github.sftwnd.crayfish.zookeeper.ZookeeperService;
import com.github.sftwnd.crayfish.zookeeper.concurrent.ZookeeperLockService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Dotter {

    private static final Random   random = new Random(System.currentTimeMillis());
    private static final Instant  limit = Instant.now().plus(3, ChronoUnit.MINUTES);
    private static final String   lockName = "lock1";
    private static final String   zookeeperPathPrefix = String.format("/%s/locks/",Dotter.class.getCanonicalName().replace('.','/'));
    private static final String   address = "localhost:2181";

    public static void main(String[] args) {
        ZookeeperConfig zookeeperConfig = new DefaultZookeeperConfig(address);
        IntStream.rangeClosed(1,3).mapToObj(i -> new DotterSession(String.format("Session %d", i), zookeeperConfig)).forEach(ds -> ds.run());
    }

    static class DotterSession implements Runnable {

        private final String name;
        private final ZookeeperService zookeeperService;
        private final ZookeeperLockService lockService;

        public DotterSession(String name, ZookeeperConfig config) {
            this.name = name;
            this.zookeeperService = new DefaultZookeeperService(config);
            try {
                this.zookeeperService.getCuratorFramework().getZookeeperClient().blockUntilConnectedOrTimedOut();
            } catch (InterruptedException itex) {
                Thread.currentThread().interrupt();
                ExceptionUtils.uncheckExceptions(itex);
            }
            this.lockService = new ZookeeperLockService(zookeeperService, zookeeperPathPrefix);
        }

        @Override
        public void run() {
           IntStream.rangeClosed(1,3).mapToObj(i -> new DotterService(String.format("%s Service %d", name, i), this.lockService)).forEach(srvc -> srvc.run());
        }

    }

    static class DotterService implements Runnable {

        private final String name;
        private final Lock lock;

        DotterService(String name, ZookeeperLockService lockService) {
            this.name = name;
            this.lock = lockService.getNamedLock(lockName);
        }

        @Override
        public void run() {
            IntStream.rangeClosed(1,3).mapToObj(i -> new DotterThread(String.format("%s Thread %d", name, i), this.lock)).forEach(thrd -> new Thread(thrd).start());
        }

    }

    @Slf4j
    static class DotterThread implements Runnable {

        private final String name;
        private final Lock lock;
        private final Task task;

        DotterThread(String name, Lock lock) {
            this.name = name;
            this.lock = lock;
            this.task = new Task(name, lock);
        }


        @Override
        public void run() {
            logger.debug("{} >>> START", name);
            try {
                while (Instant.now().isBefore(limit)) {
                    try {
                        LockUtils.runWithLock(lock, task);
                    } catch (Throwable throwable) {
                        logger.warn("{} [ERR]: {}", name, throwable.getLocalizedMessage());
                    }
                    ExceptionUtils.wrapUncheckedExceptions(() -> Thread.sleep(250+random.nextInt(500)));
                }
            } finally {
                logger.debug("{} <<< FINISH");
            }
        }

    }

    @Slf4j
    static class Task implements Runnable {

        private final String name;
        private final Lock lock;
        private final List<Step> steps;

        Task(String name, Lock lock) {
            this.name = name;
            this.lock = lock;
            this.steps = IntStream.rangeClosed(1,6).mapToObj(i -> new Step(String.format("%s Step %d", name, i))).collect(Collectors.toList());
        }

        @Override
        public void run() {
            logger.debug("{} >>> START", name);
            try {
                steps.stream().forEach(
                        step -> LockUtils.runWithLock(lock, step)
                );
            } finally {
                System.out.println();
                logger.debug("{} <<< FINISH", name);
            }
        }
    }

    @AllArgsConstructor
    @Slf4j
    static class Step implements Runnable {

        private final String name;

        @Override
        public void run() {
            logger.debug("{} >>> START", name);
            System.out.print(String.format("%s: .", name));
            try {
                Instant completeAt = Instant.now().plusMillis(750+random.nextInt(1500));
                while(Instant.now().isBefore(completeAt)) {
                    System.out.print(".");
                    ExceptionUtils.wrapUncheckedExceptions(() -> Thread.sleep(100));
                }
            } finally {
                System.out.println();
                logger.debug("{} <<< FINISH", name);
            }
        }

    }

}
