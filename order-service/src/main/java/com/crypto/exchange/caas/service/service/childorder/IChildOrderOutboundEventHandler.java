package com.crypto.exchange.caas.service.service.childorder;

import com.crypto.exchange.caas.core.childorder.ChildOrderEvent;
import com.crypto.exchange.caas.service.order.ParentOrder;

public interface IChildOrderOutboundEventHandler {

  void onNewChildOrder(ParentOrder parentOrder, ChildOrderEvent childOrderEvent);

  void onCancelChildOrder(ParentOrder parentOrder, ChildOrderEvent childOrderEvent);

  void onForceCancelChildOrder(ParentOrder parentOrder, ChildOrderEvent childOrderEvent);
}
