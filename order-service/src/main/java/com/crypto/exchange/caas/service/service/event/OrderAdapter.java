package com.crypto.exchange.caas.service.service.event;

import com.crypto.commons.datamodel.oms.v1.ExecTypeType;
import com.crypto.commons.datamodel.oms.v1.OrderLevelType;
import com.crypto.commons.datamodel.oms.v1.OrderStatusType;
import com.crypto.commons.datamodel.oms.v1.OrderType;
import com.crypto.commons.utils.IdUtil;
import com.crypto.exchange.caas.core.childorder.ChildOrderEvent;
import com.crypto.exchange.caas.service.order.ParentOrder;
import com.crypto.exchange.caas.service.service.IOrderOutboundEventHandler;
import com.crypto.exchange.caas.service.service.childorder.IChildOrderOutboundEventHandler;

import com.crypto.exchange.enums.RequestSource;
import com.crypto.exchange.enums.SystemDomain;
import com.crypto.exchange.oms.common.aeron.IOmsAeronService;
import com.crypto.exchange.oms.common.aeron.model.AeronOrder;
import com.crypto.exchange.oms.common.core.order.ParentOrderResponseCode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class OrderAdapter implements IChildOrderOutboundEventHandler, IOrderOutboundEventHandler {

  private final ThreadLocal<AeronOrder> reusableAeronOrder = ThreadLocal.withInitial(AeronOrder::new);
  @Setter
  @Autowired
  private IOmsAeronService omsAeronService;

  @Getter
  private final Map<Long, ParentOrder> parentOrderCache;

  public OrderAdapter() {
    this.parentOrderCache = new ConcurrentHashMap<>();
  }

  public void start() {
    omsAeronService.start();
  }

  @Override
  public void onAcceptNewOrder(ParentOrder parentOrder) {
    omsAeronService.acceptNewParentOrder(parentOrder.getOrderId());
  }

  @Override
  public void onAcceptCancelOrder(ParentOrder parentOrder) {
    parentOrderCache.remove(parentOrder.getOrderId());
    omsAeronService.acceptCancelParentOrder(parentOrder.getOrderId());
  }

  @Override
  public void onRejectNewOrder(ParentOrder parentOrder, ParentOrderResponseCode parentOrderResponseCode) {
    parentOrderCache.remove(parentOrder.getOrderId());
    omsAeronService.rejectNewParentOrder(parentOrder.getOrderId(), parentOrderResponseCode);
  }

  @Override
  public void onRejectCancelOrder(ParentOrder parentOrder, ParentOrderResponseCode parentOrderResponseCode) {
    omsAeronService.rejectCancelParentOrder(parentOrder.getOrderId(), parentOrderResponseCode);
  }

  @Override
  public void onUnsolicitedCancelOrder(ParentOrder parentOrder, ParentOrderResponseCode parentOrderResponseCode) {
    parentOrderCache.remove(parentOrder.getOrderId());
    if (parentOrderResponseCode == ParentOrderResponseCode.ORDER_REACHED_END_TIME) {
      omsAeronService.parentOrderExpired(parentOrder.getOrderId());
    } else {
      omsAeronService.unsolicitedCancelParentOrder(parentOrder.getOrderId(), parentOrderResponseCode);
    }
  }

  @Override
  public void onPausedOrder(ParentOrder parentOrder) {
    omsAeronService.parentOrderPaused(parentOrder.getOrderId());
  }

  @Override
  public void onResumedOrder(ParentOrder parentOrder) {
    omsAeronService.parentOrderResumed(parentOrder.getOrderId());
  }

  @Override
  public void onFilledOrder(ParentOrder parentOrder) {
    parentOrderCache.remove(parentOrder.getOrderId());
    omsAeronService.parentOrderFilled(parentOrder.getOrderId());
  }

  @Override
  public void onNewChildOrder(ParentOrder parentOrder, ChildOrderEvent childOrderEvent) {
    var aeronOrder = getReusableAeronOrder();
    /**/
    aeronOrder.setParentOrderId(parentOrder.getOrderId());
    aeronOrder.setOrderId(IdUtil.syncUniqueLong());
    aeronOrder.setClientId(parentOrder.getClientId());
    aeronOrder.setClientOrderId(childOrderEvent.getOrderId());
    aeronOrder.setAccount(parentOrder.getAccount());
    aeronOrder.setCreateTime(System.currentTimeMillis());
    aeronOrder.setTimeInForceType(childOrderEvent.getTimeInForce());
    aeronOrder.setOrderType(childOrderEvent.getPrice() == 0 ? OrderType.MARKET : OrderType.LIMIT);
    aeronOrder.setOrderSideType(childOrderEvent.getSide());
    aeronOrder.setSymbol(childOrderEvent.getSymbol());
    aeronOrder.setPrice(childOrderEvent.getPrice());
    aeronOrder.setQuantity(childOrderEvent.getQuantity());
    aeronOrder.setCustomerClientOrderId(parentOrder.getCustomerClientOrderId());
    aeronOrder.setDestination(childOrderEvent.getExecutionVenue());

    //    aeronOrder.setRoute(childOrderEvent.getExecutionVenue());
    aeronOrder.setRoute(SystemDomain.TEXO);

    aeronOrder.setVenues(childOrderEvent.getTradeableVenues());
    aeronOrder.setOrderStatusType(OrderStatusType.PENDING);
    aeronOrder.setExecType(ExecTypeType.PENDING_NEW);
    aeronOrder.setOrderLevelType(OrderLevelType.CHILD);
    aeronOrder.setRequestSourceCode(RequestSource.AGENCY.getCode());
    /**/

    omsAeronService.newChildOrder(aeronOrder);
  }

  @Override
  public void onCancelChildOrder(ParentOrder parentOrder, ChildOrderEvent childOrderEvent) {
    omsAeronService.cancelChildOrder(parentOrder.getOrderId(), childOrderEvent.getOrderId());
  }

  @Override
  public void onForceCancelChildOrder(ParentOrder parentOrder, ChildOrderEvent childOrderEvent) {
    omsAeronService.forceCancelChildOrder(parentOrder.getOrderId(), childOrderEvent.getOrderId());
  }

  private AeronOrder getReusableAeronOrder() {
    var aeronOrder = reusableAeronOrder.get();
    aeronOrder.reset();
    return aeronOrder;
  }
}
