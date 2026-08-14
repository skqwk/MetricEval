package ru.skqwk.scheduler.sandbox.actor;

import ru.skqwk.scheduler.sandbox.actor.message.Message;

import java.util.List;

public interface TalkerActor {
    List<Message> onMessage(Message message);
}
