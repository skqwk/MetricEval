package ru.skqwk.scheduler.sandbox.actor.impl;

import lombok.RequiredArgsConstructor;
import ru.skqwk.scheduler.sandbox.actor.TalkerActor;
import ru.skqwk.scheduler.sandbox.actor.message.Message;
import ru.skqwk.scheduler.sandbox.actor.message.ReadyRqMsg;
import ru.skqwk.scheduler.sandbox.actor.message.StartRqMsg;
import ru.skqwk.scheduler.sandbox.actor.message.TickMessage;
import ru.skqwk.scheduler.sandbox.props.Props;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

/**
 * Простейший счетчик запросов, который отслеживает время последнего запроса
 * и дает команду, когда можно выполнить очередной запрос, чтобы не нарушить RPM
 * В случае если выполнение запроса задержалось
 */
@RequiredArgsConstructor
public class FastRequestCounterActor implements TalkerActor {
    private final Props props;
    private LocalDateTime nextRequestTime;

    @Override
    public List<Message> onMessage(Message input) {
        if (input instanceof TickMessage message) {
            LocalDateTime now = message.getNow();
            if (nextRequestTime == null) {
                nextRequestTime = now;
            }

            if (now.isAfter(nextRequestTime) || now.isEqual(nextRequestTime)) {
                // updateNextRequestTime(now);
                // пока появилось уведомление, что запрос выполнен спамим сообщение, чтобы хоть кто-то взял его в работу
                return Collections.singletonList(new ReadyRqMsg(now));
            }
        } else if (input instanceof StartRqMsg message) {
            updateNextRequestTime(message.getNow());
        }

        return Collections.emptyList();
    }

    private void updateNextRequestTime(LocalDateTime now) {
        this.nextRequestTime = now.plus(props.getIntervalInMsBetweenRequests(), ChronoUnit.MILLIS);
    }
}
