package com.crypto.exchange.agency.tests;

import com.crypto.commons.datamodel.oms.v1.OrderType;
import com.crypto.exchange.caas.service.codec.DefaultPostTradeCodec;
import com.crypto.exchange.caas.service.order.InboundEvent;
import com.crypto.exchange.caas.service.order.OutboundEvent;
import com.crypto.exchange.caas.service.service.IInboundEventHandler;
import com.crypto.exchange.caas.service.service.IOutboundEventRouter;
import com.crypto.exchange.caas.service.service.adapter.aeron.AeronInboundEventHandler;
import com.crypto.exchange.caas.service.service.adapter.order.ChildOrderClient;
import com.crypto.exchange.caas.service.service.adapter.order.IChildOrderClient;
import com.crypto.exchange.caas.service.service.childorder.ChildOrderService;
import com.crypto.exchange.caas.service.service.event.OrderAdapter;
import com.crypto.exchange.caas.service.service.event.DisruptorOutboundEventRouter;
import com.crypto.exchange.caas.service.service.event.InboundEventHandler;
import com.crypto.exchange.caas.service.service.event.OutboundEventHandler;
import com.crypto.exchange.caas.service.service.metrics.InboundEventMetrics;
import com.crypto.exchange.caas.service.service.requestsource.AgencyRequestSource;
import com.crypto.exchange.agency.tests.WebControllerTest.TestApp;
import com.crypto.exchange.caas.service.web.OrderController;
import com.crypto.exchange.caas.service.web.OrderRequest;
import com.crypto.exchange.oms.common.aeron.IAeronInboundEventHandler;
import com.crypto.exchange.oms.common.aeron.IRequestSourceSupplier;
import com.crypto.exchange.oms.common.aeron.OmsAeronService;
import com.crypto.exchange.oms.common.aeron.cache.IPendingChildOrderTracker;
import com.crypto.exchange.oms.common.aeron.cache.PendingChildOrderTracker;
import com.crypto.exchange.oms.common.aeron.channel.ChannelType;
import com.crypto.exchange.oms.common.aeron.channel.IAeronPublisher;
import com.crypto.exchange.oms.common.aeron.channel.IChannelRegistry;
import com.crypto.exchange.oms.common.aeron.codec.ChildOrderCodec;
import com.crypto.exchange.oms.common.aeron.codec.IChildOrderCodec;
import com.crypto.exchange.oms.common.aeron.codec.IParentOrderCodec;
import com.crypto.exchange.oms.common.aeron.codec.IPostTradeCodec;
import com.crypto.exchange.oms.common.aeron.codec.ITexoPostTradeDecoder;
import com.crypto.exchange.oms.common.aeron.codec.IVenuesCodec;
import com.crypto.exchange.oms.common.aeron.codec.ParentOrderCodec;
import com.crypto.exchange.oms.common.aeron.codec.PostTradeCodec;
import com.crypto.exchange.oms.common.aeron.codec.TexoPostTradeDecoder;
import com.crypto.exchange.oms.common.aeron.codec.VenuesCodec;
import com.crypto.exchange.oms.common.aeron.config.OmsChannelConfig;
import com.crypto.exchange.oms.common.aeron.event.AeronOrderEvent;
import com.crypto.exchange.oms.common.aeron.event.AeronOrderEventHandler;
import com.crypto.exchange.oms.common.aeron.event.DisruptorAeronOrderEventRouter;
import com.crypto.exchange.oms.common.aeron.event.IAeronOrderEventRouter;
import com.crypto.exchange.oms.common.aeron.listener.ChildOrderResponseListenerFactory;
import com.crypto.exchange.oms.common.aeron.listener.IResponseListenerFactory;
import com.crypto.exchange.oms.common.aeron.listener.ITexoChildOrderResponseListener;
import com.crypto.exchange.oms.common.aeron.listener.ParentOrderRequestListenerFactory;
import com.crypto.exchange.oms.common.aeron.listener.TexoChildOrderResponseListener;
import com.crypto.exchange.oms.common.aeron.recovery.IRecoveryController;
import com.crypto.exchange.oms.common.aeron.recovery.RecoveryController;
import com.crypto.exchange.oms.common.aeron.request.ChildOrderRequestHandler;
import com.crypto.exchange.oms.common.aeron.request.ChildRequestPublisherResolver;
import com.crypto.exchange.oms.common.aeron.request.IChildOrderRequestHandler;
import com.crypto.exchange.oms.common.aeron.request.IChildRequestPublisherResolver;
import com.crypto.exchange.oms.common.aeron.request.IParentOrderRequestHandler;
import com.crypto.exchange.oms.common.aeron.request.IParentOrderRequestRejectHandler;
import com.crypto.exchange.oms.common.aeron.request.ParentOrderRequestHandler;
import com.crypto.exchange.oms.common.aeron.request.ParentOrderRequestValidator;
import com.crypto.exchange.oms.common.aeron.response.ChildOrderResponseHandler;
import com.crypto.exchange.oms.common.aeron.response.ChildOrderResponseReporter;
import com.crypto.exchange.oms.common.aeron.response.IChildOrderResponseHandler;
import com.crypto.exchange.oms.common.aeron.response.IChildOrderResponseReporter;
import com.crypto.exchange.oms.common.aeron.response.IParentOrderResponseHandler;
import com.crypto.exchange.oms.common.aeron.response.ParentOrderResponseHandler;
import com.crypto.exchange.oms.common.aeron.util.AeronOrderStore;
import com.crypto.exchange.oms.common.aeron.util.AeronSequenceStore;
import com.crypto.exchange.oms.common.aeron.util.IAeronOrderStore;
import com.crypto.exchange.oms.common.aeron.util.IAeronSequenceStore;
import com.crypto.exchange.oms.common.aeron.util.IPreAggSourceSequenceStore;
import com.crypto.exchange.oms.common.aeron.util.PreAggSourceSequenceStore;
import com.crypto.exchange.oms.common.core.IClock;
import com.crypto.exchange.oms.common.core.RealTimeClock;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.EventHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@Slf4j
@ExtendWith(SpringExtension.class)
@WebFluxTest()
@Import(TestApp.class)
public class WebControllerTest {

  static CountDownLatch latch = new CountDownLatch(1);
  @SpringBootConfiguration
  @Configuration

  public static class TestApp {

    @Bean IChannelRegistry channelRegistry() {
      IChannelRegistry mock = Mockito.mock(IChannelRegistry.class);
      Mockito.doAnswer(s -> {
        final ChannelType channelType = s.getArgument(0);
        log.info("channel registry get publisher {}, check config", channelType);
        if (channelType == ChannelType.OMS_CHILD_REQUEST_PUBLISHER) {
          int i = 0;
          for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
            if (!stackTraceElement.getClassName().startsWith("org.mockito")) {
              System.out.println(stackTraceElement);
            }
            if (++i > 13) {
              break;
            }
          }
        }

        return new IAeronPublisher() {
          @Override
          public long send(UnsafeBuffer unsafeBuffer, int i) {
            log.info("test send unsafeBuffer.byteArray() = {}, i = {}", unsafeBuffer.byteArray().length, i );
            return 0;
          }

          @Override
          public boolean isReady() {
            return true;
          }
        };
      }).when(mock).getPublisher(Mockito.any(ChannelType.class));
      return mock;
    }

    @Bean IPendingChildOrderTracker pendingChildOrderTracker() {
      return new PendingChildOrderTracker() {
        @Override
        public void addPendingNewChild(String childOrderId) {
          log.info("add pending new child {}", childOrderId);
          super.addPendingNewChild(childOrderId);
          latch.countDown();
        }
      };
    }
    @Bean IChildRequestPublisherResolver channelResolver(IChannelRegistry channelRegistry) {
      return new ChildRequestPublisherResolver(channelRegistry);
    }
    @Bean
    PostTradeCodec postTradeCodec(IAeronSequenceStore sequenceStore, IVenuesCodec venuesCodec, IRequestSourceSupplier requestSourceSupplier, IClock clock) throws NoSuchFieldException {
      return new DefaultPostTradeCodec(sequenceStore, venuesCodec, requestSourceSupplier, clock);
    }

    @Bean
    ChildOrderResponseReporter childOrderResponseReporter(IAeronInboundEventHandler agencyAeronInboundEventHandler) {
      return new ChildOrderResponseReporter(agencyAeronInboundEventHandler);
    }

    @Bean IParentOrderResponseHandler parentOrderResponseHandler(IParentOrderCodec parentOrderCodec, IAeronOrderStore aeronOrderStore, IChannelRegistry channelRegistry, IClock clock) {
      return new ParentOrderResponseHandler(parentOrderCodec, aeronOrderStore, channelRegistry, clock);
    }

    @Bean IPreAggSourceSequenceStore preAggSourceSequenceStore(IAeronSequenceStore sequenceStore) {
      return new PreAggSourceSequenceStore(sequenceStore);
    }

    @Bean
    AeronOrderEventHandler aeronOrderEventHandler(IParentOrderRequestHandler parentOrderRequestHandler, IParentOrderResponseHandler parentOrderResponseHandler,
        IChildOrderRequestHandler childOrderRequestHandler, IChildOrderResponseHandler childOrderResponseHandler, IAeronSequenceStore sequenceStore, IChannelRegistry channelRegistry,
        IPreAggSourceSequenceStore preAggSourceSequenceStore) {
      return new AeronOrderEventHandler(parentOrderRequestHandler, parentOrderResponseHandler, childOrderRequestHandler, childOrderResponseHandler, sequenceStore, channelRegistry,
          preAggSourceSequenceStore);
    }

    @Bean ChildOrderRequestHandler childOrderRequestHandler(IAeronOrderStore aeronOrderStore,
        IPendingChildOrderTracker pendingChildOrderTracker,
        IChildOrderCodec childOrderCodec,
        IChildRequestPublisherResolver channelResolver,
        IPostTradeCodec postTradeCodec,
        IChannelRegistry channelRegistry,
        IChildOrderResponseReporter responseReporter) {
      final ChildOrderRequestHandler childOrderRequestHandler = new ChildOrderRequestHandler(aeronOrderStore, pendingChildOrderTracker, childOrderCodec,
          channelResolver,
          postTradeCodec, channelRegistry, responseReporter);
      return childOrderRequestHandler;
    }

    @Bean ParentOrderRequestListenerFactory parentOrderRequestListenerFactory(IParentOrderCodec parentOrderCodec) {
      return new ParentOrderRequestListenerFactory(parentOrderCodec);
    }
    @Bean IRequestSourceSupplier requestSourceSupplier() {
      return new AgencyRequestSource();
    }
    @Bean IChildOrderCodec childOrderCodec(IVenuesCodec venuesCodec, IRequestSourceSupplier requestSourceSupplier, IClock clock) {
      return new ChildOrderCodec(venuesCodec, requestSourceSupplier, clock);
    }
    @Bean ITexoPostTradeDecoder texoPostTradeDecoder(IRequestSourceSupplier requestSourceSupplier) {
      return new TexoPostTradeDecoder(requestSourceSupplier);
    }
    @Bean ITexoChildOrderResponseListener texoChildOrderResponseListener(ITexoPostTradeDecoder texoPostTradeDecoder) {
      return new TexoChildOrderResponseListener(texoPostTradeDecoder);
    }
    @Bean
    ChildOrderResponseListenerFactory childOrderResponseListenerFactory(IChildOrderCodec childOrderCodec, ITexoPostTradeDecoder texoPostTradeDecoder) {
      return new ChildOrderResponseListenerFactory(childOrderCodec, texoPostTradeDecoder);
    }
    @Bean DisruptorAeronOrderEventRouter aeronOrderEventRouter(EventHandler<AeronOrderEvent>[] handlers, IResponseListenerFactory[] listenerFactories,
        ITexoChildOrderResponseListener childOrderResponseListener) {
      return new DisruptorAeronOrderEventRouter(handlers, 1024, listenerFactories, childOrderResponseListener);
    }
    @Bean OmsChannelConfig getChannelConfig() {
      return new OmsChannelConfig();
    }

    @Bean IAeronSequenceStore sequenceStore(OmsChannelConfig channelConfig) {
      return new AeronSequenceStore(channelConfig);
    }

    @Bean IVenuesCodec venuesCodec() {
      return new VenuesCodec();
    }

    @Bean IParentOrderCodec parentOrderCodec(IAeronSequenceStore sequenceStore, IVenuesCodec venuesCodec, IRequestSourceSupplier requestSourceSupplier, IClock clock) {
      final ParentOrderCodec parentOrderCodec = new ParentOrderCodec(sequenceStore, venuesCodec, requestSourceSupplier, clock);
      return parentOrderCodec;
    }
    @Bean IAeronOrderStore aeronOrderStore() {
     return new AeronOrderStore();
    }
    @Bean
    AeronInboundEventHandler agencyAeronInboundEventHandler() {
      final AeronInboundEventHandler aeronInboundEventHandler = new AeronInboundEventHandler();
      return aeronInboundEventHandler;
    }
    @Bean
    OutboundEventHandler agencyOutboundEventHandler() {
      return new OutboundEventHandler();
    }

    @Bean
    RecoveryController recoveryController() {
      return Mockito.mock(RecoveryController.class);
    }

    @Bean
    OmsAeronService omsAeronService(IAeronOrderEventRouter orderEventRouter, IChannelRegistry channelRegistry, IRecoveryController recoveryController) {
      return new OmsAeronService(orderEventRouter, channelRegistry, recoveryController);
    }

    @Bean
    OrderAdapter agencyAeronOrderAdapter() {
      return new OrderAdapter();
    }

    @Bean
    DisruptorOutboundEventRouter agencyDisruptorOutboundEventRouter(EventHandler<OutboundEvent> eventHandlers) {
      return new DisruptorOutboundEventRouter(eventHandlers, 1024);
    }

    @Bean
    InboundEventMetrics agencyInboundEventMetrics() {
      return new InboundEventMetrics();
    }
    @Bean
    InboundEventHandler agencyInboundEventHandler(Consumer<InboundEvent> agencyInboundEventMetrics) {
      return new InboundEventHandler(agencyInboundEventMetrics);
    }




    @Bean IChildOrderClient childOrderClient(IOutboundEventRouter outboundEventRouter, IInboundEventHandler inboundEventHandler) {
      return new ChildOrderClient(outboundEventRouter, inboundEventHandler);
    }

    @Bean
    ChildOrderService childOrderService(IChildOrderClient childOrderClient, AeronInboundEventHandler eventHandler) {
      return new ChildOrderService(childOrderClient, eventHandler);
    }
    @Bean
    IParentOrderRequestHandler parentOrderRequestHandler (
        IChannelRegistry channelRegistry,
        IParentOrderCodec parentOrderCodec,
        IAeronOrderStore aeronOrderStore,
        IAeronInboundEventHandler agencyAeronInboundEventHandler, IParentOrderRequestRejectHandler rejectHandler, IClock clock) {
      return new ParentOrderRequestHandler(channelRegistry,
          parentOrderCodec,
          aeronOrderStore,
          List.of(rejectHandler),
          clock,
          agencyAeronInboundEventHandler,
          new ParentOrderRequestValidator(aeronOrderStore)

      );
    }

    @Bean RealTimeClock clock() {
      return new RealTimeClock();
    }

    @Bean
    OrderController agencyOrderController() {
      return new OrderController();
    }

    @Bean
    ChildOrderResponseHandler childOrderResponseHandler(IPendingChildOrderTracker pendingChildOrderTracker, IAeronOrderStore aeronOrderStore, IPostTradeCodec postTradeCodec,
        IChannelRegistry channelRegistry, IChildOrderResponseReporter childOrderResponseReporter, IClock clock) {
      return new ChildOrderResponseHandler(pendingChildOrderTracker, aeronOrderStore, postTradeCodec, channelRegistry,
          childOrderResponseReporter, clock);
    }


    @Autowired
    DisruptorOutboundEventRouter agencyDisruptorOutboundEventRouter;
    @Autowired DisruptorAeronOrderEventRouter disruptorAeronOrderEventRouter;

    @Autowired OmsAeronService omsAeronService;
    @Autowired
    InboundEventHandler agencyInboundEventHandler;

    @Autowired ChildOrderRequestHandler childOrderRequestHandler;
    @Autowired ChildOrderResponseHandler childOrderResponseHandler;
    @PostConstruct
    void init() {
      agencyDisruptorOutboundEventRouter.start();
      omsAeronService.start();
    }
  }

  @Autowired
  OrderController agencyOrderController;
  @Autowired private WebTestClient webTestClient;

  @Test
  public void testWeb() throws JsonProcessingException, InterruptedException, TimeoutException {
    final ObjectMapper objectMapper = new ObjectMapper();
    Map<String,Object> reqMap = new HashMap<>();
    reqMap.put("symbol", "BTC_USDT");
    reqMap.put("orderParams", Map.of("0", "-", "1", -1L));
    reqMap.put("requestType", (byte) 0);
    reqMap.put("clientOrderId", "xxxxxxx-clientorderid");
    reqMap.put("customerClientOrderId", "xxx-customerClientOrderId");
    reqMap.put("price", 1L);
    reqMap.put("quantity", 3L);
    reqMap.put("account", "xx-account-xxxx");
    reqMap.put("orderType", OrderType.LIMIT);
    reqMap.put("timeInForceType", (byte) 2); //TimeInForceType.FILL_OR_KILL);
    reqMap.put("orderSideType", 0); //OrderSideType.BUY)
    reqMap.put("orderLevelType", 0);//OrderLevelType.PARENT)
    reqMap.put("route", 1); // AGENCY
    reqMap.put("destination", 1); // CDC
    reqMap.put("internalState", 0); //InternalStateType.NULL_VAL)

    final String s = objectMapper.writeValueAsString(reqMap);
    log.info("json request {}", s);
    final OrderRequest req = objectMapper.readValue(s, OrderRequest.class);

    webTestClient
        .post()
        .uri("/order/process")
        .body(Mono.just(req), OrderRequest.class)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk();

    if (!latch.await(5, TimeUnit.SECONDS)) {
      throw new TimeoutException("count down timeout");
    } else {
      log.info("count down done, check AgencyAeronOrderEventHandler");
    }
  }

}
