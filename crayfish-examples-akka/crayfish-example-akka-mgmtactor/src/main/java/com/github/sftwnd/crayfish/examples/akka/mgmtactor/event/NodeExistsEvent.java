package com.github.sftwnd.crayfish.examples.akka.mgmtactor.event;

/**
 * Created by ashindarev on 06.03.17.
 */
public class NodeExistsEvent extends NodeEvent {

    public NodeExistsEvent(String name, String actorBeanname) {
        super(name, actorBeanname);
    }

}