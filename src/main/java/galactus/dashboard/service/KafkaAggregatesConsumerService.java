package galactus.dashboard.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public class KafkaAggregatesConsumerService {
    @Autowired
    SimpMessagingTemplate template;

    @KafkaListener(topics="recentchange.event_count")
    public void consume(@Payload String message) {
        if(isNumeric(message)) {
            template.convertAndSend("/topic/recentchange-event-count", message);
        }

    }
    public  boolean isNumeric(String str)
    {
        try
        {
            @SuppressWarnings("unused")
            double d = Double.parseDouble(str);
        }
        catch(NumberFormatException nfe)
        {
            return false;
        }
        return true;
    }
}
