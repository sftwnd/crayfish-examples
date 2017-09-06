package com.github.sftwnd.crayfish.examples.akka.mgmtactor;

import akka.actor.AbstractFSM;
import akka.actor.ActorRef;
import com.sftwnd.crayfish.akka.spring.di.SpringExtension;
import com.github.sftwnd.crayfish.examples.akka.mgmtactor.event.NodeRegisterEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.Option;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

/**
 * Created by ashindarev on 06.03.17.
 */
@Component("crayfish-AdminActor")
@Scope("prototype")
@Profile("crayfish-example-akka-mgmtactor")
@DependsOn("crayfish-actorSystem")
public class AdminActor extends AbstractFSM<AdminActor.State, Object> {

    protected static enum State {Init, Active};

    @Autowired
    ApplicationContext applicationContext;

    public AdminActor() {

        startWith(State.Init, null, FiniteDuration.Zero());

        when( State.Init
             ,matchEventEquals(StateTimeout(), (event, data) -> goTo(State.Active)) );

        when( State.Init
             ,matchAnyEvent((event, data) -> {
                    context().system().scheduler().scheduleOnce(
                        FiniteDuration.create(10, TimeUnit.MILLISECONDS), self(), event, context().system().dispatcher(), sender()
                    );
                    return stay();
                  }
             )
        );

        when( State.Active
             ,matchEvent(
                  NodeRegisterEvent.class
                 ,(event, data) -> {
                      Option<ActorRef> option = context().child(event.getName());
                      if (!option.isDefined()) {
                            context().actorOf(SpringExtension.SpringExtProvider.get(context().system()).props(event.getActorBeanName()), getActorName(applicationContext, event.getActorBeanName()));
                      }
                      return stay();
                  }
              )
        );

    }

    private static final String getActorName(ApplicationContext applicationContext, String beanName) {
        String actorBeanName = new StringBuilder(beanName).append(".name").toString();
        return applicationContext.containsBean(actorBeanName)
               ? applicationContext.getBean(actorBeanName, String.class)
               : beanName;
    }

}
