package com.github.sftwnd.crayfish.examples.akka.cluster;

import akka.actor.AbstractFSM;
import akka.actor.PoisonPill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

/**
 * Created by ashindarev on 06.03.17.
 */
@Component("crayfish-example-singletonActor")
@Scope("prototype")
// @Profile("crayfish-example-akka-cluster")
// @DependsOn("crayfish-actorSystem")
public class SingletonActor extends AbstractFSM<SingletonActor.State, Long> {

    private static final Logger logger = LoggerFactory.getLogger(SingletonActor.class);

    protected enum State {Init, Active, Exit};
    protected enum Event {ToDo};

    @Autowired
    ApplicationContext applicationContext;

    private static final long LIMIT = 12;

    public SingletonActor() {

        startWith(State.Init, null, FiniteDuration.fromNanos(2000L));

        when( State.Init
              ,matchEventEquals(StateTimeout(), (event, data) -> {
                    logger.info("[[[[[[[[[[[ STARTED ]]]]]]]]]]}", data);
                    return goTo(State.Active).using(new Long(0L)).forMax(FiniteDuration.fromNanos(2000L));
                 }
              )
            );

        when( State.Active
             ,matchEventEquals(StateTimeout(), (event, data) -> {
                            logger.info("[[[[[[[[[[[ PROCESSED:{} ]]]]]]]]]]}", data);
                            if (data.longValue() >= LIMIT) {
                                //getSelf().tell(ClusterRunner.TestSingletonMessages.end(), getSelf());
                                throw new Exception("Кирдык-пердык исключение...");
                            }
                            logger.info("Decreases: {}", data.longValue());
                            return stay().using(data.longValue() + 1).forMax(new FiniteDuration(1, TimeUnit.SECONDS));
                        }
                )
        );

        when( State.Active
             ,matchEvent(
                     ClusterRunner.TestSingletonMessages.End.class
                    ,(event, data) -> {
                            logger.info("[[[[[[[[[[[ STOPPED ]]]]]]]]]]}");
                            return goTo(State.Exit).using(null);
                     }
                )
        );
        /*

        when( State.Init
                ,matchEvent(
                        ClusterRunner.TestSingletonMessages.End.class
                        ,(event, data) -> {
                            logger.info("Сигнал по поводу остановки...");
                            return stay().forMax(FiniteDuration.Zero());
                        }
                )
        );
        */
        onTransition(
                matchState(State.Active, State.Exit, () -> {
                    logger.info("[[[ OOPS ]]]");
                    self().tell(PoisonPill.getInstance(), self());
                })
        );

        when( State.Exit
             ,matchAnyEvent(
                        (event, data) -> { logger.info("[[[ EXIT ANY EVENT ]]]"); self().tell(PoisonPill.getInstance(), self());return stay(); }
              )
        );


        initialize();

    }

    @Override
    public void preStart() {
        logger.info("[[[[[[[[[[[ PRE START ]]]]]]]]]]}");
    }


    @Override
    public void postStop() {
        logger.info("[[[[[[[[[[[ POST STOP ]]]]]]]]]]}");
    }

}
