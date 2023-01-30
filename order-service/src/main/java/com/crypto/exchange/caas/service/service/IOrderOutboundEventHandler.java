package com.crypto.exchange.caas.service.service;

import com.crypto.exchange.caas.service.order.ParentOrder;
import com.crypto.exchange.oms.common.core.order.ParentOrderResponseCode;

public interface IOrderOutboundEventHandler {

  void onAcceptNewOrder(ParentOrder parentOrder);

  void onAcceptCancelOrder(ParentOrder parentOrder);

  void onRejectNewOrder(ParentOrder parentOrder, ParentOrderResponseCode parentOrderResponseCode);

  void onRejectCancelOrder(ParentOrder parentOrder, ParentOrderResponseCode parentOrderResponseCode);

  void onUnsolicitedCancelOrder(ParentOrder parentOrder, ParentOrderResponseCode parentOrderResponseCode);

  void onFilledOrder(ParentOrder parentOrder);

  void onPausedOrder(ParentOrder parentOrder);

  void onResumedOrder(ParentOrder parentOrder);
}
