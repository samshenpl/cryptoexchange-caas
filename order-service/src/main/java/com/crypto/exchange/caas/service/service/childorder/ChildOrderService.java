package com.crypto.exchange.caas.service.service.childorder;

import static com.crypto.exchange.oms.common.core.util.NumberUtils.toDouble;
import static org.apache.logging.log4j.util.Unbox.box;

import com.crypto.commons.datamodel.oms.v1.TimeInForceType;
import com.crypto.exchange.caas.core.childorder.ChildOrder;
import com.crypto.exchange.caas.core.childorder.ChildOrderState;
import com.crypto.exchange.caas.service.order.EventHandlerContext;
import com.crypto.exchange.caas.service.service.adapter.order.IChildOrderClient;
import com.crypto.exchange.caas.service.service.adapter.aeron.AeronInboundEventHandler;
import com.crypto.exchange.enums.Destination;
import com.crypto.exchange.oms.common.core.objectpool.IObjectPool;
import com.crypto.exchange.oms.common.core.objectpool.SimpleObjectPool;
import com.crypto.exchange.oms.common.core.order.child.ICancelOrderAck;
import com.crypto.exchange.oms.common.core.order.child.ICancelOrderReject;
import com.crypto.exchange.oms.common.core.order.child.IChildReport;
import com.crypto.exchange.oms.common.core.order.child.IFullyFilled;
import com.crypto.exchange.oms.common.core.order.child.INewOrderAck;
import com.crypto.exchange.oms.common.core.order.child.INewOrderReject;
import com.crypto.exchange.oms.common.core.order.child.IPartiallyFilled;
import com.crypto.exchange.oms.common.core.order.child.IUnsolicitedCancel;
import com.crypto.exchange.oms.common.core.util.NumberUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Unbox;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("childOrderService")
@Log4j2
public class ChildOrderService implements IChildOrderService, IChildOrderStore, IChildOrderEventHandler {

  private static final String HYPHEN = "-";

  private final ThreadLocal<ChildOrder> reusableChildOrder;
  private final IChildOrderClient childOrderClient;
  private final IObjectPool<ChildOrder> childOrdersPool;
  private final BiConsumer<EventHandlerContext, ChildOrder> childOrderValidator;
  private final Function<EventHandlerContext, List<Destination>> venuesResolver;

  public ChildOrderService(IChildOrderClient childOrderClient, AeronInboundEventHandler eventHandler) {
    this.childOrderClient = childOrderClient;
    this.childOrdersPool = new SimpleObjectPool<> (ChildOrder::new, ChildOrder.class);
    this.childOrderValidator = (context, childOrder) -> {
      log.info("validate child order {}", childOrder);
    };
    this.venuesResolver = context -> {
      return Arrays.asList(Destination.CDC);
    };

    reusableChildOrder = ThreadLocal.withInitial(ChildOrder::new);
    eventHandler.setChildOrderService(this);
    eventHandler.setChildOrderEventHandler(this);
    eventHandler.setChildOrderClient(childOrderClient);
  }

  @Override
  public void createChild(EventHandlerContext context, long price, long quantity, TimeInForceType timeInForce, Destination executionVenue) {
    var parentOrder = context.getOrder();
    var childOrder = childOrdersPool.take();
    childOrder.setOrderId(generateChildOrderId(context));
    childOrder.setParentOrderId(parentOrder.getOrderId());
    childOrder.setPrice(price);
    childOrder.setQuantity(quantity);
    childOrder.setTimeInForce(timeInForce);
    childOrder.setExecutionVenue(executionVenue);
//    if (executionVenue == SOR) {
//      childOrder.getTradeableVenues().addAll(venuesResolver.apply(context));
//    }
    childOrder.setSymbol(parentOrder.getSymbol());
    childOrder.setSide(parentOrder.getOrderSideType());
    childOrder.setOrderState(ChildOrderState.PENDING_LIVE);
    getChildOrderServiceContext(context).childOrderGroupingStore.addToStore(childOrder);
    getChildOrderServiceContext(context).totalChildQuantity += quantity;

    childOrderValidator.accept(context, childOrder);
    childOrderClient.sendChild(parentOrder, childOrder);
    log.info("[{}] New child order {} - {}@{} {} to {} {}",
        box(getOrderId(context)),
        childOrder.getOrderId(),
        Unbox.box(NumberUtils.toDouble(quantity)),
        Unbox.box(NumberUtils.toDouble(price)),
        timeInForce,
        executionVenue,
        childOrder.getTradeableVenues());
    logChildOrders(context);
  }

  private long getOrderId(EventHandlerContext context) {
    return context.getOrder().getOrderId();
  }

  @Override
  public void cancelChild(EventHandlerContext context, ChildOrder childOrder) {

    var child = getChildOrderServiceContext(context).childOrderGroupingStore.getOrderFromStore(childOrder.getOrderId());
    if (child != null) {
      if (childOrder.getOrderState() == ChildOrderState.PENDING_CANCELLED) {
        log.warn("Unable to cancel {} order: {}", childOrder.getOrderState(), childOrder.getOrderId());
        return;
      }
      child.setOrderState(ChildOrderState.PENDING_CANCELLED);
      childOrderClient.cancelChild(context.getOrder(), childOrder);
      logChildOrders(context);

      log.info("[{}] Cancel child order: {}. Remaining: {}",
          box(getOrderId(context)),
          childOrder.getOrderId(),
          Unbox.box(toDouble(child.getRemainingQuantity()))
      );
    } else {
      log.warn("Failed to find child order from store with orderId: {}", childOrder.getOrderId());
    }
  }

  private void logChildOrders(EventHandlerContext context) {
    var parentOrderId = getOrderId(context);
    var childOrders =  getChildOrderServiceContext(context).childOrderGroupingStore.getOrdersFromStore();
    log.info("[{}] Child Order Book:", box(parentOrderId));
    for (var childOrder : childOrders) {
      log.info("[{}] Child:{},{}@{},{},{},{},Filled:{}",
               box(parentOrderId),
               childOrder.getOrderId(),
               Unbox.box(toDouble(childOrder.getQuantity())),
               Unbox.box(toDouble(childOrder.getPrice())),
               childOrder.getTimeInForce(),
               childOrder.getExecutionVenue(),
               childOrder.getOrderState(),
               Unbox.box(toDouble(childOrder.getFilledQuantity())));
    }
  }

  @Override
  public void forceCancelAllChild(EventHandlerContext context) {
    var localContext = getChildOrderServiceContext(context);
    var childOrderStore = localContext.childOrderGroupingStore;
    for (var childOrder : childOrderStore.getOrdersFromStore()) {
      var remainingQuantity = childOrder.getRemainingQuantity();
      childOrderClient.forceCancelChild(context.getOrder(), childOrder);

      log.info("[{}] Force canceling child order: {}. Remaining: {}",
               box(getOrderId(context)),
               childOrder.getOrderId(),
               Unbox.box(NumberUtils.toDouble(remainingQuantity))
      );

      localContext.totalChildQuantity -= remainingQuantity;
      childOrderStore.removeFromStore(childOrder.getOrderId());
    }
  }

  @Override
  public ChildOrder getChildOrderByChildOrderId(EventHandlerContext context, String childOrderId) {
    return getChildOrderServiceContext(context).childOrderGroupingStore.getOrderFromStore(childOrderId);
  }

  @Override
  public List<ChildOrder> getChildOrders(EventHandlerContext context) {
    return getChildOrderServiceContext(context).childOrderGroupingStore.getOrdersFromStore();
  }

//  @Override
//  public List<OmsOrder> getChildOrdersByGroup(AgencyEventHandlerContext context, GoalGroup group) {
//    return getChildOrderServiceContext(context).childOrderGroupingStore.getOrdersByGroup(group);
//  }
//
//  @Override
//  public List<OmsOrder> getChildOrdersByGroupAndPrice(AgencyEventHandlerContext context, GoalGroup group, long price) {
//    return getChildOrderServiceContext(context).childOrderGroupingStore.getOrdersByGroupAndPrice(group, price);
//  }
//
//  @Override
//  public LongArrayList getSortedPriceLevels(AgencyEventHandlerContext context, GoalGroup group, Comparator<Long> comparator) {
//    return getChildOrderServiceContext(context).childOrderGroupingStore.getSortedPriceLevels(group, comparator);
//  }
//
//  @Override
//  public long getChildQuantityByGroupAndPrice(AgencyEventHandlerContext context, GoalGroup group, long price) {
//    var childOrders = getChildOrdersByGroupAndPrice(context, group, price);
//    var quantity = 0L;
//    for (var childOrder : childOrders) {
//      quantity += childOrder.getRemainingQuantity();
//    }
//    return quantity;
//  }

  @Override
  public long getTotalChildQuantity(EventHandlerContext context) {
    return getChildOrderServiceContext(context).totalChildQuantity;
  }

  @Override
  public ChildOrder onChildReport(EventHandlerContext context, IChildReport childReport) {
    log.info("[{}] Receiving child report {} with childOrderId: {}",
        Unbox.box(context.getOrder().getOrderId()),
        childReport.getClass().getSimpleName(),
        childReport.getOrderId()
    );
    if (childReport instanceof ICancelOrderAck) {
      return onCancelChildAccept(context, (ICancelOrderAck) childReport);
    } else if (childReport instanceof ICancelOrderReject) {
      return onCancelChildReject(context, (ICancelOrderReject) childReport);
    } else if (childReport instanceof IUnsolicitedCancel) {
      return onUnsolicitedCancelChild(context, (IUnsolicitedCancel) childReport);
    } else if (childReport instanceof IFullyFilled) {
      return onFullyFillChild(context, (IFullyFilled) childReport);
    } else if (childReport instanceof IPartiallyFilled) {
      return onPartiallyFillChild(context, (IPartiallyFilled) childReport);
    } else if (childReport instanceof INewOrderAck) {
      return onNewChildAck(context, (INewOrderAck) childReport);
    } else if (childReport instanceof INewOrderReject) {
      return onNewChildReject(context, (INewOrderReject) childReport);
    }
    throw new IllegalArgumentException("Unexpected report type: " + childReport.getClass().getSimpleName());
  }

  @Override
  public void onChildOrderRecovery(EventHandlerContext context, ChildOrder childOrder) {
    var recoveredOrder = childOrdersPool.take();
    recoveredOrder.copyFrom(childOrder);
    getChildOrderServiceContext(context).childOrderGroupingStore.addToStore(recoveredOrder);
    getChildOrderServiceContext(context).totalChildQuantity += recoveredOrder.getRemainingQuantity();
//    GoalGroup.of(childOrder);
    log.info("[{}] Recovered child order {} - {}@{} {} at {} Filled: {}, {}",
             box(getOrderId(context)),
             childOrder.getOrderId(),
             Unbox.box(toDouble(recoveredOrder.getQuantity())),
             Unbox.box(toDouble(recoveredOrder.getPrice())),
             recoveredOrder.getTimeInForce(),
             recoveredOrder.getExecutionVenue(),
             Unbox.box(toDouble(recoveredOrder.getFilledQuantity())),
             recoveredOrder.getOrderState()
    );
  }

  private ChildOrder onCancelChildAccept(EventHandlerContext context, ICancelOrderAck cancelOrderAck) {
    var childOrder = getChildOrderServiceContext(context).childOrderGroupingStore.removeFromStore(cancelOrderAck.getOrderId());
    if (childOrder != null && childOrder.getOrderState() == ChildOrderState.PENDING_CANCELLED) {
      var result = getReusableChildOrder();
      getChildOrderServiceContext(context).totalChildQuantity -= childOrder.getRemainingQuantity();
      childOrder.setOrderState(ChildOrderState.CANCELLED);
      result.copyFrom(childOrder);
      childOrdersPool.release(childOrder);
      return result;
    } else {
      log.warn("Failed to process cancel child accept event with order: {}", cancelOrderAck.getOrderId());
      return null;
    }
  }

  private ChildOrder onCancelChildReject(EventHandlerContext context, ICancelOrderReject cancelOrderReject) {
    var childOrder = getChildOrderServiceContext(context).childOrderGroupingStore.getOrderFromStore(cancelOrderReject.getOrderId());
    if (childOrder != null && childOrder.getOrderState() == ChildOrderState.PENDING_CANCELLED) {
      childOrder.setOrderState(ChildOrderState.LIVE);
    } else {
      log.warn("Failed to process cancel child reject event with order: {}", cancelOrderReject.getOrderId());
    }
    return childOrder;
  }

  private ChildOrder onUnsolicitedCancelChild(EventHandlerContext context, IUnsolicitedCancel unsolicitedCancel) {
    var childOrder = getChildOrderServiceContext(context).childOrderGroupingStore.removeFromStore(unsolicitedCancel.getOrderId());
    if (childOrder != null) {
      getChildOrderServiceContext(context).totalChildQuantity -= childOrder.getRemainingQuantity();
      childOrder.setOrderState(ChildOrderState.CANCELLED);
      var result = getReusableChildOrder();
      result.copyFrom(childOrder);
      childOrdersPool.release(childOrder);
      return result;
    } else {
      log.warn("Failed to process unsolicited cancel child event with order: {}", unsolicitedCancel.getOrderId());
      return null;
    }
  }

  private ChildOrder onFullyFillChild(EventHandlerContext context, IFullyFilled fullyFilled) {
    var childOrder = getChildOrderServiceContext(context).childOrderGroupingStore.removeFromStore(fullyFilled.getOrderId());
    if (childOrder != null) {
      var lastQuantity = fullyFilled.getLastQuantity();
      childOrder.setFilledQuantity(childOrder.getFilledQuantity() + lastQuantity);
      getChildOrderServiceContext(context).totalChildQuantity -= lastQuantity;
      childOrder.setOrderState(ChildOrderState.FILLED);
      var result = getReusableChildOrder();
      result.copyFrom(childOrder);
      childOrdersPool.release(childOrder);
      return result;
    } else {
      log.warn("Failed to process fully filled child event with order: {}", fullyFilled.getOrderId());
      return null;
    }
  }

  private ChildOrder onPartiallyFillChild(EventHandlerContext context, IPartiallyFilled partiallyFilled) {
    var childOrder = getChildOrderServiceContext(context).childOrderGroupingStore.getOrderFromStore(partiallyFilled.getOrderId());
    if (childOrder != null) {
      var lastQuantity = partiallyFilled.getLastQuantity();
      childOrder.setFilledQuantity(childOrder.getFilledQuantity() + lastQuantity);
      getChildOrderServiceContext(context).totalChildQuantity -= lastQuantity;
    } else {
      log.warn("Failed to process partially filled child event with order: {}", partiallyFilled.getOrderId());
    }
    return childOrder;
  }

  private ChildOrder onNewChildAck(EventHandlerContext context, INewOrderAck newOrderAck) {
    var childOrder = getChildOrderServiceContext(context).childOrderGroupingStore.getOrderFromStore(newOrderAck.getOrderId());
    if (childOrder != null && childOrder.getOrderState() == ChildOrderState.PENDING_LIVE) {
      childOrder.setOrderState(ChildOrderState.LIVE);
    } else {
      log.warn("Failed to process new child accept event with order: {}", newOrderAck.getOrderId());
    }
    return childOrder;
  }

  private ChildOrder onNewChildReject(EventHandlerContext context, INewOrderReject newOrderReject) {
    var childOrder = getChildOrderServiceContext(context).childOrderGroupingStore.removeFromStore(newOrderReject.getOrderId());
    if (childOrder != null) {
      getChildOrderServiceContext(context).totalChildQuantity -= childOrder.getRemainingQuantity();
      childOrder.setOrderState(ChildOrderState.REJECTED);
      var result = getReusableChildOrder();
      result.copyFrom(childOrder);
      childOrdersPool.release(childOrder);
      return result;
    } else {
      log.warn("Failed to process new child reject event with order:  {}", newOrderReject.getOrderId());
      return null;
    }
  }

  private String generateChildOrderId(EventHandlerContext context) {
    var stringBuilder = getChildOrderServiceContext(context).childOrderIdStringBuilder;
    var length = stringBuilder.length();
    if (length == 0) {
      stringBuilder
          .append(context.getOrder().getOrderId())
          .append(HYPHEN)
          .append("0")
          .append(HYPHEN);
      length = stringBuilder.length();
    }
    var orderId = stringBuilder
        .append(Integer.toString(getChildOrderServiceContext(context).childOrderCounter++, Character.MAX_RADIX) // base36 encode
        .toUpperCase())
        .toString();
    stringBuilder.delete(length, stringBuilder.length());
    return orderId;
  }

  private ChildOrderServiceContext getChildOrderServiceContext(EventHandlerContext context) {
    return context.getContext(ChildOrderServiceContext.class, ChildOrderServiceContext::new);
  }

  private ChildOrder getReusableChildOrder() {
    var childOrder = reusableChildOrder.get();
    childOrder.reset();
    return childOrder;
  }

  private static class ChildOrderServiceContext {

    private final ChildOrderGroupingStore childOrderGroupingStore = new ChildOrderGroupingStore();
    private final StringBuilder childOrderIdStringBuilder = new StringBuilder();
    private int childOrderCounter;
    private long totalChildQuantity;
  }

  static class ChildOrderGroupingStore {

    Map<String, ChildOrder> store = new ConcurrentHashMap<>();

    public void addToStore(ChildOrder childOrder) {
      store.put(childOrder.getOrderId(), childOrder);
    }

    public ChildOrder getOrderFromStore(String orderId) {
      return store.get(orderId);
    }

    public List<ChildOrder> getOrdersFromStore() {
      return new ArrayList<>(store.values());
    }

    public ChildOrder removeFromStore(String orderId) {
      return store.remove(orderId);
    }
  }
}
