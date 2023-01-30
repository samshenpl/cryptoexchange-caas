package com.crypto.exchange.agency.tests;

import com.crypto.commons.aeron.AeronConfig;
import com.crypto.commons.aeron.channel.ChannelConfigs;
import com.crypto.commons.aeron.processor.ChannelListener;
import com.crypto.exchange.caas.server.CaasServerAppConfig;
import com.crypto.exchange.matchingengine.model.oms.v1.MessageHeaderEncoder;
import com.crypto.exchange.matchingengine.model.oms.v1.OrderEncoder;
import com.crypto.exchange.matchingengine.model.oms.v1.OrderExtEncoder;
import com.crypto.exchange.matchingengine.model.oms.v1.OrderSideType;
import com.crypto.exchange.matchingengine.model.oms.v1.RequestType;
import com.crypto.exchange.matchingengine.model.oms.v1.SingleOrderRequestEncoder;
import com.crypto.exchange.matchingengine.model.oms.v1.TimeInForceType;
import com.crypto.exchange.oms.common.aeron.channel.AeronChannel;
import com.crypto.exchange.oms.common.aeron.channel.ChannelType;
import com.crypto.exchange.oms.common.aeron.model.AeronOrder;
import com.crypto.exchange.oms.common.aeron.util.AeronOrderStore;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

@Slf4j
public class ServerTest {
  public static class YamlPropertySourceFactory implements PropertySourceFactory {

    @Override
    public org.springframework.core.env.PropertySource<?> createPropertySource(String name, EncodedResource encodedResource) {
      YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
      factory.setResources(encodedResource.getResource());
      Properties properties = factory.getObject();
      return new PropertiesPropertySource(encodedResource.getResource().getFilename(), properties);
    }
  }

  private static AeronChannel newClientRequestChannel() {
    final org.springframework.core.env.PropertySource<?> source = new YamlPropertySourceFactory()
        .createPropertySource("application.yaml",
            new EncodedResource(new ClassPathResource("/application.yaml")));

    String name = "test";
    ChannelType channelType = ChannelType.SOR_CHILD_REQUEST_PUBLISHER;
    int channelId = (Integer) source.getProperty("cryptoexchange.oms-common.channels.parent-order-request-channel.channel-id");
    ChannelConfigs configs = new ChannelConfigs() {
      @Override
      public Optional<AeronConfig> get(String s) {
        final AeronConfig config = new AeronConfig();
        config.setAddress((String) source.getProperty("cryptoexchange.oms-common.channels.parent-order-request-channel.aeron-config.address"));
        config.setPort((Integer) source.getProperty("cryptoexchange.oms-common.channels.parent-order-request-channel.aeron-config.port"));
        config.setStreamId((Integer) source.getProperty("cryptoexchange.oms-common.channels.parent-order-request-channel.aeron-config.stream-id"));
        config.setDir("tmp/agencytest");
        config.setThreadingMode("SHARED");
        return Optional.of(config);
      }
    };
    ChannelListener channelListener = null;
    Supplier<Long> replayPositionSupplier = () -> 0L;
    final AeronChannel aeronChannel = new AeronChannel(name,
        channelType,
        channelId,
        configs,
        channelListener,
        replayPositionSupplier);
    aeronChannel.init();
    return aeronChannel;
  }

  private static AeronChannel createPtListener(CountDownLatch latch) {
    final org.springframework.core.env.PropertySource<?> source = new YamlPropertySourceFactory()
        .createPropertySource("application.yaml",
            new EncodedResource(new ClassPathResource("/application.yaml")));

    String name = "test";
    final String channelName = "post-trade-response-channel";
    ChannelType channelType = ChannelType.OMS_CHILD_RESPONSE_SUBSCRIBER;
    int channelId = (Integer) source.getProperty("cryptoexchange.oms-common.channels."  + channelName + ".channel-id");
    ChannelConfigs configs = new ChannelConfigs() {
      @Override
      public Optional<AeronConfig> get(String s) {
        final AeronConfig config = new AeronConfig();
        config.setAddress((String) source.getProperty("cryptoexchange.oms-common.channels." + channelName + ".aeron-config.address"));
        config.setPort((Integer) source.getProperty("cryptoexchange.oms-common.channels." + channelName + ".aeron-config.port"));
        config.setStreamId((Integer) source.getProperty("cryptoexchange.oms-common.channels." + channelName + ".aeron-config.stream-id"));
        config.setDir("tmp/agencytest2");
        config.setThreadingMode("SHARED");
        return Optional.of(config);
      }
    };
    ChannelListener channelListener = (buffer, l) -> {
      log.info("aeronpost-trade-response-channel receive parent or child. buffer {} l {}", buffer, l);
      latch.countDown();
    };
    Supplier<Long> replayPositionSupplier = () -> 0L;
    final AeronChannel aeronChannel = new AeronChannel(name,
        channelType,
        channelId,
        configs,
        channelListener,
        replayPositionSupplier);
    aeronChannel.init();
    return aeronChannel;
  }

  private static AeronChannel createDownstream(/*CountDownLatch latch*/) {
    final org.springframework.core.env.PropertySource<?> source = new YamlPropertySourceFactory()
        .createPropertySource("application.yaml",
            new EncodedResource(new ClassPathResource("/application.yaml")));

    String name = "testdownstreamoms";
    // FIXME FIXME oms-child-request-channel
    final String channelName = "oms-child-request-channel";
//    final String channelName = "texo-child-response-channel";
    ChannelType channelType = ChannelType.OMS_CHILD_RESPONSE_SUBSCRIBER; // just any subscriber for
    int channelId = (Integer) source.getProperty("cryptoexchange.oms-common.channels."  + channelName + ".channel-id");
    ChannelConfigs configs = new ChannelConfigs() {
      @Override
      public Optional<AeronConfig> get(String s) {
        final AeronConfig config = new AeronConfig();
        config.setAddress((String) source.getProperty("cryptoexchange.oms-common.channels." + channelName + ".aeron-config.address"));
        config.setPort((Integer) source.getProperty("cryptoexchange.oms-common.channels." + channelName + ".aeron-config.port"));
        config.setStreamId((Integer) source.getProperty("cryptoexchange.oms-common.channels." + channelName + ".aeron-config.stream-id"));
        config.setDir("tmp/agencytest2/downstream-oms");
        config.setThreadingMode("SHARED");
        return Optional.of(config);
      }
    };
    ChannelListener channelListener = (buffer, l) -> {
      log.info("aeron downstream oms-response-channel receive parent or child. buffer {} l {}", buffer, l);
//      latch.countDown();
    };
    Supplier<Long> replayPositionSupplier = () -> 0L;
    final AeronChannel aeronChannel = new AeronChannel(name,
        channelType,
        channelId,
        configs,
        channelListener,
        replayPositionSupplier);
    aeronChannel.init();
    return aeronChannel;
  }

  private static void sendOrder(AeronChannel aeronChannel) {
    final ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 16);
    final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(byteBuffer);
    final SingleOrderRequestEncoder encoder = new SingleOrderRequestEncoder();
    final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    encoder.wrapAndApplyHeader(unsafeBuffer, 0, headerEncoder);
    encoder.wrap(unsafeBuffer, headerEncoder.encodedLength());
    encoder.request().requestType(RequestType.NEW);
    final OrderExtEncoder order = encoder.request().order();
    order.venues("1");
    order.orderParams("0=TWAP;1=" + System.currentTimeMillis() +";allocationid=123123");

    final OrderEncoder orderEncoder = order.order()
        .orderId(364852)
        .account("acc").clientOrderId("codiyryridid").side(OrderSideType.BUY)
        .symbol("BTC_USDC")
        .timeInForce(TimeInForceType.GOOD_TILL_CANCEL);

    orderEncoder.price().value(199).decimals((byte) 1);
    orderEncoder.quantity().value(132).decimals((byte) -1);
    aeronChannel.send(unsafeBuffer, encoder.encodedLength() + order.encodedLength());
  }

  @Slf4j
  @Configuration
  @PropertySource(value = "application.yaml", factory = YamlPropertySourceFactory.class)
  @EnableConfigurationProperties
  @Import(CaasServerAppConfig.class)
  static class TestConfig {
  }

  @Test
  @Disabled
  public void test1() throws InterruptedException {

    final CountDownLatch latch = new CountDownLatch(2);
    createPtListener(latch);
    createDownstream();
    log.info("simpletest-start");
    final AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(TestConfig.class);


    sendOrder(newClientRequestChannel());

    if (!latch.await(14, TimeUnit.SECONDS)) {
      throw new RuntimeException("check failed 14 :()");
    }
    int parentOrderCount = 0;
    Iterator<AeronOrder> iterator = applicationContext.getBean(AeronOrderStore.class).getAllParentOrders().iterator();
    while (iterator.hasNext()) {
      parentOrderCount++;
      iterator.next();
    }
    if (parentOrderCount < 1) {
      throw new RuntimeException("check parentOrderCount failed :()");
    }
    int childOrderCount = 0;
    iterator = applicationContext.getBean(AeronOrderStore.class).getAllParentOrders().iterator();
    while (iterator.hasNext()) {
      childOrderCount++;
      iterator.next();
    }
    if (childOrderCount < 1) {
      throw new RuntimeException("check childOrderCount: " + childOrderCount);
    }

  }
}
