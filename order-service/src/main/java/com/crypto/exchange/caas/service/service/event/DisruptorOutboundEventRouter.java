package com.crypto.exchange.caas.service.service.event;

import com.crypto.exchange.caas.core.childorder.ChildOrder;
import com.crypto.exchange.caas.service.order.ParentOrder;
import com.crypto.exchange.caas.service.order.OutboundEvent;
import com.crypto.exchange.caas.service.order.OutboundEventType;
import com.crypto.exchange.caas.service.service.IOutboundEventRouter;
import com.crypto.exchange.oms.common.core.event.LogErrorExceptionHandler;
import com.crypto.exchange.oms.common.core.order.ParentOrderResponseCode;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DisruptorOutboundEventRouter implements IOutboundEventRouter {

  private final Disruptor<OutboundEvent> disruptor;
  private final RingBuffer<OutboundEvent> ringBuffer;

  public DisruptorOutboundEventRouter(EventHandler<OutboundEvent> eventHandler,
                                      @Value("${inbound.disruptor.bufferSize:1024}") int bufferSize) {
    disruptor = new Disruptor<>(
        OutboundEvent::new,
        bufferSize,
        DaemonThreadFactory.INSTANCE,
        ProducerType.MULTI,
        new BlockingWaitStrategy()
    );
    disruptor.setDefaultExceptionHandler(new LogErrorExceptionHandler<>());
    disruptor.handleEventsWith(eventHandler);
    ringBuffer = disruptor.getRingBuffer();
  }

  @Override
  public void start() {
    disruptor.start();
  }

  @Override
  public void routeNewParentOrderAck(ParentOrder parentOrder) {
    publish(OutboundEventType.NEW_PARENT_ORDER_ACK, parentOrder, null, null);
  }

  @Override
  public void routeCancelParentOrderAck(ParentOrder parentOrder) {
    publish(OutboundEventType.CANCEL_PARENT_ORDER_ACK, parentOrder, null, null);
  }

  @Override
  public void routeNewParentOrderReject(ParentOrder parentOrder, ParentOrderResponseCode parentOrderResponseCode) {
    publish(OutboundEventType.NEW_PARENT_ORDER_REJECT, parentOrder, null, parentOrderResponseCode);
  }

  @Override
  public void routeCancelParentOrderReject(ParentOrder parentOrder, ParentOrderResponseCode parentOrderResponseCode) {
    publish(OutboundEventType.CANCEL_PARENT_ORDER_REJECT, parentOrder, null, parentOrderResponseCode);
  }

  @Override
  public void routeUnsolicitedCancelParentOrder(ParentOrder parentOrder, ParentOrderResponseCode parentOrderResponseCode) {
    publish(OutboundEventType.UNSOLICITED_CANCEL_PARENT_ORDER, parentOrder, null, parentOrderResponseCode);
  }

  @Override
  public void routeFilledParentOrder(ParentOrder parentOrder) {
    publish(OutboundEventType.PARENT_ORDER_FILLED, parentOrder, null, null);
  }

  @Override
  public void routePausedParentOrder(ParentOrder parentOrder) {
    publish(OutboundEventType.PARENT_ORDER_PAUSED, parentOrder, null, null);
  }

  @Override
  public void routeResumedParentOrder(ParentOrder parentOrder) {
    publish(OutboundEventType.PARENT_ORDER_RESUMED, parentOrder, null, null);
  }

  @Override
  public void routeNewChildOrder(ParentOrder parentOrder, ChildOrder childOrder) {
    publish(OutboundEventType.NEW_CHILD_ORDER, parentOrder, childOrder, null);
  }

  @Override
  public void routeCancelChildOrder(ParentOrder parentOrder, ChildOrder childOrder) {
    publish(OutboundEventType.CANCEL_CHILD_ORDER, parentOrder, childOrder, null);
  }

  @Override
  public void routeForceCancelChildOrder(ParentOrder parentOrder, ChildOrder childOrder) {
    publish(OutboundEventType.FORCE_CANCEL_CHILD_ORDER, parentOrder, childOrder, null);
  }

  private void publish(OutboundEventType type, ParentOrder parentOrder, ChildOrder childOrder, ParentOrderResponseCode parentOrderResponseCode) {
    ringBuffer.publishEvent((event, sequence, pParentOrder, pChildOrder) -> {
      event.reset();
      event.setEventType(type);
      event.setParentOrder(pParentOrder);
      event.setParentOrderResponseCode(parentOrderResponseCode);
      if (pChildOrder != null) {
        event.getChildOrderEvent().copyFrom(pChildOrder);
      }
    }, parentOrder, childOrder);

  }

}
