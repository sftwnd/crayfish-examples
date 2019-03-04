package com.github.sftwnd.crayfish.examples.java.threadpool;

import java.util.concurrent.*;

public class ThreadExecutorExceptionError {

    /**/
    public static void main(String[] args) throws InterruptedException {

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        System.out.println("Будьте внимательны - исключение от Runnable будет проглочено!!!!");

        executorService.submit(() -> {
            System.out.println("Submit I...");
            System.out.println(1 / 0);
        });
        Thread.sleep(1000L);

        Callable<Double> callable = new Callable<Double>() {
            @Override
            public Double call() throws Exception {
                System.out.println("Submit II...");
                double d = 1 / 0;
                return d;
            }
        };
        Future<Double> feature = executorService.submit(callable);

        try {
            Double d = feature.get();
            System.out.println(d.isInfinite());
        } catch (Exception ex) {
            System.out.println("Finished with feature II ..."+ex.getMessage());
        }

        Thread.sleep(1000L);
        executorService.shutdown();

    }
   /** /
    public static void main(String[] args) {
        try {
            System.out.println(
                    Executors.newSingleThreadExecutor()
                             .submit(() -> 1.0d / 0).get()
            );
        } catch (Throwable tr) {
            System.out.println(tr.getLocalizedMessage());
        }
    }
    /** /
    public static void main(String[] args) {
        try {
            System.out.println(
                    Executors.newSingleThreadExecutor()
                            .submit(
                                    () -> {
                                            return 1/0;
                                    }
                            ).get()
            );
        } catch (Throwable tr) {
            System.out.println(tr.getLocalizedMessage());
        }
    }
    /**/

}
