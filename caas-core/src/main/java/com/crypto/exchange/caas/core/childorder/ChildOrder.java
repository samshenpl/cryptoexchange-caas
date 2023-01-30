package com.crypto.exchange.caas.core.childorder;

import com.crypto.commons.datamodel.oms.v1.OrderSideType;
import com.crypto.commons.datamodel.oms.v1.TimeInForceType;
import com.crypto.exchange.enums.Destination;
import com.crypto.exchange.oms.common.core.objectpool.Resettable;
import com.crypto.exchange.oms.common.core.util.NumberUtils;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChildOrder extends Resettable {

  private String orderId;
  private long parentOrderId;
  private String symbol;
  private OrderSideType side;
  private long price;
  private long quantity;
  private long filledQuantity;
  private TimeInForceType timeInForce;
  private Destination executionVenue;
  private final List<Destination> tradeableVenues = new ArrayList<>();
  private ChildOrderState orderState;

  public long getRemainingQuantity() {
    return quantity - filledQuantity;
  }

  public void copyFrom(ChildOrder childOrder) {
    orderId = childOrder.getOrderId();
    parentOrderId = childOrder.getParentOrderId();
    symbol = childOrder.getSymbol();
    side = childOrder.getSide();
    price = childOrder.getPrice();
    quantity = childOrder.getQuantity();
    filledQuantity = childOrder.getFilledQuantity();
    timeInForce = childOrder.getTimeInForce();
    executionVenue = childOrder.getExecutionVenue();
    tradeableVenues.clear();
    tradeableVenues.addAll(childOrder.getTradeableVenues());
    orderState = childOrder.getOrderState();
  }

  @Override
  public void reset() {
    orderId = null;
    parentOrderId = NumberUtils.EMPTY_VALUE;
    symbol = null;
    side = null;
    price = NumberUtils.EMPTY_VALUE;
    quantity = NumberUtils.EMPTY_VALUE;
    filledQuantity = 0L;
    timeInForce = null;
    executionVenue = null;
    tradeableVenues.clear();
    orderState = null;
  }
}
