package com.crypto.exchange.caas.service.service.adapter.order;

import com.crypto.exchange.caas.core.childorder.ChildOrder;
import com.crypto.exchange.caas.service.order.ParentOrder;
import com.crypto.exchange.oms.common.core.order.child.IChildReport;
import java.util.function.Consumer;

public interface IChildOrderClient {

  void sendChild(ParentOrder parentOrder, ChildOrder childOrder);

  void cancelChild(ParentOrder parentOrder, ChildOrder childOrder);

  void forceCancelChild(ParentOrder parentOrder, ChildOrder childOrder);

  void subscribeChildReport(ParentOrder parentOrder, Consumer<IChildReport> handler);

  void unsubscribeChildReport(ParentOrder parentOrder, Consumer<IChildReport> handler);
}
