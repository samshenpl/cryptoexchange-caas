package com.crypto.exchange.caas.service.service.adapter.aeron;

import static org.springframework.util.ObjectUtils.isEmpty;

import com.crypto.exchange.caas.service.order.EventHandlerContext;
import com.crypto.exchange.caas.service.order.ParentOrder;
import com.crypto.exchange.caas.service.service.adapter.order.IChildOrderClient;
import com.crypto.exchange.caas.service.service.childorder.IChildOrderEventHandler;
import com.crypto.exchange.caas.service.service.childorder.IChildOrderService;
import com.crypto.exchange.oms.common.aeron.IAeronInboundEventHandler;
import com.crypto.exchange.oms.common.aeron.model.AeronOrder;
import com.crypto.exchange.oms.common.aeron.response.IParentOrderResponseHandler;
import com.crypto.exchange.oms.common.core.order.ParentOrderResponseCode;
import com.crypto.exchange.oms.common.core.order.child.ICancelOrderAck;
import com.crypto.exchange.oms.common.core.order.child.ICancelOrderReject;
import com.crypto.exchange.oms.common.core.order.child.IFullyFilled;
import com.crypto.exchange.oms.common.core.order.child.INewOrderAck;
import com.crypto.exchange.oms.common.core.order.child.INewOrderReject;
import com.crypto.exchange.oms.common.core.order.child.IPartiallyFilled;
import com.crypto.exchange.oms.common.core.order.child.IUnsolicitedCancel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AeronInboundEventHandler implements IAeronInboundEventHandler {

  Map<Long, EventHandlerContext> contexts = new ConcurrentHashMap<>();

  @Setter
  IChildOrderService childOrderService;

  @Setter
  IChildOrderEventHandler childOrderEventHandler;

  @Setter
  IChildOrderClient childOrderClient;

  @Autowired
  @Setter
  IParentOrderResponseHandler parentOrderResponseHandler;
  @Override
  public void handleNewParentOrder(AeronOrder aeronOrder) {
    log.info("handle new parent order {}", aeronOrder);
    if (isEmpty(aeronOrder.getEndUserTag())) {
      log.info("empty end user tag account {}, symbol {}, side {}, price {}", aeronOrder.getAccount(), aeronOrder.getSymbol(),
          aeronOrder.getOrderSideType(), aeronOrder.getPrice());
    }
    aeronOrder.setCreateTime(System.currentTimeMillis());
    final long orderId = aeronOrder.getOrderId();
    ParentOrder parentOrder = ParentOrder.valueOf(aeronOrder);
    log.info("handleNewParentOrder orderId {}", orderId);
    final EventHandlerContext context = createContext(orderId, parentOrder);
    childOrderClient.subscribeChildReport(parentOrder, childReport -> {
      childOrderEventHandler.onChildReport(context, childReport);
    });
    long quantity = aeronOrder.getQuantity();
    long price = aeronOrder.getPrice();
    childOrderService.createChild(context, price, quantity, aeronOrder.getTimeInForceType(), aeronOrder.getDestination());
  }

  @Override
  public void handleCancelParentOrder(AeronOrder aeronOrder) {
    childOrderService.forceCancelAllChild(contexts.get(aeronOrder.getOrderId()));
  }

  @Override
  public void handleNewChildOrderAck(long parentOrderId, INewOrderAck newOrderAck) {
    log.info("handle new child order ack {} {}", parentOrderId, newOrderAck);

  }

  @Override
  public void handleNewChildOrderReject(long parentOrderId, INewOrderReject newOrderReject) {
    log.info("handle new child order reject {} {}", parentOrderId, newOrderReject != null ? newOrderReject.getChildOrderResponseCode() : null);

    ParentOrderResponseCode responseCode;


    switch (newOrderReject.getChildOrderResponseCode()) {
      case SYMBOL_NOT_FOUND:
      case DERIV_INVALID_INSTRUMENT:
        responseCode = ParentOrderResponseCode.INVALID_SYMBOL;
        break;
      case SIDE_NOT_SUPPORTED:
        responseCode = ParentOrderResponseCode.INVALID_SIDE;
        break;
      case MIN_PRICE_VIOLATED:
      case MAX_PRICE_VIOLATED:
      case INVALID_PRICE_PRECISION:
        responseCode = ParentOrderResponseCode.INVALID_PRICE;
        break;
      case MIN_QUANTITY_VIOLATED:
      case MAX_QUANTITY_VIOLATED:
      case INVALID_QUANTITY_PRECISION:
        responseCode = ParentOrderResponseCode.INVALID_QUANTITY;
        break;
      case BK_UNAVAILABLE:
      case ROUTE_ORDER_ERROR:
        responseCode = ParentOrderResponseCode.INTERNAL_ERROR;
        break;
      case NEGATIVE_BALANCE:
        responseCode = ParentOrderResponseCode.STOP_ORDER_DUE_TO_INSUFFICIENT_QUANTITY;
        break;

      case UNKNOWN:
      case ORDERTYPE_NOT_SUPPORTED:
      case USER_TIER_INVALID:
      case ACCOUNT_NOT_TRADABLE:
      case ACCOUNT_NOT_FOUND:
      case USER_NOT_FOUND:
      case DERIV_ACCOUNT_IS_NOT_ACTIVE:
      case DERIV_ACCOUNT_IS_SUSPENDED:
      case DERIV_INVALID_USER:
      case DERIV_USER_NO_DERIV_ACCESS:
      case DERIV_ACCOUNT_NO_DERIV_ACCESS:
      case ACCOUNT_TRADING_SUSPENDED:
      case ACCOUNT_INSTRUCTION_BANNED_DUE_TO_USER_REGION:
      case MIN_NOTIONAL_VIOLATED:
      case MAX_NOTIONAL_VIOLATED:
      case MIN_AMOUNT_VIOLATED:
      case MAX_AMOUNT_VIOLATED:
      case AMOUNT_PRECISION_OVERFLOW:
      case DERIV_INVALID_AMOUNT:
      case DERIV_MARGIN_UNIT_IS_SUSPENDED:
      case DERIV_EXCEEDS_MAX_AVAILABLE_BALANCE:
      case DERIV_TRANSFER_INVALID_CURRENCY:
      case OVER_DAILY_LIMIT:
      case MARKET_ORDER_ONLY_QUANTITY_OR_NOTIONAL:
      case DERIV_NEGATIVE_BALANCE:
      default:
        responseCode = ParentOrderResponseCode.STOP_ORDER_DUE_TO_DOWNSTREAM_REJECT;

    }
    parentOrderResponseHandler.handleNewReject(parentOrderId, responseCode);
  }

  @Override
  public void handleCancelChildOrderAck(long parentOrderId, ICancelOrderAck cancelOrderAck) {

  }

  @Override
  public void handleCancelChildOrderReject(long parentOrderId, ICancelOrderReject cancelOrderReject) {

  }

  @Override
  public void handleChildOrderFullyFilled(long parentOrderId, IFullyFilled fullyFilled) {

  }

  @Override
  public void handleChildOrderPartiallyFilled(long parentOrderId, IPartiallyFilled partiallyFilled) {

  }

  @Override
  public void handleChildOrderUnsolicitedCancel(long parentOrderId, IUnsolicitedCancel unsolicitedCancel) {
    parentOrderResponseHandler.handleUnsolicitedCancel(parentOrderId, ParentOrderResponseCode.STOP_ORDER_DUE_TO_DOWNSTREAM_REJECT);
  }

  @Override
  public void handleParentOrderRecovery(AeronOrder aeronOrder) {
    log.info("handle new parent order recovery {}", aeronOrder.getOrderId());
    ParentOrder parentOrder = ParentOrder.valueOf(aeronOrder);
    createContext(aeronOrder.getOrderId(), parentOrder);
  }

  private EventHandlerContext createContext(long orderId, ParentOrder parentOrder) {
    final EventHandlerContext context = new EventHandlerContext(parentOrder);
    contexts.put(orderId, context);
    return context;
  }

  @Override
  public void handleChildOrderRecovery(AeronOrder childOrder) {

  }

  @Override
  public void handleRecoveryEnd() {
    for (EventHandlerContext context : contexts.values()) {
      childOrderClient.subscribeChildReport(context.getOrder(), childReport -> {
        childOrderEventHandler.onChildReport(context, childReport);
      });
    }
  }


}
