package it.redbyte.springlivestomp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChangeEventMessage {
    private Object subject;
    private ChangeEventType changeType;

    public static ChangeEventMessage fromEvent(ChangeEvent event) {
        return ChangeEventMessage.builder()
                .changeType(event.getChangeType())
                .subject(event.getSource())
                .build();
    }
}
