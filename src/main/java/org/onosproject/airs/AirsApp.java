package org.onosproject.airs;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.airs.attack.AbstractAttack;
import org.onosproject.airs.attack.AppEviction;
import org.onosproject.airs.attack.DummyPrint;
import org.onosproject.airs.attack.FlowTableClear;
import org.onosproject.airs.attack.InfiniteLoop;
import org.onosproject.airs.attack.SysCmdExec;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.packet.PacketService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WORK-IN-PROGRESS: ONOS application for AIRS testbed.
 */
@Component(immediate = true)
@Service(value = AirsApp.class)
public class AirsApp {

  private final Logger log = LoggerFactory.getLogger(getClass());

  @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
  protected CoreService coreService;

  @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
  protected PacketService packetService;

  @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
  protected DeviceService deviceService;

  @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
  protected FlowRuleService flowRuleService;

  private ApplicationId appId;
  private ComponentContext componentContext;

  private long attackDelayMs;
  private long attackIntervalMs;
  private int attackCountdownSec;
  private ScheduledExecutorService attackExecutor;
  private ScheduledFuture<?> attackTask;

  public AirsApp() {
    appId = null;
    componentContext = null;

    attackDelayMs = 1000;
    attackIntervalMs = 0;
    attackCountdownSec = 3;
    attackExecutor = null;
    attackTask = null;
  }

  protected Logger getLog() {
    return log;
  }

  @Activate
  public void activate(final ComponentContext context) {
    appId = coreService.registerApplication(getClass().getPackage().getName());
    final Short id = appId != null ? appId.id() : null;

    componentContext = context;

    getLog().info("started appId={}", id);
  }

  @Deactivate
  public void deactivate(final ComponentContext context) {
    cancelAttackIfRunning();

    final Short id = appId != null ? appId.id() : null;
    appId = null;

    componentContext = null;

    getLog().info("stopped appId={}", id);
  }

  protected ApplicationId getAppId() {
    return appId;
  }

  protected ComponentContext getComponentContext() {
    return componentContext;
  }

  public long getAttackDelayMs() {
    return attackDelayMs;
  }

  public void setAttackDelayMs(final long attackDelayMs) {
    this.attackDelayMs = attackDelayMs;
  }

  public long getAttackIntervalMs() {
    return attackIntervalMs;
  }

  public void setAttackIntervalMs(final long attackIntervalMs) {
    this.attackIntervalMs = attackIntervalMs;
  }

  public int getAttackCountdownSec() {
    return attackCountdownSec;
  }

  public void setAttackCountdownSec(final int attackCountdownSec) {
    this.attackCountdownSec = attackCountdownSec;
  }

  protected void cancelAttackIfRunning() {
    if (attackTask != null && !attackTask.isDone()) {
      attackTask.cancel(false);
    }
    if (attackExecutor != null) {
      getLog().info("cancelling running attack");
      attackExecutor.shutdownNow();
    }
    attackExecutor = null;
    attackTask = null;
  }

  /**
   * Cancel any running attack and then execute the given attack
   *
   * @param attackName name of the attack ({@code null} to cancel any running attack without executing a new attack)
   * @param params extra information required for the specific attack
   */
  protected void executeAttackByName(final String attackName, final String... params) {
    cancelAttackIfRunning();
    if (attackName != null) {
      attackExecutor = Executors.newSingleThreadScheduledExecutor();
      AbstractAttack attack = null;
      switch (attackName) {
        case "DummyPrint": {
          attack = new DummyPrint(getAttackCountdownSec());
          break;
        }
        case "SysCmdExec": {
          attack = new SysCmdExec(getAttackCountdownSec());
          break;
        }
        case "InfiniteLoop": {
          attack = new InfiniteLoop(getAppId(), packetService, getAttackCountdownSec());
          break;
        }
        case "FlowTableClear": {
          attack = new FlowTableClear(deviceService, flowRuleService, getAttackCountdownSec());
          break;
        }
        case "AppEviction": {
          if (params != null && params.length > 0) {
            attack = new AppEviction(params[0], getComponentContext(), getAttackCountdownSec());
          }
          else {
            getLog().error("attack '{}' requires 1 parameter: appName", attackName);
          }
          break;
        }
        // TODO: add case block for all subclasses of airs.attack.AbstractAttack
        default: {
          getLog().error("unknown attack name: {}", attackName);
        }
      }
      if (attack != null) {
        final long delayMs = Math.max(getAttackDelayMs(), 0);
        if (getAttackIntervalMs() > 0) {
          attackTask = attackExecutor.scheduleWithFixedDelay(attack, delayMs, getAttackIntervalMs(),
            TimeUnit.MILLISECONDS);
        }
        else {
          attackTask = attackExecutor.schedule(attack, delayMs, TimeUnit.MILLISECONDS);
        }
      }
    }
  }

  // TODO: implement all relevant DELTA attacks as subclasses of airs.attack.AbstractAttack
}
