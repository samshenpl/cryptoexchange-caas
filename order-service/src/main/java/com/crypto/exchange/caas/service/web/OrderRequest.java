package com.crypto.exchange.caas.service.web;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.agrona.collections.Object2ObjectHashMap;

@Getter
@Setter
@ToString
public class OrderRequest {

  private long orderId;
  private String clientOrderId;
  private long createTime;
  private long updateTime;
  private long clientTime;
  private long matchingTime;
  private String account;
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
  private String settlementFeeCurrency;
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
  private long parentOrderId;
  private String customerClientOrderId;
  private String preAggSourceSequence;
  private OrderLevelType orderLevelType;
  private Destination destination;
  private final List<Destination> venues = new ArrayList<>();
  private SystemDomain route;
  private InternalStateType internalState;
  private int requestSourceCode;
  private final Map<String, Object> orderParams = new Object2ObjectHashMap<>();
  private long downstreamOrderId;
  private RequestType requestType;
  private String clientId;

  private String endUserTag;
  public void write(AeronOrder aeronOrder) {
    aeronOrder.reset();
    
    aeronOrder.setOrderId(orderId); // long orderId;
    aeronOrder.setClientOrderId(clientOrderId); // String clientOrderId;
    aeronOrder.setCreateTime(createTime); // long createTime;
    aeronOrder.setUpdateTime(updateTime); // long updateTime;
    aeronOrder.setClientTime(clientTime); // long clientTime;
    aeronOrder.setMatchingTime(matchingTime); // long matchingTime;
    aeronOrder.setAccount(account); // String account;
    aeronOrder.setOrderStatusType(orderStatusType); // OrderStatusType orderStatusType;
    aeronOrder.setExecType(execType); // ExecTypeType execType;
    aeronOrder.setOrderType(orderType); // OrderType orderType;
    aeronOrder.setTimeInForceType(timeInForceType); // TimeInForceType timeInForceType;
    aeronOrder.setOrderSideType(orderSideType); // OrderSideType orderSideType;
    aeronOrder.setSymbol(symbol); // String symbol;
    aeronOrder.setPrice(price); // long price;
    aeronOrder.setQuantity(quantity); // long quantity;
    aeronOrder.setCumulativeQuantity(cumulativeQuantity); // long cumulativeQuantity;
    aeronOrder.setOrderValue(orderValue); // long orderValue;
    aeronOrder.setCumulativeOrderValue(cumulativeOrderValue); // long cumulativeOrderValue;
    aeronOrder.setCumulativeFee(cumulativeFee); // long cumulativeFee;
    aeronOrder.setFeeCurrency(feeCurrency); // String feeCurrency;
    aeronOrder.setSettlementFeeCurrency(settlementFeeCurrency); // String settlementFeeCurrency;
    aeronOrder.setSourceSequence(sourceSequence); // long sourceSequence;
    aeronOrder.setChannelSequence(channelSequence); // long channelSequence;
    aeronOrder.setResponseCode(responseCode); // ExchangeResponseCode responseCode;
    aeronOrder.setMetaInfo(metaInfo); // String metaInfo;
    aeronOrder.setExecInst(execInst); // short execInst;
    aeronOrder.setTriggerPrice(triggerPrice); // long triggerPrice;
    aeronOrder.setAlwaysPublish(alwaysPublish); // boolean alwaysPublish;
    aeronOrder.setIncomingTime(incomingTime); // long incomingTime;
    aeronOrder.setContingencyId(contingencyId); // long contingencyId;
    aeronOrder.setContingencyType(contingencyType); // ContingencyType contingencyType;
    aeronOrder.setParentOrderId(parentOrderId); // long parentOrderId;
    aeronOrder.setCustomerClientOrderId(customerClientOrderId); // String customerClientOrderId;
    aeronOrder.setPreAggSourceSequence(preAggSourceSequence); // String preAggSourceSequence;
    aeronOrder.setOrderLevelType(orderLevelType); // OrderLevelType orderLevelType;
    aeronOrder.setDestination(destination); // Destination destination;
    aeronOrder.setVenues(venues); // final List<Destination> venues = new ArrayList();
    aeronOrder.setRoute(route); // SystemDomain route;
    aeronOrder.setInternalState(internalState); // InternalStateType internalState;
    aeronOrder.setRequestSourceCode(requestSourceCode); // int requestSourceCode;
    aeronOrder.setEndUserTag(endUserTag);

    aeronOrder.getOrderParams().clear();
    for (var entry : orderParams.entrySet()) {
      final OrderParamInfo<?> key = OrderParamInfo.fromKey(entry.getKey());
      Object value = entry.getValue();
      if (key.getParamType() == Long.class && value instanceof Number) {
        value = ((Number) value).longValue();
      }
      aeronOrder.getOrderParams().put(key, value);
    }
    // final Map<OrderParamInfo<?>, Object> orderParams = new Object2ObjectHashMap();

    aeronOrder.setDownstreamOrderId(downstreamOrderId); // long downstreamOrderId;
    aeronOrder.setRequestType(requestType); // RequestType requestType;
    aeronOrder.setClientId(clientId); // String clientId;

  }
}
