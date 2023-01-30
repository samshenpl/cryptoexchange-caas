package com.crypto.exchange.caas.service.service.requestsource;

import com.crypto.exchange.enums.RequestSource;
import com.crypto.exchange.oms.common.aeron.IRequestSourceSupplier;
import org.springframework.stereotype.Component;

@Component
public class AgencyRequestSource implements IRequestSourceSupplier {

  @Override
  public RequestSource get() {
    return RequestSource.AGENCY;
  }
}
