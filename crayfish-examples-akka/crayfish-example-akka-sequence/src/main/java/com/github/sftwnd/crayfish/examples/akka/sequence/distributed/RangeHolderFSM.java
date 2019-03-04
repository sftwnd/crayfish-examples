package com.github.sftwnd.crayfish.examples.akka.sequence.distributed;

import akka.actor.AbstractFSM;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.FSM;
import akka.actor.PoisonPill;
import akka.cluster.Cluster;
import akka.cluster.ddata.DistributedData;
import akka.cluster.ddata.Key;
import akka.cluster.ddata.LWWRegister;
import akka.cluster.ddata.LWWRegisterKey;
import akka.cluster.ddata.Replicator;
import akka.cluster.ddata.Replicator.Get;
import akka.cluster.ddata.Replicator.GetFailure;
import akka.cluster.ddata.Replicator.GetSuccess;
import akka.cluster.ddata.Replicator.NotFound;
import akka.cluster.ddata.Replicator.ReadConsistency;
import akka.cluster.ddata.Replicator.ReadMajority;
import akka.cluster.ddata.Replicator.Update;
import akka.cluster.ddata.Replicator.UpdateFailure;
import akka.cluster.ddata.Replicator.UpdateSuccess;
import akka.cluster.ddata.Replicator.UpdateTimeout;
import akka.cluster.ddata.Replicator.WriteConsistency;
import akka.cluster.ddata.Replicator.WriteMajority;
import akka.dispatch.PriorityGenerator;
import akka.dispatch.UnboundedStablePriorityMailbox;
import com.github.sftwnd.crayfish.examples.akka.sequence.distributed.RangeHolderFSM.Data;
import com.github.sftwnd.crayfish.examples.akka.sequence.distributed.RangeHolderFSM.State;
import com.typesafe.config.Config;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.github.sftwnd.crayfish.examples.akka.sequence.distributed.RangeHolderFSM.State.*;

@Component("rangeHolderFSM")
@Scope("prototype")
public class RangeHolderFSM extends AbstractFSM<State, Data> //implements
        //RequiresMessageQueue<RangeHolderFSM.AdminPriorityMailbox> {
        //RequiresMessageQueue<SingleConsumerOnlyUnboundedMailbox>
{


    private static final Logger logger = LoggerFactory.getLogger(RangeHolderFSM.class);

    private static final FiniteDuration ONE_SECOND_DURATION   = FiniteDuration.create(1, TimeUnit.SECONDS);
    private static final FiniteDuration TWO_SECOND_DURATION   = FiniteDuration.create(2, TimeUnit.SECONDS);
    private static final FiniteDuration THREE_SECOND_DURATION = FiniteDuration.create(3, TimeUnit.SECONDS);
    private static final FiniteDuration FIVE_SECOND_DURATION  = FiniteDuration.create(5, TimeUnit.SECONDS);

    private static final WriteConsistency writeMajority = new WriteMajority(FIVE_SECOND_DURATION);
    private static final ReadConsistency  readMajority  = new ReadMajority(TWO_SECOND_DURATION);

    public enum State {
        START, ACTIVE, UPDATE
    }

    public static class Empty {

        @Override
        public String toString() {
            return Empty.class.getSimpleName();
        }

    }

    public static final Empty EMPTY = new Empty();

    public static class Request {

        private final int size;

        public Request(int size) {
            this.size = Math.max(1, size);
        }

        public Request() {
            this(1);
        }

        public final int getSize() {
            return this.size;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                    .append("size", getSize())
                    .toString();
        }

    }

    public static class Data extends Range {

        private Data(Data data) {
            this(data.getStartValue(), data.getEndValue());
        }

        public Data(BigInteger startValue, BigInteger endValue) {
            super(startValue, endValue);
            assert startValue != null && endValue != null;
        }

        public Data(BigInteger endValue) {
            this(endValue.add(BigInteger.ONE), endValue);
        }

        // Первый элемент > последнего
        public boolean isEmpty() {
            return this.getSize() == 0;
        }

        public Range takeRange(int takeSize) {
            int size = Math.min(getSize(), takeSize);
            Range range = null;
            if (size > 0) {
                range = new Range(getStartValue(), getStartValue().add(BigInteger.valueOf(size-1)));
                setStartValue(getStartValue().add(BigInteger.valueOf(size)));
            }
            return range;
        }

        public Data copy() {
            return new Data(this);
        }

        public Data extend(BigInteger beforeValue) {
            return beforeValue == null ||
                   getEndValue().add(BigInteger.ONE).compareTo(beforeValue) >= 0
                 ? null
                 : new Data(getStartValue(), beforeValue.subtract(BigInteger.ONE));
        }

    }

    public static class Range {

        private BigInteger startValue;
        private BigInteger endValue;

        public Range(BigInteger startValue, BigInteger endValue) {
            this.startValue = startValue;
            this.endValue = endValue;
        }

        public BigInteger getStartValue() {
            return this.startValue;
        }

        public BigInteger getEndValue() {
            return this.endValue;
        }

        protected void setStartValue(BigInteger startValue) {
            this.startValue = startValue;
        }

        // Последний элемент минус Первый элемент + 1, но не меньше 0
        public int getSize() {
            return Math.max(getEndValue().subtract(getStartValue()).intValue()+1,0);
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                    .append("startValue", getStartValue())
                    .append("endValue", getEndValue())
                    .toString();
        }

    }

    private final ActorRef replicator = DistributedData.get(context().system()).replicator();
    private final Cluster node = Cluster.get(context().system());
    private final Key<LWWRegister<BigInteger>> sequenceKey;
    private final int cacheSize;

    public RangeHolderFSM(String sequenceName, int cacheSize, Supplier<BigInteger> initValue) {
        super();
        sequenceKey = LWWRegisterKey.create(sequenceName);
        this.cacheSize = cacheSize;
        configure(initValue);
    }

    public RangeHolderFSM(String name, int cacheSize, final BigInteger initValue) {
        this(name, cacheSize, () -> initValue);
    }

    public RangeHolderFSM(String name, Supplier<BigInteger> initValue) {
        this(name, 10000, initValue);
    }

    public RangeHolderFSM(String name, BigInteger initValue) {
        this(name, 10000, initValue);
    }

    public RangeHolderFSM(String name) {
        this(name, 10000, BigInteger.ZERO);
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        //replicator.tell(new Replicator.Subscribe<>(sequenceKey, self()), ActorRef.noSender());
    }

    @Override
    public void postStop() {
        //replicator.tell(new Replicator.Unsubscribe<>(sequenceKey, self()), ActorRef.noSender());
        super.postStop();
    }

    private void configure(Supplier<BigInteger> initValue) {

        startWith(START, null, FiniteDuration.Zero());

        when(
                START
               ,matchEventEquals(
                        StateTimeout()
                        ,(event, data) -> {
                            logger.trace("[#01] State: {}, Event: {} for sequence: {} with data: {}", stateName(), event, sequenceKey, String.valueOf(data));
                            if (data == null) {
                                replicator.tell(
                                        new Get<>(
                                                sequenceKey
                                               ,readMajority
                                        )
                                        ,getSelf()
                                );
                            }
                            return stay().forMax(THREE_SECOND_DURATION);
                        }
                )
        );

        when(
                START
               ,matchEvent(
                        Arrays.asList(GetFailure.class, UpdateFailure.class)
                       ,(event, data) -> {
                            logger.trace("[#02] State: {}, Event: {} for sequence: {} with data: {}", stateName(), event, sequenceKey, String.valueOf(data));
                            return stay().forMax(ONE_SECOND_DURATION);
                        }
                )
        );

        when(
                 START
                ,matchEvent(
                         GetSuccess.class
                        ,(event, data) -> {
                            @SuppressWarnings("unchecked")
                            GetSuccess<LWWRegister<BigInteger>> success = (GetSuccess<LWWRegister<BigInteger>>)event;
                            Data newData = new Data(success.dataValue().getValue());
                            logger.trace("[#03] State: {}, Event: {} for sequence: {} with data: {} -> {}. Value: {}", stateName(), event, sequenceKey, String.valueOf(data), String.valueOf(newData), success.dataValue());
                            return goTo(ACTIVE).using(newData);
                         }
                )
        );

        when(
                START
                ,matchEvent(
                        Replicator.NotFound.class
                        ,(event, data) -> {
                            logger.trace("[#04] State: {}, Event: {} for sequence: {} with data: {}", stateName(), event, sequenceKey, String.valueOf(data));
                            CompletableFuture.supplyAsync(
                                    initValue,
                                    getContext().dispatcher()
                            ).thenAccept(
                                    value -> {
                                        logger.trace("[#05] State: {}, initValue::thenAccept(initValue={})", stateName(), String.valueOf(value));
                                        replicator.tell(
                                                new Update<>(
                                                        sequenceKey,
                                                        LWWRegister.create(node, value),
                                                        writeMajority,
                                                        val -> LWWRegister.create(node, val.getValue())
                                                )
                                                ,getSelf()
                                        );
                                    }
                            ).exceptionally(
                                    throwable -> {
                                        logger.error("[#06] State: {}, initValue::exceptionally(throwable={})", stateName(), throwable.getLocalizedMessage(), throwable);
                                        getSelf().tell(StateTimeout(), getSelf());
                                        return null;
                                    }
                            );
                            return stay().forMax(FIVE_SECOND_DURATION);
                        }
                )
        );

        when(
                START
                ,matchEvent(
                        UpdateSuccess.class
                        ,(event, data) -> {
                            logger.trace("[#07] State: {}, Event: {} for sequence: {} with data: {}", stateName(), event, sequenceKey, String.valueOf(data));
                            return stay().forMax(FiniteDuration.Zero());
                        }
                )
        );

        onTransition(
                matchState(
                         START
                        ,ACTIVE
                        ,(prevState, state) -> logger.trace("[#08] State: {} -> {}, for sequence: {}", prevState, state, sequenceKey)
                )
        );

        when(
                START
                ,matchEvent(
                        Request.class
                        ,(event, data) -> {
                            sender().tell(EMPTY, getSelf());
                            return stay().forMax(Duration.Zero());
                        }
                )
        );

        when (
                ACTIVE
               ,matchEvent(
                       Request.class
                      ,(event, data) -> processRequest(event, data)
                )
        );

        when (
                UPDATE
                ,matchEventEquals(
                        StateTimeout()
                        ,(event, data) -> {
                            logger.trace("[#12] State: {}, Event: {} for sequence: {} with data: {}", stateName(), event, sequenceKey, String.valueOf(data));
                            replicator.tell(
                                    new Update<>(
                                            sequenceKey,
                                            LWWRegister.create(node, BigInteger.ZERO),
                                            writeMajority,
                                            val -> {
                                                return LWWRegister.create(node, data.getStartValue().add(BigInteger.valueOf(cacheSize+1)));
                                            }
                                    )
                                    ,getSelf()
                            );
                            return stay().forMax(THREE_SECOND_DURATION);
                        }
                )
        );

        when (
                 UPDATE
                ,matchEvent(
                         Arrays.asList(UpdateFailure.class, GetFailure.class, UpdateTimeout.class)
                        ,(event, data) -> {
                            logger.trace("[#13] State: {}, Event: {} for sequence: {} with data: {}", stateName(), event, sequenceKey, String.valueOf(data));
                            return stay().forMax(THREE_SECOND_DURATION);
                        }
                )
        );

        when (
                UPDATE
                ,matchEvent(
                        UpdateSuccess.class
                        ,(event, data) -> {
                            logger.trace("[#14] State: {}, Event: {} for sequence: {} with data: {}", stateName(), event, sequenceKey, String.valueOf(data));
                            replicator.tell(
                                    new Get<>(
                                             sequenceKey
                                            ,readMajority
                                    )
                                    ,getSelf()
                            );
                            return stay().forMax(THREE_SECOND_DURATION);
                        }
                )
        );

        when (
                UPDATE
                ,matchEvent(
                        GetSuccess.class
                        ,(event, data) -> {
                            @SuppressWarnings("unchecked")
                            GetSuccess<LWWRegister<BigInteger>> success = (GetSuccess<LWWRegister<BigInteger>>)event;
                            Data newData = data.extend(success.dataValue().value());
                            logger.trace("[#15] State: {}, Event: {} for sequence: {} with data: {} -> {}", stateName(), event, sequenceKey, String.valueOf(data), String.valueOf(newData));
                            return (newData == null ? stay().forMax(ONE_SECOND_DURATION) : goTo(ACTIVE).using(newData));
                        }
                )
        );

        when (
                UPDATE
                ,matchEvent(
                        Request.class
                        ,(event, data) -> processRequest(event, data)
                )
        );

        whenUnhandled(
                matchAnyEvent(
                        (event, data) -> {
                            logger.trace("[#10] Unhandled on State: {}, Event: {} with data: {}", stateName(), event, data);
                            return stay();
                        }
                )
        );

        initialize();
    }

    FSM.State<RangeHolderFSM.State, RangeHolderFSM.Data> processRequest(Request request, Data data) {
        Data newData = data;
        if (!data.isEmpty()) {
            newData = data.copy();
            Range range = newData.takeRange(request.getSize());
            logger.trace("[#09] State: {}, Event: {} for sequence: {} with data: {} -> {}", stateName(), request, sequenceKey, String.valueOf(data), String.valueOf(newData));
            getSender().tell(range, getSelf());
        } else {
            logger.trace("[#11] State: {}, Event: {} for sequence: {} with data: {} -> EMPTY", stateName(), request, sequenceKey, String.valueOf(data));
            getSender().tell(EMPTY, getSelf());
        }
        return (newData.getSize() <= cacheSize >> 1 ? goTo(UPDATE).forMax(FiniteDuration.Zero()) : stay()).using(newData);
    }


    public interface AdminPriorityMailboxSemantics {}

    public static class AdminPriorityMailbox extends UnboundedStablePriorityMailbox implements AdminPriorityMailboxSemantics {

        public AdminPriorityMailbox(ActorSystem.Settings settings, Config config) {
            super(new PriorityGenerator() {

                          private final List<Class> classList = Arrays.asList(GetFailure.class, UpdateFailure.class, GetSuccess.class, NotFound.class, UpdateSuccess.class, UpdateTimeout.class);

                          @Override
                          public int gen(Object message) {
                              if (message != null) {
                                  if (classList.contains(message.getClass()))
                                      return 0;
                                  else if (message.getClass().getSimpleName().contains("TimeoutMarker"))
                                      return 1;
                                  else if (message.equals(PoisonPill.getInstance()))
                                      return 3;
                              }
                              return 2;
                          }

                      }
            );
        }
    }


}
