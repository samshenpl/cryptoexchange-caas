package com.crypto.exchange.caas.service.order;

import com.crypto.commons.datamodel.oms.v1.ContingencyType;
import com.crypto.commons.datamodel.oms.v1.ExecTypeType;
import com.crypto.commons.datamodel.oms.v1.InternalStateType;
import com.crypto.commons.datamodel.oms.v1.OrderLevelType;
import com.crypto.commons.datamodel.oms.v1.OrderSideType;
import com.crypto.commons.datamodel.oms.v1.OrderStatusType;
import com.crypto.commons.datamodel.oms.v1.OrderType;
import com.crypto.commons.datamodel.oms.v1.RequestType;
import com.crypto.commons.datamodel.oms.v1.TimeInForceType;
import com.crypto.exchange.enums.Destination;
import com.crypto.exchange.enums.OrderParamInfo;
import com.crypto.exchange.enums.SystemDomain;
import com.crypto.exchange.exception.ExchangeResponseCode;
import com.crypto.exchange.oms.common.aeron.model.AeronOrder;
import com.crypto.exchange.oms.common.core.objectpool.Resettable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.agrona.collections.Object2ObjectHashMap;

@Setter
@Getter
@ToString
public class ParentOrder extends Resettable {

  private long orderId;
  private String clientOrderId;
  private long createTime;
  private long updateTime;
  private long clientTime;
  private long matchingTime;
  private String account; // map to accountUuid in PT/PTW
  private OrderStatusType orderStatusType;
  private ExecTypeType execType;
  private OrderType orderType;
  private TimeInForceType timeInForceType;
  private OrderSideType orderSideType;
  private String symbol;
  private long price;
  private long quantity;
  private long cumulativeQuantity;
  private long orderValue;
  private long cumulativeOrderValue;
  private long cumulativeFee;
  private String feeCurrency;
  private long sourceSequence;
  private long channelSequence;
  private ExchangeResponseCode responseCode;
  private String metaInfo;
  private short execInst;
  private long triggerPrice;
  private boolean alwaysPublish;
  private long incomingTime;
  private long contingencyId;
  private ContingencyType contingencyType;

  // New fields
  private long parentOrderId;
  private String customerClientOrderId;
  private String preAggSourceSequence;
  private OrderLevelType orderLevelType;
  private Destination destination;
  private final List<Destination> venues = new ArrayList<>();
  private SystemDomain route;
  private InternalStateType internalState;
  private int requestSourceCode;
  private final Map<OrderParamInfo<?>, Object> orderParams = new Object2ObjectHashMap<>();
  private long downstreamOrderId;

  // internal use
  private RequestType requestType;
  private String clientId;

  public void setVenues(List<Destination> venues) {
    this.venues.clear();
    this.venues.addAll(venues);
  }

  @Override
  public void reset() {
    orderId = 0;
    parentOrderId = 0;
    clientOrderId = null;
    customerClientOrderId = null;
    createTime = 0;
    updateTime = 0;
    clientTime = 0;
    matchingTime = 0;
    account = null;
    orderStatusType = OrderStatusType.NULL_VAL;
    execType = ExecTypeType.NULL_VAL;
    orderType = OrderType.NULL_VAL;
    timeInForceType = TimeInForceType.NULL_VAL;
    orderSideType = OrderSideType.NULL_VAL;
    symbol = null;
    price = 0;
    quantity = 0;
    cumulativeQuantity = 0;
    orderValue = 0;
    cumulativeOrderValue = 0;
    cumulativeFee = 0;
    feeCurrency = null;
    sourceSequence = 0;
    channelSequence = 0;
    responseCode = null;
    metaInfo = null;
    execInst = 0;
    triggerPrice = 0;
    alwaysPublish = false;
    incomingTime = 0;
    contingencyId = 0;
    contingencyType = ContingencyType.NULL_VAL;
    preAggSourceSequence = null;
    orderLevelType = OrderLevelType.NULL_VAL;
    destination = Destination.NULL_VAL;
    venues.clear();
    route = SystemDomain.NULL_VAL;
    internalState = InternalStateType.NULL_VAL;
    requestSourceCode = 0;
    orderParams.clear();
    downstreamOrderId = 0;
    requestType = null;
    clientId = null;
  }

  public void copyFrom(ParentOrder order) {
    orderId = order.getOrderId();
    parentOrderId = order.getParentOrderId();
    clientOrderId = order.getClientOrderId();
    customerClientOrderId = order.getCustomerClientOrderId();
    createTime = order.getCreateTime();
    clientTime = order.getClientTime();
    updateTime = order.getUpdateTime();
    matchingTime = order.getMatchingTime();
    symbol = order.getSymbol();
    account = order.getAccount();
    orderType = order.getOrderType();
    timeInForceType = order.getTimeInForceType();
    orderSideType = order.getOrderSideType();
    orderLevelType = order.getOrderLevelType();
    orderStatusType = order.getOrderStatusType();
    execType = order.getExecType();
    price = order.getPrice();
    quantity = order.getQuantity();
    cumulativeQuantity = order.getCumulativeQuantity();
    orderValue = order.getOrderValue();
    cumulativeOrderValue = order.getCumulativeOrderValue();
    cumulativeFee = order.getCumulativeFee();
    feeCurrency = order.getFeeCurrency();
    sourceSequence = order.getSourceSequence();
    channelSequence = order.getChannelSequence();
    responseCode = order.getResponseCode();
    metaInfo = order.getMetaInfo();
    execInst = order.getExecInst();
    triggerPrice = order.getTriggerPrice();
    alwaysPublish = order.isAlwaysPublish();
    incomingTime = order.getIncomingTime();
    contingencyId = order.getContingencyId();
    contingencyType = order.getContingencyType();
    preAggSourceSequence = order.getPreAggSourceSequence();
    destination = order.getDestination();
    venues.clear();
    venues.addAll(order.getVenues());
    route = order.getRoute();
    internalState = order.getInternalState();
    requestSourceCode = order.getRequestSourceCode();
    orderParams.clear();
    orderParams.putAll(order.getOrderParams());
    downstreamOrderId = order.getDownstreamOrderId();
    requestType = order.getRequestType();
    clientId = order.getClientId();
  }

  public static ParentOrder valueOf(AeronOrder aeronOrder) {
    final ParentOrder order = new ParentOrder();
    order.orderId = aeronOrder.getOrderId();
    order.parentOrderId = aeronOrder.getParentOrderId();
    order.clientOrderId = aeronOrder.getClientOrderId();
    order.customerClientOrderId = aeronOrder.getCustomerClientOrderId();
    order.createTime = aeronOrder.getCreateTime();
    order.clientTime = aeronOrder.getClientTime();
    order.updateTime = aeronOrder.getUpdateTime();
    order.matchingTime = aeronOrder.getMatchingTime();
    order.symbol = aeronOrder.getSymbol();
    order.account = aeronOrder.getAccount();
    order.orderType = aeronOrder.getOrderType();
    order.timeInForceType = aeronOrder.getTimeInForceType();
    order.orderSideType = aeronOrder.getOrderSideType();
    order.orderLevelType = aeronOrder.getOrderLevelType();
    order.orderStatusType = aeronOrder.getOrderStatusType();
    order.execType = aeronOrder.getExecType();
    order.price = aeronOrder.getPrice();
    order.quantity = aeronOrder.getQuantity();
    order.cumulativeQuantity = aeronOrder.getCumulativeQuantity();
    order.orderValue = aeronOrder.getOrderValue();
    order.cumulativeOrderValue = aeronOrder.getCumulativeOrderValue();
    order.cumulativeFee = aeronOrder.getCumulativeFee();
    order.feeCurrency = aeronOrder.getFeeCurrency();
    order.sourceSequence = aeronOrder.getSourceSequence();
    order.channelSequence = aeronOrder.getChannelSequence();
    order.responseCode = aeronOrder.getResponseCode();
    order.metaInfo = aeronOrder.getMetaInfo();
    order.execInst = aeronOrder.getExecInst();
    order.triggerPrice = aeronOrder.getTriggerPrice();
    order.alwaysPublish = aeronOrder.isAlwaysPublish();
    order.incomingTime = aeronOrder.getIncomingTime();
    order.contingencyId = aeronOrder.getContingencyId();
    order.contingencyType = aeronOrder.getContingencyType();
    order.preAggSourceSequence = aeronOrder.getPreAggSourceSequence();
    order.destination = aeronOrder.getDestination();
    order.venues.clear();
    order.venues.addAll(aeronOrder.getVenues());
    order.route = aeronOrder.getRoute();
    order.internalState = aeronOrder.getInternalState();
    order.requestSourceCode = aeronOrder.getRequestSourceCode();
    order.orderParams.clear();
    order.orderParams.putAll(aeronOrder.getOrderParams());
    order.downstreamOrderId = aeronOrder.getDownstreamOrderId();
    order.requestType = aeronOrder.getRequestType();
    order.clientId = aeronOrder.getClientId();
    return order;
  }

}
