package com.crypto.exchange.caas.service.service.childorder;

import com.crypto.exchange.oms.common.core.order.child.ICancelOrderAck;
import com.crypto.exchange.oms.common.core.order.child.ICancelOrderReject;
import com.crypto.exchange.oms.common.core.order.child.IFullyFilled;
import com.crypto.exchange.oms.common.core.order.child.INewOrderAck;
import com.crypto.exchange.oms.common.core.order.child.INewOrderReject;
import com.crypto.exchange.oms.common.core.order.child.IPartiallyFilled;
import com.crypto.exchange.oms.common.core.order.child.IUnsolicitedCancel;

public interface IChildOrderInboundEventHandler {

  void onNewChildOrderAck(long parentOrderId, INewOrderAck newOrderAck);

  void onNewChildOrderReject(long parentOrderId, INewOrderReject newOrderReject);

  void onCancelChildOrderAck(long parentOrderId, ICancelOrderAck cancelOrderAck);

  void onCancelChildOrderReject(long parentOrderId, ICancelOrderReject cancelOrderReject);

  void onChildOrderFullyFilled(long parentOrderId, IFullyFilled fullyFilled);

  void onChildOrderPartiallyFilled(long parentOrderId, IPartiallyFilled partiallyFilled);

  void onChildOrderUnsolicitedCancel(long parentOrderId, IUnsolicitedCancel unsolicitedCancel);
}
