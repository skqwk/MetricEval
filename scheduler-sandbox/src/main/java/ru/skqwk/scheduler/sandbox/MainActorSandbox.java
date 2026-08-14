package ru.skqwk.scheduler.sandbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.skqwk.scheduler.sandbox.actor.ActorContext;
import ru.skqwk.scheduler.sandbox.actor.message.EndMsg;
import ru.skqwk.scheduler.sandbox.actor.message.Message;
import ru.skqwk.scheduler.sandbox.actor.message.ShutdownMsg;
import ru.skqwk.scheduler.sandbox.actor.message.TickMessage;
import ru.skqwk.scheduler.sandbox.clock.DefaultClockProvider;
import ru.skqwk.scheduler.sandbox.util.AccumulatedDuration;

import java.time.LocalDateTime;
import java.util.Queue;

@Slf4j
@RequiredArgsConstructor
public class MainActorSandbox {
    private final ActorContext context;
    private final Queue<Message> messages;
    private final DefaultClockProvider clockProvider;

    public void run(int limit, AccumulatedDuration duration) {
        int tick = 0;
        boolean paused = false;
        while (tick < limit) {
            if (paused) break;

            while (!messages.isEmpty()) {
                Message message = messages.poll();

                if (message instanceof ShutdownMsg shutdownMsg) {
                    duration.setDuration(shutdownMsg.getDuration());
                    paused = true;
                }

                process(message);
            }

            tick += 1;
            clockProvider.setTick(tick);
            TickMessage tickMessage = new TickMessage();
            messages.add(update(tickMessage, tick, LocalDateTime.now(clockProvider.getClock())));
            if (tick == limit - 1) {
                messages.add(new EndMsg());
            }
        }
    }

    private Message update(TickMessage tickMessage, int tick, LocalDateTime now) {
        tickMessage.setTick(tick);
        tickMessage.setNow(now);

        return tickMessage;
    }

    private void process(Message message) {
        context.getAll()
                .forEach(actor -> messages.addAll(actor.onMessage(message)));
    }
}
