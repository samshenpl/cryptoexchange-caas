package com.crypto.exchange.caas.service.order;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import lombok.Getter;

@Getter
public class EventHandlerContext {

  private final Map<Class, Object> contexts = new HashMap<>();

  private final ParentOrder order;

  public EventHandlerContext(ParentOrder order) {
    this.order = order;
  }

  public <T> T getContext(Class<T> contextClass, Supplier<T> supplier) {
    return (T) contexts.computeIfAbsent(contextClass, clazz -> supplier.get());
  }
}
