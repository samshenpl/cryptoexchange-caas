package com.crypto.exchange.caas.core.childorder;

public enum ChildOrderState {
  PENDING_LIVE,
  LIVE,
  REJECTED,
  PENDING_CANCELLED,
  CANCELLED,
  FILLED
}
