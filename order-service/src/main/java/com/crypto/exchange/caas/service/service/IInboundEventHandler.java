package com.crypto.exchange.caas.service.service;

import com.crypto.exchange.caas.service.order.InboundEvent;
import com.crypto.exchange.caas.service.service.childorder.IChildOrderInboundEventHandler;
import com.lmax.disruptor.EventHandler;

public interface IInboundEventHandler extends EventHandler<InboundEvent> {

//  void setMarketDataEventHandler(IMarketDataEventHandler marketDataEventHandler);

  void setParentOrderInboundEventHandler(IOrderInboundEventHandler parentOrderInboundEventHandler);

  void setChildOrderInboundEventHandler(IChildOrderInboundEventHandler childOrderInboundEventHandler);

//  void setTimerEventHandler(ITimerEventHandler timerEventHandler);

//  void setRecoveryEventHandler(IRecoveryEventHandler recoveryEventHandler);
}
