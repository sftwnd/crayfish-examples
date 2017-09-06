package com.github.sftwnd.crayfish.examples.akka.fsm.actorlambda;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedList;

//#test-code
public class BuncherTest extends AbstractJavaTest {

    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create("BuncherTest");
    }

    @AfterClass
    public static void tearDown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testBuncherActorBatchesCorrectly() {
        new TestKit(system) {{
            final ActorRef buncher =
                    system.actorOf(Props.create(Buncher.class));
            final ActorRef probe = getRef();

            buncher.tell(new Events.SetTarget(probe), probe);
            buncher.tell(new Events.Queue(42), probe);
            buncher.tell(new Events.Queue(43), probe);
            LinkedList<Object> list1 = new LinkedList<>();
            list1.add(42);
            list1.add(43);
            expectMsgEquals(new Events.Batch(list1));
            buncher.tell(new Events.Queue(44), probe);
            buncher.tell(Events.Flush.Flush, probe);
            buncher.tell(new Events.Queue(45), probe);
            LinkedList<Object> list2 = new LinkedList<>();
            list2.add(44);
            expectMsgEquals(new Events.Batch(list2));
            LinkedList<Object> list3 = new LinkedList<>();
            list3.add(45);
            expectMsgEquals(new Events.Batch(list3));
            system.stop(buncher);
        }};
    }

    @Test
    public void testBuncherActorDoesntBatchUninitialized() {
        new TestKit(system) {{
            final ActorRef buncher =
                    system.actorOf(Props.create(Buncher.class));
            final ActorRef probe = getRef();

            buncher.tell(new Events.Queue(42), probe);
            expectNoMsg();
            system.stop(buncher);
        }};
    }
}
//#test-code