package com.github.sftwnd.crayfish.examples.akka.mgmtactor.event;

/**
 * Created by ashindarev on 06.03.17.
 */
public class NodeRegisterEvent extends NodeEvent {

    public NodeRegisterEvent(String name, String actorBeanname) {
        super(name, actorBeanname);
    }

    public NodeRegisterEvent(String actorBeanname) {
        super(null, actorBeanname);
    }

}