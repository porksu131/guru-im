package com.guru.im.mq.starter;

import com.guru.im.mq.starter.core.DefaultMQMessageSender;
import com.guru.im.mq.starter.core.MQMessageSender;
import com.guru.im.mq.starter.core.handler.MQMessageHandler;
import com.guru.im.mq.starter.core.handler.MQMessageHandlerRegistry;
import com.guru.im.mq.starter.core.handler.MQMessageProcessor;
import com.guru.im.mq.starter.core.retry.DistributedRetryService;
import com.guru.im.mq.starter.core.retry.RetryStrategyExecutor;
import com.guru.im.mq.starter.core.router.MQMessageAnnotationRouter;
import com.guru.im.mq.starter.core.router.MQMessageRouter;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

@AutoConfiguration
@ConditionalOnProperty(name = "rocketmq.name-server")
public class RocketmqAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public MQMessageHandlerRegistry mqMessageHandlerRegistry() {
        return new MQMessageHandlerRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public MQMessageRouter mqMessageRouter(MQMessageHandlerRegistry mqMessageHandlerRegistry) {
        return new MQMessageAnnotationRouter(mqMessageHandlerRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public DistributedRetryService distributedRetryService(RocketMQTemplate rocketMQTemplate,
                                                           RedisTemplate<String, Object> redisTemplate,
                                                           RetryStrategyExecutor retryExecutor) {
        return new DistributedRetryService(redisTemplate, rocketMQTemplate, retryExecutor);
    }

    @Bean
    @ConditionalOnMissingBean
    public RetryStrategyExecutor retryStrategyExecutor() {
        return new RetryStrategyExecutor();
    }


    @Bean
    public MQMessageSender mqMessageSender(RocketMQTemplate rocketMQTemplate,
                                           DistributedRetryService distributedRetryService) {
        return new DefaultMQMessageSender(rocketMQTemplate, distributedRetryService);
    }


    @Configuration(proxyBeanMethods = false)
    static class MQProcessorRegistryConfiguration implements SmartInitializingSingleton {

        @Autowired
        private ApplicationContext applicationContext;

        @Autowired
        private MQMessageHandlerRegistry mqMessageHandlerRegistry;

        @Override
        public void afterSingletonsInstantiated() {
            registerProcessors();
        }

        public void registerProcessors() {
            // 查找 ApplicationContext 中所有带有 @MQMessageProcessor 注解的 Bean
            Map<String, Object> processors = applicationContext.getBeansWithAnnotation(MQMessageProcessor.class);
            processors.forEach((beanName, bean) -> {
                if (bean instanceof MQMessageHandler) {
                    MQMessageProcessor annotation = bean.getClass().getAnnotation(MQMessageProcessor.class);
                    mqMessageHandlerRegistry.register(annotation.value(), (MQMessageHandler) bean);
                    System.out.println("Registered MQ message processor: " + annotation.value() + " -> " + bean.getClass().getName());
                }
            });
        }
    }

}
