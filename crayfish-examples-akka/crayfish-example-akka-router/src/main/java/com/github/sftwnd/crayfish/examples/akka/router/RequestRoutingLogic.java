package com.github.sftwnd.crayfish.examples.akka.router;

import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.RoutingLogic;
import akka.routing.SeveralRoutees;
import java.util.HashMap;
import scala.collection.immutable.IndexedSeq;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;

public class RequestRoutingLogic implements RoutingLogic {

    Map<Long, Routee> routeeMap = new HashMap<>();

    @Override
    public Routee select(Object message, IndexedSeq<Routee> routees) {
        List<Routee> targets = new ArrayList<Routee>();
        if (message != null && message instanceof RequestEvent) {
            @SuppressWarnings("unchecked")
            long id = ((RequestEvent)message).getId();
            if (routeeMap.containsKey(id)) {
                return routeeMap.get(id);
            } else {

            }
        }
        return new SeveralRoutees(targets);
    }
}
