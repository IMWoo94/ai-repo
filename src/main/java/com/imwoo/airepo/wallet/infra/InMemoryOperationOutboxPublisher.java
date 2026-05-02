package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.wallet.application.OperationOutboxPublisher;
import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

@Component
public class InMemoryOperationOutboxPublisher implements OperationOutboxPublisher {

    private final List<OperationOutboxEvent> publishedEvents = new CopyOnWriteArrayList<>();

    @Override
    public void publish(OperationOutboxEvent outboxEvent) {
        publishedEvents.add(outboxEvent);
    }

    public List<OperationOutboxEvent> publishedEvents() {
        return List.copyOf(publishedEvents);
    }
}
