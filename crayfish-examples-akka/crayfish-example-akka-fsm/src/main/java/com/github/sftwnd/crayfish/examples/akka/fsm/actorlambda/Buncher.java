package com.github.sftwnd.crayfish.examples.akka.fsm.actorlambda;

/**
 * Created by ashindarev on 26.02.17.
 */
//#simple-imports

import akka.actor.AbstractFSM;
import akka.actor.ActorRef;
import akka.japi.pf.UnitMatch;
import scala.concurrent.duration.Duration;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.github.sftwnd.crayfish.examples.akka.fsm.actorlambda.Buncher.State.Active;
import static com.github.sftwnd.crayfish.examples.akka.fsm.actorlambda.Buncher.State.Idle;
import static com.github.sftwnd.crayfish.examples.akka.fsm.actorlambda.Buncher.Uninitialized.Uninitialized;
import static com.github.sftwnd.crayfish.examples.akka.fsm.actorlambda.Events.*;
//#simple-imports

//#simple-fsm
public class Buncher extends AbstractFSM<Buncher.State, Buncher.Data> {
    {
        //#fsm-body
        startWith(Idle, Uninitialized);

        //#when-syntax
        when(Idle,
                matchEvent(SetTarget.class, Uninitialized.class,
                        (setTarget, uninitialized) ->
                                stay().using(new Todo(setTarget.getRef(), new LinkedList<>())))
            );
        //#when-syntax
        //#when-syntax

        //#transition-elided
        onTransition(
                matchState(Active, Idle, () -> {
                    // reuse this matcher
                    final UnitMatch<Data> m = UnitMatch.create(
                            matchData(Todo.class, todo -> todo.getTarget().tell(new Batch(todo.getQueue()), self())));
                    m.match(stateData());
                }).
                        state(Idle, Active, () -> {/* Do something here */}));
        //#transition-elided

        when(Active, Duration.create(1, "second"),
                matchEvent(Arrays.asList(Flush.class, StateTimeout()), Todo.class,
                        (event, todo) -> goTo(Idle).using(todo.copy(new LinkedList<>()))));

        //#unhandled-elided
        whenUnhandled(
                matchEvent(Queue.class, Todo.class,
                        (queue, todo) -> goTo(Active).using(todo.addElement(queue.getObj()))).
                        anyEvent((event, state) -> {
                            log().warning("received unhandled request {} in state {}/{}",
                                    event, stateName(), state);
                            return stay();
                        }));
        //#unhandled-elided

        initialize();
        //#fsm-body
    }
    //#simple-fsm

    static
            //#simple-state
            // states
    enum State {
        Idle, Active
    }

    //#simple-state
    static
            //#simple-state
            // state data
    interface Data {
    }

    //#simple-state
    static
            //#simple-state
    enum Uninitialized implements Data {
        Uninitialized
    }

    //#simple-state
    static
    //#simple-state
    final class Todo implements Data {
        private final ActorRef target;
        private final List<Object> queue;

        public Todo(ActorRef target, List<Object> queue) {
            this.target = target;
            this.queue = queue;
        }

        public ActorRef getTarget() {
            return target;
        }

        public List<Object> getQueue() {
            return queue;
        }
        //#boilerplate

        @Override
        public String toString() {
            return "Todo{" +
                    "target=" + target +
                    ", queue=" + queue +
                    '}';
        }

        public Todo addElement(Object element) {
            List<Object> nQueue = new LinkedList<>(queue);
            nQueue.add(element);
            return new Todo(this.target, nQueue);
        }

        public Todo copy(List<Object> queue) {
            return new Todo(this.target, queue);
        }

        public Todo copy(ActorRef target) {
            return new Todo(target, this.queue);
        }
        //#boilerplate
    }
    //#simple-state
    //#simple-fsm
}
//#simple-fsm