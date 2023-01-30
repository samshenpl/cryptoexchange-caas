package com.crypto.exchange.caas.service.service.metrics;

import com.crypto.commons.datamodel.oms.v1.ExecTypeType;
import com.crypto.commons.datamodel.oms.v1.OrderStatusType;
import com.crypto.exchange.caas.service.order.InboundEvent;
import com.crypto.exchange.oms.common.aeron.model.AeronOrder;
import com.crypto.exchange.oms.common.aeron.request.IParentOrderRequestRejectHandler;
import com.crypto.exchange.oms.common.core.order.ParentOrderResponseCode;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InboundEventMetrics implements Consumer<InboundEvent>, IParentOrderRequestRejectHandler {

  @Override
  public void accept(InboundEvent inboundEvent) {
    log.info("child inbound event {}", inboundEvent);
  }

  @Override
  public void onParentOrderRequestRejected(OrderStatusType orderStatusType, ExecTypeType execTypeType, AeronOrder aeronOrder, ParentOrderResponseCode parentOrderResponseCode) {
    log.info("reject parent order request {} {} {} {}", orderStatusType, execTypeType, aeronOrder, parentOrderResponseCode);
  }
}
