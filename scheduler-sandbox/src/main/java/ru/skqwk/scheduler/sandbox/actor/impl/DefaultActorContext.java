package ru.skqwk.scheduler.sandbox.actor.impl;

import ru.skqwk.scheduler.sandbox.actor.ActorContext;
import ru.skqwk.scheduler.sandbox.actor.TalkerActor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DefaultActorContext implements ActorContext {
    private final List<TalkerActor> actors = new ArrayList<>();

    @Override
    public String addActor(TalkerActor actor) {
        actors.add(actor);
        return UUID.randomUUID().toString();
    }

    @Override
    public List<TalkerActor> getAll() {
        return new ArrayList<>(actors);
    }
}
