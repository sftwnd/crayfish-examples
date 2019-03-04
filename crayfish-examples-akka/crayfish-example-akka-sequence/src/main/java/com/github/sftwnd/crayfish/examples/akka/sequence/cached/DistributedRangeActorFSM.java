package com.github.sftwnd.crayfish.examples.akka.sequence.cached;

import akka.actor.AbstractFSM;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.ddata.*;
import com.github.sftwnd.crayfish.examples.akka.sequence.cached.DistributedRangeActorFSM.Data;
import com.github.sftwnd.crayfish.examples.akka.sequence.cached.DistributedRangeActorFSM.State;
import com.github.sftwnd.crayfish.utils.ToStringer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.FiniteDuration;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component("distributedRangeActorFSM")
@Scope("prototype")
@Profile("crayfish-example-akka-sequence-cached")
public class DistributedRangeActorFSM extends AbstractFSM<State, Data> {

    private static final Logger logger = LoggerFactory.getLogger(DistributedRangeActorFSM.class);

    enum State {
        START, INIT, GET
    }

    static class Data {
        private BigInteger initValue = null;

        public Data(BigInteger initValue) {
            this.initValue = initValue;
        }

        public BigInteger getInitValue() {
            return initValue;
        }

        public void setInitValue(BigInteger initValue) {
            this.initValue = initValue;
        }

        @Override
        public String toString() {
            return new ToStringer(Data.class)
                      .append("initValue", this.initValue)
                      .toString();
        }
    }

    private static final Replicator.ReadConsistency       readAll = new Replicator.ReadAll(FiniteDuration.create(3, TimeUnit.SECONDS));
    private static final Replicator.ReadConsistency  readMajority = new Replicator.ReadMajority(FiniteDuration.create(3, TimeUnit.SECONDS));
    private static final Replicator.WriteConsistency    writeAll  = new Replicator.WriteAll(FiniteDuration.create(3, TimeUnit.SECONDS)); // Replicator.writeLocal();

    private final ActorSystem                              system = context().system();
    private final Cluster                                    node = Cluster.get(system);
    private final ActorRef                             replicator = DistributedData.get(system).replicator();

    private final Key<LWWRegister<BigInteger>>        sequenceKey;
    private final BigInteger                          incrementer;

    public DistributedRangeActorFSM(String sequenceName, final BigInteger initValue, int incrementBy) {
        this(sequenceName, () -> initValue == null ? BigInteger.valueOf(0L) : initValue, incrementBy);
    }

    @Override
    public void postStop() {
        super.postStop();
     // replicator.tell(new Replicator.Unsubscribe<>(sequenceKey, getSelf()), getSelf());
    }

    public DistributedRangeActorFSM(String sequenceName, Supplier<BigInteger> getInitValue, int incrementBy) {
        super();

        final FiniteDuration TWO_SECOND_DURATION   = FiniteDuration.create(2, TimeUnit.SECONDS);
        final FiniteDuration THREE_SECOND_DURATION = FiniteDuration.create(3, TimeUnit.SECONDS);
        final FiniteDuration FIVE_SECOND_DURATION  = FiniteDuration.create(5, TimeUnit.SECONDS);
        sequenceKey = LWWRegisterKey.create(sequenceName);
        incrementer = BigInteger.valueOf(incrementBy);

     // replicator.tell(new Replicator.Subscribe<>(sequenceKey, getSelf()), getSelf());

        startWith(State.START, null, FiniteDuration.Zero());

        when(
                 State.START
                ,matchEventEquals(
                        StateTimeout()
                        ,(event, data) -> {
                            logger.warn("[#01] State: {}, Event: {} for sequence: {} with data: {}", stateName(), event, sequenceName, String.valueOf(data));
                            if (data == null) {
                                replicator.tell(
                                        new Replicator.Get<>(
                                                sequenceKey
                                               ,readAll
                                        )
                                        ,getSelf()
                                );
                            }
                            return stay().forMax(TWO_SECOND_DURATION);
                        }
                )
        );

        when(
                State.START
               ,matchEvent(
                       Replicator.GetFailure.class
                      ,(event, data) -> {
                           logger.warn("[#02] State: {}, Event: {} for sequence: {} with data: {}", stateName(), event, sequenceName, String.valueOf(data));
                           return stay().forMax(TWO_SECOND_DURATION);
                       }
                )
        );

        when(
                 State.START
                ,matchEvent(
                        Replicator.NotFound.class
                        ,(event, data) -> {
                            logger.warn("[#03] State: {}, Event: {} for sequence: {} with data: {}", stateName(), event, sequenceName, String.valueOf(data));
                            CompletableFuture.supplyAsync(
                                    getInitValue
                                   ,getContext().dispatcher()
                            ).thenAccept(
                                    initValue -> {
                                        logger.trace("[#04] State: START, getInitValue::thenAccept(initValue={})", String.valueOf(initValue));
                                        replicator.tell(
                                                new Replicator.Update(
                                                        sequenceKey,
                                                        LWWRegister.create(node, initValue),
                                                        writeAll,
                                                        value -> LWWRegister.create(node, initValue)
                                                )
                                                ,getSelf()
                                        );
                                    }
                            ).exceptionally(
                                    throwable -> {
                                        logger.error("[#05] State: START, getInitValue::exceptionally(throwable={})", throwable.getLocalizedMessage(), throwable);
                                        getSelf().tell(StateTimeout(), getSelf());
                                        return null;
                                    }
                            );
                            return goTo(State.INIT).forMax(FIVE_SECOND_DURATION);
                        }
                )
        );

        when(
                State.START
                ,matchEvent(
                        Replicator.GetSuccess.class
                        ,(event, data) -> {
                            @SuppressWarnings("unchecked")
                            Replicator.GetSuccess<LWWRegister<BigInteger>> success = (Replicator.GetSuccess<LWWRegister<BigInteger>>)event;
                            logger.warn("[#06] State: {}, Event: {} for sequence: {} with data: {}. Value: ", stateName(), event, sequenceName, String.valueOf(data), success.dataValue().getValue());
                            return goTo(State.GET).forMax(TWO_SECOND_DURATION);
                        }
                )
        );

        onTransition(
                matchState(
                         State.START
                        ,State.INIT
                        ,(startState, endState) -> {
                             final BigInteger initValue = stateData().getInitValue();
                            logger.warn("[#07] onTransition({} -> {}) for sequence: {} with data: {}", startState, endState, sequenceName, String.valueOf(stateData()));
                            replicator.tell(
                                     new Replicator.Update(
                                             sequenceKey,
                                             LWWRegister.create(node, initValue),
                                             writeAll,
                                             value -> LWWRegister.create(node, initValue)
                                     )
                                     ,getSelf()
                             );
                        }
                )
        );

        when(
                State.START
               ,matchEvent(
                       Arrays.asList(Replicator.UpdateFailure.class, Replicator.UpdateTimeout.class, Replicator.UpdateSuccess.class, StateTimeout())
                      ,Data.class
                      ,(event, data) -> {
                            logger.warn("[#08] State: {}, Event: {} for sequence: {} with data: {}", stateName(), event, sequenceName, String.valueOf(data));
                            return goTo(State.START).forMax(FiniteDuration.Zero());
                       }
                )
        );
        /*
        when(
                State.INIT
                ,THREE_SECOND_DURATION
                ,matchEvent(
                        Arrays.asList(Replicator.UpdateFailure.class, Replicator.UpdateTimeout.class, StateTimeout())
                        ,Data.class
                        ,(event, data) -> {
                            logger.warn("[#09] State: {}, Event: {} for sequence: {} with data: {}", stateName(), event, sequenceName, String.valueOf(data));
                            return goTo(State.START).forMax(FiniteDuration.Zero());
                        }
                )
        );
        */
        whenUnhandled(
                matchAnyEvent(
                        (event, data) -> {
                            logger.info("[#10] Unhandled on State: {}, Event: {} with data: {}", event, stateName(), data);
                            return stay();
                        }
                )
        );

        initialize();
    }



}
