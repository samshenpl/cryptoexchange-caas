package com.crypto.exchange.caas.service.service.adapter.order;

import static org.apache.logging.log4j.util.Unbox.box;

import com.crypto.exchange.caas.core.childorder.ChildOrder;
import com.crypto.exchange.caas.service.order.ParentOrder;
import com.crypto.exchange.caas.service.service.childorder.IChildOrderInboundEventHandler;
import com.crypto.exchange.caas.service.service.IInboundEventHandler;
import com.crypto.exchange.caas.service.service.IOutboundEventRouter;
import com.crypto.exchange.oms.common.core.order.child.ICancelOrderAck;
import com.crypto.exchange.oms.common.core.order.child.ICancelOrderReject;
import com.crypto.exchange.oms.common.core.order.child.IChildReport;
import com.crypto.exchange.oms.common.core.order.child.IFullyFilled;
import com.crypto.exchange.oms.common.core.order.child.INewOrderAck;
import com.crypto.exchange.oms.common.core.order.child.INewOrderReject;
import com.crypto.exchange.oms.common.core.order.child.IPartiallyFilled;
import com.crypto.exchange.oms.common.core.order.child.IUnsolicitedCancel;
import java.util.function.Consumer;
import lombok.extern.log4j.Log4j2;
import org.agrona.collections.Long2ObjectHashMap;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class ChildOrderClient implements IChildOrderClient, IChildOrderInboundEventHandler {

  private final Long2ObjectHashMap<Consumer<IChildReport>> childReportSubscribers = new Long2ObjectHashMap<>();
  private final IOutboundEventRouter outboundEventRouter;

  public ChildOrderClient(IOutboundEventRouter outboundEventRouter,
                          IInboundEventHandler inboundEventHandler) {
    this.outboundEventRouter = outboundEventRouter;
    inboundEventHandler.setChildOrderInboundEventHandler(this);
  }

  @Override
  public void sendChild(ParentOrder parentOrder, ChildOrder childOrder) {
    outboundEventRouter.routeNewChildOrder(parentOrder, childOrder);
  }

  @Override
  public void cancelChild(ParentOrder parentOrder, ChildOrder childOrder) {
    outboundEventRouter.routeCancelChildOrder(parentOrder, childOrder);
  }

  @Override
  public void forceCancelChild(ParentOrder parentOrder, ChildOrder childOrder) {
    outboundEventRouter.routeForceCancelChildOrder(parentOrder, childOrder);
  }

  @Override
  public void subscribeChildReport(ParentOrder parentOrder, Consumer<IChildReport> handler) {
    var orderId = parentOrder.getOrderId();
    if (childReportSubscribers.containsKey(orderId)) {
      throw new IllegalStateException("Duplicated child report subscription for parent order: " + orderId);
    }
    childReportSubscribers.put(orderId, handler);
  }

  @Override
  public void unsubscribeChildReport(ParentOrder parentOrder, Consumer<IChildReport> handler) {
    var orderId = parentOrder.getOrderId();
    if (!childReportSubscribers.containsKey(orderId)) {
      throw new IllegalStateException("Unexpected child report unsubscription for parent order: " + orderId);
    }
    childReportSubscribers.remove(orderId);
  }

  @Override
  public void onNewChildOrderAck(long parentOrderId, INewOrderAck newOrderAck) {
    onChildReport(parentOrderId, newOrderAck);
  }

  @Override
  public void onNewChildOrderReject(long parentOrderId, INewOrderReject newOrderReject) {
    onChildReport(parentOrderId, newOrderReject);
  }

  @Override
  public void onCancelChildOrderAck(long parentOrderId, ICancelOrderAck cancelOrderAck) {
    onChildReport(parentOrderId, cancelOrderAck);
  }

  @Override
  public void onCancelChildOrderReject(long parentOrderId, ICancelOrderReject cancelOrderReject) {
    onChildReport(parentOrderId, cancelOrderReject);
  }

  @Override
  public void onChildOrderFullyFilled(long parentOrderId, IFullyFilled fullyFilled) {
    onChildReport(parentOrderId, fullyFilled);
  }

  @Override
  public void onChildOrderPartiallyFilled(long parentOrderId, IPartiallyFilled partiallyFilled) {
    onChildReport(parentOrderId, partiallyFilled);
  }

  @Override
  public void onChildOrderUnsolicitedCancel(long parentOrderId, IUnsolicitedCancel unsolicitedCancel) {
    onChildReport(parentOrderId, unsolicitedCancel);
  }

  private void onChildReport(long parentOrderId, IChildReport childReport) {
    if (childReportSubscribers.containsKey(parentOrderId)) {
      childReportSubscribers.get(parentOrderId).accept(childReport);
    } else {
      log.warn("Unexpected child report {} for child order: {}. No child report subscription for parent order: {}",
          childReport.getClass().getSimpleName(),
          childReport.getOrderId(),
          box(parentOrderId)
      );
    }
  }
}
