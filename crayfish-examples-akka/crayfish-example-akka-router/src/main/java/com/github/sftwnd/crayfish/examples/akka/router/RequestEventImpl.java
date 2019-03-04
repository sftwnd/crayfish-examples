package com.github.sftwnd.crayfish.examples.akka.router;

public class RequestEventImpl implements RequestEvent {

    private long id;

    public RequestEventImpl(long id) {
        this.id = id;
    }

    @Override
    public long getId() {
        return 0;
    }

}
