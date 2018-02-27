package org.onosproject.airs.attack;

import org.onlab.packet.Ethernet;
import org.onosproject.airs.AirsPacketProcessor;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.packet.InboundPacket;
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
    logInfo("installed packet processor");

    // Request IPv4 and ARP packets
    final TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
    selector.matchEthType(Ethernet.TYPE_IPV4);
    packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
    selector.matchEthType(Ethernet.TYPE_ARP);
    packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
    logInfo("packet processor requesting IPv4 and ARP packets");

    // Create loop thread
    final LoopThread t = new LoopThread();
    t.start();
    packetProcessor.setCallback(t);
    try {
      t.join();
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
      final InboundPacket pkt = ctx.inPacket();
      final ConnectPoint pktFrom = pkt.receivedFrom();
      final Ethernet pktParsed = pkt.parsed();
      logInfo("inbound packet from {}: {}", pktFrom, pktParsed);
      logInfo("now starting infinite loop");
      int i = 0;
      while (i < 32767) {
        i++;
        if (i == 32766) {
          i = 0;
          logInfo("still looping...");
        }
      }
    }
  }
}
