package com.crypto.exchange.caas.server;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@Log4j2
@EnableScheduling
@SpringBootApplication
public class CaasServerApp {

  public static void main(String[] args) {
    var context = SpringApplication.run(CaasServerApp.class, args);
    var app = context.getBean(CaasServerApp.class);
    app.init();
  }

  private void init() {
  }
}
