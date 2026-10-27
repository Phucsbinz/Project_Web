package com.utetra.support;
import org.springframework.stereotype.Component;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.Map;
@Component
public class SupportEvents {
 public record Changed(String conversationId,long messageId){}
 private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(SupportEvents.class);
 private final SupportRepository repo;private final SimpMessagingTemplate broker;
 public SupportEvents(SupportRepository repo,SimpMessagingTemplate broker){this.repo=repo;this.broker=broker;}
 @TransactionalEventListener @Transactional(propagation=Propagation.REQUIRES_NEW,readOnly=true)
 public void committed(Changed event){try{var hint=Map.of("conversationId",event.conversationId(),"messageId",event.messageId());for(long id:repo.participants(event.conversationId()))try{broker.convertAndSendToUser(Long.toString(id),"/queue/support",hint);}catch(RuntimeException failure){LOG.warn("Support notification unavailable; clients can poll committed messages.");}}catch(RuntimeException failure){LOG.warn("Support notification lookup unavailable; clients can poll committed messages.");}}
}
