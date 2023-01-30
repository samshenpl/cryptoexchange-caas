package com.crypto.exchange.caas.service.service;

import com.crypto.exchange.caas.service.order.ParentOrder;

public interface IOrderInboundEventHandler {

  void onNewParentOrder(ParentOrder parentOrder);

  void onCancelParentOrder(long orderId);//, CancelType cancelType);

  void onPauseParentOrder(long orderId);

  void onResumeParentOrder(long orderId);
}
