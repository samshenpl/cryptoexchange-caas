package com.crypto.exchange.caas.service.service.childorder;

import com.crypto.exchange.caas.core.childorder.ChildOrder;
import com.crypto.exchange.caas.service.order.EventHandlerContext;
import com.crypto.exchange.oms.common.core.order.child.IChildReport;

public interface IChildOrderEventHandler {

  ChildOrder onChildReport(EventHandlerContext context, IChildReport childReport);

  void onChildOrderRecovery(EventHandlerContext context, ChildOrder childOrder);
}
