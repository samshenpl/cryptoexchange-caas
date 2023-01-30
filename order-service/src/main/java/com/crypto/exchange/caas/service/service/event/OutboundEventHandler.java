package com.crypto.exchange.caas.service.service.event;

import com.crypto.exchange.caas.service.order.OutboundEvent;
import com.crypto.exchange.caas.service.service.IOrderOutboundEventHandler;
import com.crypto.exchange.caas.service.service.childorder.IChildOrderOutboundEventHandler;
import com.lmax.disruptor.EventHandler;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OutboundEventHandler implements EventHandler<OutboundEvent> {

  @Setter
  @Autowired
  private IOrderOutboundEventHandler parentOrderOutboundEventListener;
  @Setter
  @Autowired
  private IChildOrderOutboundEventHandler childOrderOutboundEventListener;

  @Override
  public void onEvent(OutboundEvent event, long sequence, boolean endOfBatch) {
    switch (event.getEventType()) {
      case NEW_PARENT_ORDER_ACK:
        parentOrderOutboundEventListener.onAcceptNewOrder(event.getParentOrder());
        break;
      case CANCEL_PARENT_ORDER_ACK:
        parentOrderOutboundEventListener.onAcceptCancelOrder(event.getParentOrder());
        break;
      case NEW_PARENT_ORDER_REJECT:
        parentOrderOutboundEventListener.onRejectNewOrder(event.getParentOrder(), event.getParentOrderResponseCode());
        break;
      case CANCEL_PARENT_ORDER_REJECT:
        parentOrderOutboundEventListener.onRejectCancelOrder(event.getParentOrder(), event.getParentOrderResponseCode());
        break;
      case UNSOLICITED_CANCEL_PARENT_ORDER:
        parentOrderOutboundEventListener.onUnsolicitedCancelOrder(event.getParentOrder(), event.getParentOrderResponseCode());
        break;
      case PARENT_ORDER_FILLED:
        parentOrderOutboundEventListener.onFilledOrder(event.getParentOrder());
        break;
      case PARENT_ORDER_PAUSED:
        parentOrderOutboundEventListener.onPausedOrder(event.getParentOrder());
        break;
      case PARENT_ORDER_RESUMED:
        parentOrderOutboundEventListener.onResumedOrder(event.getParentOrder());
        break;
      case NEW_CHILD_ORDER:
        childOrderOutboundEventListener.onNewChildOrder(event.getParentOrder(), event.getChildOrderEvent());
        break;
      case CANCEL_CHILD_ORDER:
        childOrderOutboundEventListener.onCancelChildOrder(event.getParentOrder(), event.getChildOrderEvent());
        break;
      case FORCE_CANCEL_CHILD_ORDER:
        childOrderOutboundEventListener.onForceCancelChildOrder(event.getParentOrder(), event.getChildOrderEvent());
        break;
    }
  }
}
