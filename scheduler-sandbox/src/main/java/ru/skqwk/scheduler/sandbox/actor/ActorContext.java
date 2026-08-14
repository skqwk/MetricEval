package ru.skqwk.scheduler.sandbox.actor;

import java.util.List;

public interface ActorContext {
    String addActor(TalkerActor actor);

    List<TalkerActor> getAll();
}
