package it.redbyte.springlivestomp;

import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public abstract class ChangeEventListenerConfigurer implements ApplicationListener<ChangeEvent> {
    private final SimpMessagingTemplate messagingTemplate;
    private final ChangeEventMappings changeEventMappings;

    public ChangeEventListenerConfigurer(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        changeEventMappings = new ChangeEventMappings();
        configureChangeEventMappings(changeEventMappings);
    }

    @Override
    public void onApplicationEvent(ChangeEvent event) {
        this.changeEventMappings.getForClass(event.getClass(), event)
                .subscribe((endpoint) -> this.messagingTemplate.convertAndSend(endpoint, ChangeEventMessage.fromEvent(event)));
    }

    public abstract void configureChangeEventMappings(ChangeEventMappings changeEventMappings);
}
