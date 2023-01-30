package com.crypto.exchange.caas.service.web;

import com.crypto.commons.utils.IdUtil;
import com.crypto.exchange.oms.common.aeron.event.IAeronOrderEventRouter;
import com.crypto.exchange.oms.common.aeron.model.AeronOrder;
import com.crypto.exchange.oms.common.aeron.request.IParentOrderRequestHandler;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping({"/order"})
@Slf4j
public class OrderController {

  @Autowired
  IAeronOrderEventRouter aeronOrderEventRouter;
  @Autowired
  IParentOrderRequestHandler parentOrderRequestHandler;

  @PostMapping(value = "/process", consumes = {"application/json"})
  public Mono<String> process(@RequestBody OrderRequest request) {
    log.info("request new agency order {}", request);
    AeronOrder ao = new AeronOrder();
    request.write(ao);
    if (ao.getOrderId() == 0) {
      ao.setOrderId(IdUtil.syncUniqueLong());
    }
    parentOrderRequestHandler.handleOrderRequest(Arrays.asList(ao));
    return Mono.just("OK");
  }

}
