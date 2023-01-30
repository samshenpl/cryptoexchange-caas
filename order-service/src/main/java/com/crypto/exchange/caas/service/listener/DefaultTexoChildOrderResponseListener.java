package com.crypto.exchange.caas.service.listener;

import com.crypto.exchange.oms.common.aeron.codec.ITexoPostTradeDecoder;
import com.crypto.exchange.oms.common.aeron.listener.TexoChildOrderResponseListener;
import com.crypto.exchange.oms.common.aeron.model.AeronFill;
import com.crypto.exchange.oms.common.aeron.response.ChildOrderResponse;
import java.util.Iterator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.agrona.collections.MutableLong;
import org.agrona.concurrent.UnsafeBuffer;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DefaultTexoChildOrderResponseListener extends TexoChildOrderResponseListener {

  private final ITexoPostTradeDecoder postTradeDecoder;

  public DefaultTexoChildOrderResponseListener(ITexoPostTradeDecoder postTradeDecoder) {
    super(postTradeDecoder);
    this.postTradeDecoder = postTradeDecoder;
  }

  @Override
  public void onMessage(Integer shardId, UnsafeBuffer unsafeBuffer, MutableLong sequence) {
    if (log.isDebugEnabled()) {
      try {
        log.debug("texochildorder message from {}, {}:{}", shardId, unsafeBuffer.capacity(), sequence);
        long channelSequence = sequence.get();
        List<ChildOrderResponse> childOrderResponseList = this.postTradeDecoder.decodePostTradeEvent(unsafeBuffer, channelSequence);
        Iterator var8 = childOrderResponseList.iterator();
        log.debug("texochildOrderResponseList size: {}", childOrderResponseList.size());

        while(var8.hasNext()) {
          ChildOrderResponse childOrderResponse = (ChildOrderResponse)var8.next();
          List<AeronFill> aeronFills = childOrderResponse.getFills();
          log.debug("texochildOrderResponseList id/ aeronFills size:: {}/ {}", childOrderResponse.getAeronOrder().getOrderId(), aeronFills.size());
        }
      } catch (Throwable t) {
        log.warn("failed to debug {}, {}:{}", shardId, unsafeBuffer, sequence, t);
      }
    }
    super.onMessage(shardId, unsafeBuffer, sequence);
  }
}
