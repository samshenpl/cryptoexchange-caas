package com.crypto.exchange.caas.service.service.recovery;

import com.crypto.exchange.oms.common.aeron.recovery.IRecoveryController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RecoveryController implements IRecoveryController {

  @Override
  public void recover() {
    log.info("agency config 2 recovery end");
  }
}
