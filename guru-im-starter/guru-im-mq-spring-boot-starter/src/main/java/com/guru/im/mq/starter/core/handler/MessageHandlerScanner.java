package com.guru.im.mq.starter.core.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MessageHandlerScanner implements ApplicationContextAware {
    
    @Autowired
    private MQMessageHandlerRegistry registry;
    
    @Override
    public void setApplicationContext(ApplicationContext context) {
        Map<String, Object> beans = context.getBeansWithAnnotation(MQMessageProcessor.class);
        beans.forEach((name, bean) -> {
            if (bean instanceof MQMessageHandler) {
                MQMessageProcessor annotation = bean.getClass().getAnnotation(MQMessageProcessor.class);
                registry.register(annotation.value(), (MQMessageHandler)bean);
            }
        });
    }
}