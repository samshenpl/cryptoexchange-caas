package com.crypto.exchange.caas.service.service.childorder;

import com.crypto.commons.datamodel.oms.v1.TimeInForceType;
import com.crypto.exchange.caas.core.childorder.ChildOrder;
import com.crypto.exchange.caas.service.order.EventHandlerContext;
import com.crypto.exchange.enums.Destination;

public interface IChildOrderService {

  void createChild(EventHandlerContext context, long price, long quantity, TimeInForceType timeInForce, Destination executionVenue);

  void cancelChild(EventHandlerContext context, ChildOrder childOrder);

  void forceCancelAllChild(EventHandlerContext context);
}
