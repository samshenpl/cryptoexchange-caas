package com.crypto.exchange.caas.server;

import com.crypto.exchange.aeronservice.client.AeronServiceDBClientConfig;
import com.crypto.exchange.caas.service.service.IInboundEventHandler;
import com.crypto.exchange.caas.service.service.adapter.aeron.AeronInboundEventHandler;
import com.crypto.exchange.caas.service.service.adapter.order.ChildOrderClient;
import com.crypto.exchange.caas.service.service.childorder.ChildOrderService;
import com.crypto.exchange.caas.service.service.event.OrderAdapter;
import com.crypto.exchange.caas.service.service.event.DisruptorOutboundEventRouter;
import com.crypto.exchange.caas.service.service.event.OutboundEventHandler;
import com.crypto.exchange.caas.service.service.metrics.InboundEventMetrics;
import com.crypto.exchange.oms.common.aeron.IOmsAeronService;
import com.crypto.exchange.oms.common.aeron.cache.IPendingChildOrderTracker;
import com.crypto.exchange.oms.common.aeron.channel.ChannelRegistry;
import com.crypto.exchange.oms.common.aeron.channel.IAeronChannelFactory;
import com.crypto.exchange.oms.common.aeron.codec.ChildOrderCodec;
import com.crypto.exchange.oms.common.aeron.codec.ParentOrderCodec;
import com.crypto.exchange.oms.common.aeron.codec.PostTradeCodec;
import com.crypto.exchange.oms.common.aeron.config.OmsAeronClientConfig;
import com.crypto.exchange.oms.common.aeron.config.OmsChannelConfig;
import com.crypto.exchange.oms.common.aeron.event.AeronOrderEventHandler;
import com.crypto.exchange.oms.common.aeron.event.DisruptorAeronOrderEventRouter;
import com.crypto.exchange.oms.common.aeron.listener.ChildOrderResponseListenerFactory;
import com.crypto.exchange.oms.common.aeron.listener.ParentOrderRequestListenerFactory;
import com.crypto.exchange.oms.common.aeron.recovery.IRecoveryController;
import com.crypto.exchange.oms.common.aeron.request.ChildOrderRequestHandler;
import com.crypto.exchange.oms.common.aeron.request.IChildRequestPublisherResolver;
import com.crypto.exchange.oms.common.aeron.request.IParentOrderRequestRejectHandler;
import com.crypto.exchange.oms.common.aeron.request.ParentOrderRequestHandler;
import com.crypto.exchange.oms.common.aeron.request.ParentOrderRequestValidator;
import com.crypto.exchange.oms.common.aeron.response.ChildOrderResponseHandler;
import com.crypto.exchange.oms.common.aeron.response.ChildOrderResponseReporter;
import com.crypto.exchange.oms.common.aeron.response.ParentOrderResponseHandler;
import com.crypto.exchange.oms.common.aeron.rest.traderepository.TradeRepositoryClient;
import com.crypto.exchange.oms.common.aeron.util.AeronOrderStore;
import com.crypto.exchange.oms.common.aeron.util.AeronSequenceStore;
import com.crypto.exchange.oms.common.aeron.util.IAeronSequenceStore;
import com.crypto.exchange.oms.common.core.IClock;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

@Slf4j
@Configuration
@EnableConfigurationProperties
@Import(OmsAeronClientConfig.class)
@ComponentScan(basePackages = {
    "com.crypto.exchange.oms.common.aeron.config",
    "com.crypto.exchange.oms.common.aeron.codec",
    "com.crypto.exchange.oms.common.aeron.listener",
    "com.crypto.exchange.oms.common.aeron.util",
    "com.crypto.exchange.oms.common.aeron.channel",
    "com.crypto.exchange.oms.common.aeron.cache",
    "com.crypto.exchange.oms.common.aeron.event",
    "com.crypto.exchange.oms.common.aeron.request",
    "com.crypto.exchange.oms.common.aeron.response",
    "com.crypto.exchange.oms.common.aeron",
    "com.crypto.exchange.oms.common.core",
    "com.crypto.exchange.agency.service.requestsource",
    "com.crypto.exchange.agency.service.metrics",
    "com.crypto.exchange.agency.service.adapter.aeron",
    "com.crypto.exchange.agency.service.adapter.order",
    "com.crypto.exchange.agency.service.event",
    "com.crypto.exchange.agency.service.childorder",
    "com.crypto.exchange.agency.web",
    "com.crypto.exchange.agency.listener",
    "com.crypto.exchange.agency.codec",
},
excludeFilters = {
    @Filter(type = FilterType.REGEX, pattern = {
        "com.crypto.exchange.oms.common.aeron.listener.TexoChildOrderResponseListener",
        "com.crypto.exchange.oms.common.aeron.codec.PostTradeCodec",
//        "com.crypto.exchange.oms.common.aeron.codec.ParentOrderCodec",
//        "com.crypto.exchange.oms.common.aeron.rest.traderepository.*",
//        "com.crypto.exchange.oms.common.aeron.recovery.*"
//        "com.crypto.exchange.oms.common.aeron.util.AeronSequenceStore",
    })
})
public class CaasServerAppConfig {

  @Autowired OmsChannelConfig omsChannelConfig;

  @Autowired AeronSequenceStore sequenceStore;

  @Autowired ParentOrderCodec parentOrderCodec;
  @Autowired ParentOrderRequestListenerFactory parentOrderRequestListenerFactory;
  @Autowired ChildOrderCodec childOrderCodec;
  @Autowired PostTradeCodec postTradeCodec;

  @Autowired ChildOrderResponseListenerFactory childOrderResponseListenerFactory;

  @Autowired IAeronSequenceStore aeronSequenceStore;
  @Autowired AeronOrderStore aeronOrderStore;

  @Autowired IAeronChannelFactory aeronChannelFactory;
  @Autowired ChannelRegistry channelRegistry;

  @Autowired IPendingChildOrderTracker pendingChildOrderTracker;

  @Autowired
  InboundEventMetrics metricsReporter;
  @Autowired IRecoveryController agencyRecoveryController;

  @Autowired
  AeronInboundEventHandler aeronInboundEventHandler;
  @Autowired IClock clock;
  @Autowired List<IParentOrderRequestRejectHandler> parentOrderRejectHandlers;
  @Autowired ParentOrderRequestValidator parentOrderRequestValidator;
  @Autowired ParentOrderRequestHandler parentOrderRequestHandler;
  @Autowired ParentOrderResponseHandler parentOrderResponseHandler;

  @Autowired IChildRequestPublisherResolver childRequestChannelResolver;
  @Autowired ChildOrderResponseReporter childOrderResponseReporter;
  @Autowired ChildOrderRequestHandler childOrderRequestHandler;
  @Autowired ChildOrderResponseHandler childOrderResponseHandler;
  @Autowired DisruptorAeronOrderEventRouter disruptorAeronOrderEventRouter;
  @Autowired IOmsAeronService omsAeronService;
  @Autowired
  OrderAdapter parentOrderOutboundEventListener;
  @Autowired
  OutboundEventHandler outboundEventHandler;
  @Autowired
  DisruptorOutboundEventRouter outboundEventRouter;

  @Autowired
  IInboundEventHandler inboundEventHandler;
  @Autowired ChildOrderClient childOrderClient;
  @Autowired ChildOrderService childOrderService;

  @Autowired AeronServiceDBClientConfig aeronServiceDBClientConfig;
  @Autowired TradeRepositoryClient tradeRepositoryClient;
  @Autowired AeronOrderEventHandler aeronOrderEventHandler;

  @PostConstruct
  public void init() {
    log.info("Caas server init {} {} {} {}", tradeRepositoryClient, parentOrderCodec, outboundEventRouter, parentOrderOutboundEventListener);
    outboundEventRouter.start();
    parentOrderOutboundEventListener.start();
  }
}
