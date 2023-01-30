package com.crypto.exchange.caas.service.service.childorder;

import com.crypto.exchange.caas.core.childorder.ChildOrder;
import com.crypto.exchange.caas.service.order.EventHandlerContext;
import java.util.List;

public interface IChildOrderStore {

  ChildOrder getChildOrderByChildOrderId(EventHandlerContext context, String childOrderId);

  List<ChildOrder> getChildOrders(EventHandlerContext context);

//  List<OmsOrder> getChildOrdersByGroup(AgencyEventHandlerContext context, GoalGroup group);
//
//  List<OmsOrder> getChildOrdersByGroupAndPrice(AgencyEventHandlerContext context, GoalGroup group, long price);
//
//  LongArrayList getSortedPriceLevels(AgencyEventHandlerContext context, GoalGroup group, Comparator<Long> comparator);
//
//  long getChildQuantityByGroupAndPrice(AgencyEventHandlerContext context, GoalGroup group, long price);

  long getTotalChildQuantity(EventHandlerContext context);
}
