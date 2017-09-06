package com.github.sftwnd.crayfish.examples.akka.mgmtactor;

import akka.actor.AbstractFSM;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Created by ashindarev on 06.03.17.
 */
@Component("crayfish-AmqpActor")
@Scope("prototype")
@Profile("crayfish-example-akka-mgmtactor")
@DependsOn("crayfish-actorSystem")
public class AmqpActor extends AbstractFSM<AmqpActor.State, Object> {

    protected static enum State {Init, Active};

    @Autowired
    ApplicationContext applicationContext;

}
