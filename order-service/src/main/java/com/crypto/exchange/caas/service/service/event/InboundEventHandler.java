package com.crypto.exchange.caas.service.service.event;

import static com.google.common.base.Preconditions.checkArgument;

import com.crypto.exchange.caas.service.order.InboundEvent;
import com.crypto.exchange.caas.service.service.childorder.IChildOrderInboundEventHandler;
import com.crypto.exchange.caas.service.service.IInboundEventHandler;
import com.crypto.exchange.caas.service.service.IOrderInboundEventHandler;
import com.google.common.base.Preconditions;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class InboundEventHandler implements IInboundEventHandler {

  private final Consumer<InboundEvent> metricsReporter;

  private IOrderInboundEventHandler parentOrderInboundEventHandler;
  private IChildOrderInboundEventHandler childOrderInboundEventHandler;
//  private IRecoveryEventHandler recoveryEventHandler;

  public InboundEventHandler(Consumer<InboundEvent> metricsReporter) {
    this.metricsReporter = metricsReporter;
  }

  @Override
  public void setParentOrderInboundEventHandler(IOrderInboundEventHandler parentOrderInboundEventHandler) {
    Preconditions.checkArgument(this.parentOrderInboundEventHandler == null, "There should only be one parentOrderInboundEventHandler");
    this.parentOrderInboundEventHandler = parentOrderInboundEventHandler;
  }

  @Override
  public void setChildOrderInboundEventHandler(IChildOrderInboundEventHandler childOrderInboundEventHandler) {
    Preconditions.checkArgument(this.childOrderInboundEventHandler == null, "There should only be one childOrderInboundEventHandler");
    this.childOrderInboundEventHandler = childOrderInboundEventHandler;
  }

  public void onEvent(InboundEvent event, long sequence, boolean endOfBatch) {
    metricsReporter.accept(event);

    switch (event.getEventType()) {
      case BOOK:
      case BOOK_FEED_STATUS:
      case TRADE:
      case TRADE_FEED_STATUS:
      case STATISTICS:
      case STATISTICS_FEED_STATUS:
        break;
      case NEW_PARENT_ORDER:
        parentOrderInboundEventHandler.onNewParentOrder(event.getParentOrder());
        break;
      case CANCEL_PARENT_ORDER:
        parentOrderInboundEventHandler.onCancelParentOrder(event.getOrderId());//, event.getCancelType());
        break;
      case PAUSE_PARENT_ORDER:
        parentOrderInboundEventHandler.onPauseParentOrder(event.getOrderId());
        break;
      case RESUME_PARENT_ORDER:
        parentOrderInboundEventHandler.onResumeParentOrder(event.getOrderId());
        break;
      case NEW_CHILD_ORDER_ACK:
        childOrderInboundEventHandler.onNewChildOrderAck(event.getParentOrderId(), event.getNewOrderAck());
        break;
      case NEW_CHILD_ORDER_REJECT:
        childOrderInboundEventHandler.onNewChildOrderReject(event.getParentOrderId(), event.getNewOrderReject());
        break;
      case CANCEL_CHILD_ORDER_ACK:
        childOrderInboundEventHandler.onCancelChildOrderAck(event.getParentOrderId(), event.getCancelOrderAck());
        break;
      case CANCEL_CHILD_ORDER_REJECT:
        childOrderInboundEventHandler.onCancelChildOrderReject(event.getParentOrderId(), event.getCancelOrderReject());
        break;
      case CHILD_ORDER_FULLY_FILLED:
        childOrderInboundEventHandler.onChildOrderFullyFilled(event.getParentOrderId(), event.getFullyFilled());
        break;
      case CHILD_ORDER_PARTIALLY_FILLED:
        childOrderInboundEventHandler.onChildOrderPartiallyFilled(event.getParentOrderId(), event.getPartiallyFilled());
        break;
      case CHILD_ORDER_UNSOLICITED_CANCEL:
        childOrderInboundEventHandler.onChildOrderUnsolicitedCancel(event.getParentOrderId(), event.getUnsolicitedCancel());
        break;
      case TIMER:
      case PARENT_ORDER_RECOVERY:
      case CHILD_ORDER_RECOVERY:
      case RECOVERY_END:
        break;
    }
  }
}
