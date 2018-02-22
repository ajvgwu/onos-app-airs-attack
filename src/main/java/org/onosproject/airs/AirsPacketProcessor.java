package org.onosproject.airs;

import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;

public class AirsPacketProcessor implements PacketProcessor {

  private Callback callback;

  public AirsPacketProcessor() {
    callback = null;
  }

  public Callback getCallback() {
    return callback;
  }

  public void setCallback(final Callback callback) {
    this.callback = callback;
  }

  @Override
  public void process(final PacketContext ctx) {
    if (callback != null) {
      callback.processPacket(ctx);
    }
  }

  public static interface Callback {
    public void processPacket(final PacketContext ctx);
  }
}
