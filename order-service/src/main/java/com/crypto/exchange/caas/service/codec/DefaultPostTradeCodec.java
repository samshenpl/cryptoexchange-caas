package com.crypto.exchange.caas.service.codec;

import com.crypto.commons.datamodel.oms.v1.PostTradeEventEncoder;
import com.crypto.commons.datamodel.oms.v1.PostTradeEventEncoder.OrderExtsEncoder;
import com.crypto.exchange.oms.common.aeron.IRequestSourceSupplier;
import com.crypto.exchange.oms.common.aeron.codec.IVenuesCodec;
import com.crypto.exchange.oms.common.aeron.codec.PostTradeCodec;
import com.crypto.exchange.oms.common.aeron.model.AeronFill;
import com.crypto.exchange.oms.common.aeron.model.AeronOrder;
import com.crypto.exchange.oms.common.aeron.response.ChildOrderResponse;
import com.crypto.exchange.oms.common.aeron.util.IAeronSequenceStore;
import com.crypto.exchange.oms.common.core.IClock;
import java.lang.reflect.Field;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.UnsafeBuffer;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DefaultPostTradeCodec extends PostTradeCodec {

  private final Field encoderField;
  private final Field orderExtsField;
  private final Field orderExtEncoderIndex;
  private final Field orderExtEncoderCount;

  public DefaultPostTradeCodec(IAeronSequenceStore sequenceStore, IVenuesCodec venuesCodec,
                               IRequestSourceSupplier requestSourceSupplier, IClock clock) throws NoSuchFieldException {
    super(sequenceStore, venuesCodec, requestSourceSupplier, clock);
    encoderField = PostTradeCodec.class.getDeclaredField("postTradeEventEncoder");
    encoderField.setAccessible(true);
    orderExtsField = PostTradeEventEncoder.class.getDeclaredField("orderExts");
    orderExtsField.setAccessible(true);
    orderExtEncoderIndex = OrderExtsEncoder.class.getDeclaredField("index");
    orderExtEncoderIndex.setAccessible(true);
    orderExtEncoderCount = OrderExtsEncoder.class.getDeclaredField("count");
    orderExtEncoderCount.setAccessible(true);
  }

  @Override
  public UnsafeBuffer encodePostTradeEvent(List<ChildOrderResponse> responseList, List<AeronOrder> parentOrders, List<AeronFill> parentFills) {
    if (log.isDebugEnabled()) {
      log.debug("encodePostTradeEvent {}:{}:{}", responseList.size(), parentOrders.size(), parentFills.size());
    }
    try {
      return super.encodePostTradeEvent(responseList, parentOrders, parentFills);
    } catch (NoSuchElementException nsee) {
      if (log.isDebugEnabled()) {
        try {
          final PostTradeEventEncoder postTradeEventEncoder = (PostTradeEventEncoder) encoderField.get(this);
          OrderExtsEncoder orderExts = (OrderExtsEncoder) orderExtsField.get(postTradeEventEncoder);
          throw new NoSuchElementException("order ext encoder index "
              + orderExtEncoderIndex.get(orderExts)
              + ", count "
              + orderExtEncoderCount.get(orderExts));
        } catch (Throwable t) {
          throw nsee;
        }
      }
      throw nsee;
    }
  }
}
