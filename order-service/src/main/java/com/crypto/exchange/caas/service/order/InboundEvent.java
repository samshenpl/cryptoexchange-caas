package com.crypto.exchange.caas.service.order;

import com.crypto.exchange.oms.common.core.order.child.CancelOrderAck;
import com.crypto.exchange.oms.common.core.order.child.CancelOrderReject;
import com.crypto.exchange.oms.common.core.order.child.FullyFilled;
import com.crypto.exchange.oms.common.core.order.child.NewOrderAck;
import com.crypto.exchange.oms.common.core.order.child.NewOrderReject;
import com.crypto.exchange.oms.common.core.order.child.PartiallyFilled;
import com.crypto.exchange.oms.common.core.order.child.UnsolicitedCancel;
import com.crypto.exchange.oms.common.core.util.NumberUtils;
import javax.print.attribute.standard.Destination;
import lombok.Getter;
import lombok.Setter;

@Getter
public class InboundEvent {

  @Setter
  private InboundEventType eventType;
  @Setter
  private Destination venue;
  @Setter
  private String symbol;
  @Setter
  private ParentOrder parentOrder;
  @Setter
  private long parentOrderId;
  @Setter
  private long orderId;
  @Setter
  private long timerExpiryMillis;

  /**
   * Monotonic time from clock implementation when the event is enqueued
   * Used for queue time tracking only
   * Do not interpret this as wall clock time
   */
  @Setter
  private long enqueueTimestampNanos;
  private final NewOrderAck newOrderAck = new NewOrderAck();
  private final NewOrderReject newOrderReject = new NewOrderReject();
  private final CancelOrderAck cancelOrderAck = new CancelOrderAck();
  private final CancelOrderReject cancelOrderReject = new CancelOrderReject();
  private final FullyFilled fullyFilled = new FullyFilled();
  private final PartiallyFilled partiallyFilled = new PartiallyFilled();
  private final UnsolicitedCancel unsolicitedCancel = new UnsolicitedCancel();

  public void reset() {
    eventType = null;
    symbol = null;
    parentOrder = null;
    parentOrderId = NumberUtils.EMPTY_VALUE;
    orderId = NumberUtils.EMPTY_VALUE;
    timerExpiryMillis = 0L;
    enqueueTimestampNanos = 0L;
    newOrderAck.reset();
    newOrderReject.reset();
    cancelOrderAck.reset();
    cancelOrderReject.reset();
    fullyFilled.reset();
    partiallyFilled.reset();
    unsolicitedCancel.reset();
  }
}
