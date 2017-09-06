package com.github.sftwnd.crayfish.examples.akka.mgmtactor.event;

/**
 * Created by ashindarev on 06.03.17.
 */
public class NodeEvent implements AdminEvent, NamedEvent {

    private String name;
    private String actorBeanName;

    public NodeEvent(String name, String actorBeanName) {
        this.name = name;
        this.actorBeanName = actorBeanName;
    }

    @Override
    public String getName() {
        return this.name;
    }


    public String getActorBeanName() {
        return this.actorBeanName;
    }

}
