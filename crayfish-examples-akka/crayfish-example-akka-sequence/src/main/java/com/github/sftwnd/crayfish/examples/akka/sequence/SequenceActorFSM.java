package com.github.sftwnd.crayfish.examples.akka.sequence;

import akka.actor.AbstractFSM;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Status;
import akka.cluster.Cluster;
import akka.cluster.ddata.*;
import akka.cluster.ddata.Replicator.ReadConsistency;
import akka.cluster.ddata.Replicator.WriteConsistency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component("sequenceActorFSM")
@Scope("prototype")
//@Profile(value = "crayfish-example-akka-sequence")
public class SequenceActorFSM extends AbstractFSM<SequenceActorFSM.State, SequenceActorFSM.Data> {

    private static final Logger logger = LoggerFactory.getLogger(SequenceActorFSM.class);

    private static final String CCIRQ_ID_KEY="ccirq-id";

    public enum Request {
        NEXT
    }

    public enum State {
        INIT, GET, ACTIVE
    }

    @Autowired
    ApplicationContext applicationContext;

    public SequenceActorFSM(final BigInteger initValue, final int incrementBy) {

        super();

        final ActorSystem                      actorSystem = context().system();
        final Cluster                                 node = Cluster.get(actorSystem);
        final ActorRef                          replicator = DistributedData.get(actorSystem).replicator();
        final Key<LWWRegister<BigInteger>>     sequenceKey = LWWRegisterKey.create(CCIRQ_ID_KEY);
        final ReadConsistency                      readAll = new Replicator.ReadAll(FiniteDuration.create(3, TimeUnit.SECONDS));
        final ReadConsistency                 readMajority = new Replicator.ReadMajority(FiniteDuration.create(3, TimeUnit.SECONDS));
                                                             //Replicator.readLocal();
        final WriteConsistency            writeConsistency = new Replicator.WriteAll(FiniteDuration.create(3, TimeUnit.SECONDS)); // Replicator.writeLocal();
        final BigInteger                       incrementer = BigInteger.valueOf(incrementBy);

        startWith(State.INIT, new SequenceActorFSM.Data(), FiniteDuration.Zero());

        // Делаем запрос Get для запроса данных, если они есть
        when( State.INIT
             ,matchEventEquals(
                   StateTimeout()
                  ,(event, data) -> {
                       replicator.tell(
                            new Replicator.Get<>(
                                 sequenceKey
                                ,data.getValue() == null || data.getLastValue() == null ? readAll : readMajority
                            )
                           ,getSelf()
                       );
                       logger.trace("State: {}, TimeOut event: {}", stateName(), String.valueOf(event));
                       return goTo(State.GET).forMax(Duration.create(5, TimeUnit.SECONDS));
                   }
              )
        );

        // Сохраняем запросы в список
        when( State.INIT
             ,matchEventEquals(
                   Request.NEXT
                  ,(event, data) -> {
                       /*if (!data.getRequests().contains(getSender())) {
                            getContext().watch(sender());
                       }*/
                       data.getRequests().add(getSender());
                       logger.trace("State: {}, Next event: {} from: {}", stateName(), String.valueOf(event), getSender());
                       return stay().forMax(Duration.Zero());
                   }
              )
        );

        // Если не удалось получить данные - отправляем ответ подписчикам и отписываемся от них, переходим на "Выход в Init"
        when( State.GET
             ,matchEvent(
                   Arrays.asList(Replicator.GetFailure.class, Replicator.UpdateFailure.class, Replicator.UpdateTimeout.class, StateTimeout())
                  ,(event, data) -> {
                       logger.error("Unable to Get/Update replicated value for sequence: {}. State: {}, Event: {}", String.valueOf(sequenceKey), stateName(), String.valueOf(event));
                       rejectData(event);
                       return goTo(State.INIT).forMax(Duration.create(5, TimeUnit.SECONDS));
                  }
             )
        );

        // Если ключ ещё не создавался
        when( State.GET
                ,matchEvent(
                        Replicator.NotFound.class
                        ,(event, data) -> {
                            replicator.tell(
                                    new Replicator.Update(
                                            sequenceKey,
                                            LWWRegister.create(node, initValue),
                                            writeConsistency,
                                            value -> LWWRegister.create(node, initValue)
                                    )
                                    ,getSelf()
                            );
                            logger.trace("State: {}, NotFound event: {}", stateName(), String.valueOf(event));
                            return goTo(State.GET).forMax(Duration.create(5, TimeUnit.SECONDS));
                        }
                )
        );

        // Откладываем пришедший запрос
        when( State.GET
             ,matchEventEquals(
                   Request.NEXT
                  ,(event, data) -> {
                       /*if (!data.getRequests().contains(getSender())) {
                           getContext().watch(sender());
                       }*/
                       data.getRequests().add(getSender());
                       logger.trace("State: {}, Next event: {} from: {}", stateName(), String.valueOf(event), getSender());
                       return stay().forMax(Duration.Zero());
                  }
             )
        );

        // Если успешно прочитали
        when( State.GET
                ,matchEvent(
                        Replicator.GetSuccess.class
                        ,(event, data) -> {
                            @SuppressWarnings("unchecked")
                            Replicator.GetSuccess<LWWRegister<BigInteger>> getSuccess = event;
                            logger.trace("State: {}, GetSuccess event: {}", stateName(), String.valueOf(event));
                            Data newData = processData(stateData().changeLastValue(getSuccess.dataValue().getValue()));
                            if (newData.isActive()) {
                                return goTo(State.ACTIVE).using(newData);
                            } else {
                                replicator.tell(
                                        new Replicator.Update(
                                                sequenceKey,
                                                LWWRegister.create(node, newData.getLastValue()),
                                                writeConsistency,
                                                val -> {
                                                    logger.trace("LWWRegister update value: {}", String.valueOf(val));
                                                    return LWWRegister.create(node, newData.getLastValue().add(BigInteger.valueOf(incrementBy)));
                                                }
                                        )
                                        ,getSelf()
                                );
                                return stay().using(newData).forMax(Duration.create(5, TimeUnit.SECONDS));
                            }
                        }
                )
        );

        // Обрабатываем успешное внесение изменений
        when( State.GET
                ,matchEvent(
                        Replicator.UpdateSuccess.class
                        ,(event, data) -> {
                            logger.trace("State: {}, UpdateSuccess event: {}", stateName(), String.valueOf(event));
                            replicator.tell(
                                    new Replicator.Get<>(
                                            sequenceKey
                                            ,readAll
                                    )
                                    ,getSelf()
                            );
                            return stay().forMax(Duration.create(5, TimeUnit.SECONDS));
                        }
                )
        );

        // При переходе из статусе GET в статус ACTIVE
        onTransition(
                matchState(
                         State.GET
                        ,State.ACTIVE
                        ,() -> {
                            Data newData = processData();
                            if (newData.isActive()) {
                                stay().using(newData);
                            } else {
                                goTo(State.INIT).using(newData).forMax(Duration.Zero());
                            }
                        }
                )
        );

        // Откладываем пришедший запрос
        when( State.ACTIVE
             ,matchEventEquals(
                   Request.NEXT
                  ,(event, data) -> {
                      /*if (!data.getRequests().contains(getSender())) {
                          getContext().watch(sender());
                      }*/
                      data.getRequests().add(getSender());
                      Data newData = processData();
                      if (newData.isActive()) {
                          logger.trace("State: {}, Next event: {} from: {} -> Stay", stateName(), String.valueOf(event), getSender());
                          return stay().using(newData);
                      } else {
                          logger.trace("State: {}, Next event: {} from: {} -> Init", stateName(), String.valueOf(event), getSender());
                          return goTo(State.INIT).using(newData).forMax(Duration.Zero());
                      }
                  }
             )
        );

        // Обрабатываем необработанные запросы
        whenUnhandled(
                matchAnyEvent(
                        (event, data) -> {
                            logger.info(">>> Unhandled event: {}, state: {}, with data: {}", String.valueOf(event), stateName(), data);
                            return stay().forMax(Duration.create(3, TimeUnit.SECONDS));
                        }
                )
        );

        initialize();

    }

    private Data processData() {
        return processData(stateData());
    }

    private Data processData(Data data) {
        if (data != null && data.isProcessable()) {
            BigInteger value = data.getValue();
            while (!data.getRequests().isEmpty() && value.compareTo(data.getLastValue()) < 0) {
                value = value.add(BigInteger.ONE);
                ActorRef actorRef = data.getRequests().get(0);
                actorRef.tell(value, getSelf());
                logger.trace("Message {} send to actor: {}", value, actorRef);
                data.getRequests().remove(0);
                /*if (!data.getRequests().contains(actorRef)) {
                    getContext().unwatch(actorRef);
                }*/
            }
            return data.changeValue(value);
        }
        return data;
    }

    private void rejectData(Object event) {
        stateData().getRequests().forEach(actor -> {
            actor.tell(
                    new Status.Failure(
                            new SequenceReceiveException(
                                    "Unable to receive Distributed Sequence value"
                                    ,event
                            )
                    )
                    ,getSelf()
            );
            //getContext().unwatch(actor);
        });
        stateData().getRequests().clear();
        logger.trace("State: {}, Get[Failure/TimeOut] event: {}", stateName(), String.valueOf(event));
    }

    public static class SequenceReceiveException extends Throwable {

        private Object event;

        public SequenceReceiveException(String message, Object event, Throwable throwable) {
            super(message, throwable, true, true);
            this.event = event;
        }

        public SequenceReceiveException(String message, Object event) {
            this(message, event, null);
        }

        public Object getEvent() {
            return event;
        }

    }

    public static class Data {

        private BigInteger       value;
        private BigInteger       lastValue;
        private List<ActorRef>   requests;

        public Data(BigInteger value, BigInteger lastValue, List<ActorRef> requests) {
            this.value     = value;
            this.lastValue = lastValue;
            this.requests  = requests;
            logger.trace("SequenceActorFSM.Data(value:{}, lastValue:{}, requests:{})", this.value, this.lastValue, this.requests);
        }

        public Data(BigInteger lastValue) {
            this(null, lastValue, new ArrayList<>());
        }

        public Data() {
            this(null);
        }

        public BigInteger getValue() {
            return value;
        }

        public BigInteger getLastValue() {
            return this.lastValue;
        }

        public List<ActorRef> getRequests() {return this.requests; }

        public boolean isActive() {
            return this.value != null
                && this.lastValue != null
                && this.value.compareTo(this.lastValue) < 0;
        }

        public boolean isProcessable() {
            return !this.requests.isEmpty()
                && isActive();
        }

        public Data changeLastValue(BigInteger newLastValue) {
            return newLastValue == null
                    ? new Data(null, null, this.requests)
                    : new Data( this.value == null ? newLastValue : this.value
                    ,newLastValue
                    ,this.requests );
        }

        public Data changeValue(BigInteger newValue) {
            return new Data(newValue, this.lastValue, this.requests);
        }

    }

}
