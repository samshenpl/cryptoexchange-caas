package com.crypto.exchange.caas.service.order;

import com.crypto.exchange.caas.core.childorder.ChildOrderEvent;
import com.crypto.exchange.oms.common.core.order.ParentOrderResponseCode;
import lombok.Getter;
import lombok.Setter;

@Getter
public class OutboundEvent {

  @Setter
  private OutboundEventType eventType;
  @Setter
  private ParentOrder parentOrder;
  @Setter
  private ParentOrderResponseCode parentOrderResponseCode;
  private final ChildOrderEvent childOrderEvent = new ChildOrderEvent();

  public void reset() {
    eventType = null;
    parentOrder = null;
    parentOrderResponseCode = null;
    childOrderEvent.reset();
  }
}
