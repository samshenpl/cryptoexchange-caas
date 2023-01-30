package com.crypto.exchange.caas.service.service;

import com.crypto.exchange.caas.core.childorder.ChildOrder;
import com.crypto.exchange.caas.service.order.ParentOrder;
import com.crypto.exchange.oms.common.core.order.ParentOrderResponseCode;

public interface IOutboundEventRouter {

  void start();

  void routeNewParentOrderAck(ParentOrder parentOrder);

  void routeCancelParentOrderAck(ParentOrder parentOrder);

  void routeNewParentOrderReject(ParentOrder parentOrder, ParentOrderResponseCode parentOrderResponseCode);

  void routeCancelParentOrderReject(ParentOrder parentOrder, ParentOrderResponseCode parentOrderResponseCode);

  void routeUnsolicitedCancelParentOrder(ParentOrder parentOrder, ParentOrderResponseCode parentOrderResponseCode);

  void routeFilledParentOrder(ParentOrder parentOrder);

  void routePausedParentOrder(ParentOrder parentOrder);

  void routeResumedParentOrder(ParentOrder parentOrder);

  void routeNewChildOrder(ParentOrder parentOrder, ChildOrder childOrder);

  void routeCancelChildOrder(ParentOrder parentOrder, ChildOrder childOrder);

  void routeForceCancelChildOrder(ParentOrder parentOrder, ChildOrder childOrder);

}
