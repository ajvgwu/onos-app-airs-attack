package org.onosproject.airs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import org.onosproject.airs.attack.AttackDoneCallback;
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
public class AirsApp implements AttackDoneCallback {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final List<LogCallback> logCallbacks = new ArrayList<>();

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

  private AbstractAttack runningAttack;
  private ScheduledExecutorService attackExecutor;
  private ScheduledFuture<?> attackTask;

  public AirsApp() {
    appId = null;
    componentContext = null;

    runningAttack = null;
    attackExecutor = null;
    attackTask = null;
  }

  protected Logger getLog() {
    return log;
  }

  public boolean addLogCallback(final LogCallback c) {
    return logCallbacks.add(c);
  }

  public boolean removeLogCallback(final LogCallback c) {
    return logCallbacks.remove(c);
  }

  protected void logInfo(final String format, final Object... args) {
    getLog().info(format, args);
    for (final LogCallback c : logCallbacks) {
      c.out(format, args);
    }
  }

  protected void logError(final String format, final Object... args) {
    getLog().error(format, args);
    for (final LogCallback c : logCallbacks) {
      c.err(format, args);
    }
  }

  protected void logException(final String msg, final Throwable t) {
    getLog().error(msg, t);
    for (final LogCallback c : logCallbacks) {
      c.catching(msg, t);
    }
  }

  @Activate
  public void activate(final ComponentContext context) {
    appId = coreService.registerApplication(getClass().getPackage().getName());
    final Short id = appId != null ? appId.id() : null;

    componentContext = context;

    logInfo("started appId={}", id);
  }

  @Deactivate
  public void deactivate(final ComponentContext context) {
    cancelAttackIfRunning();

    final Short id = appId != null ? appId.id() : null;
    appId = null;

    componentContext = null;

    logInfo("stopped appId={}", id);
  }

  protected ApplicationId getAppId() {
    return appId;
  }

  protected ComponentContext getComponentContext() {
    return componentContext;
  }

  public void cancelAttackIfRunning() {
    if (attackExecutor != null && !attackExecutor.isShutdown()) {
      attackExecutor.shutdown();
    }
    if (attackTask != null && !attackTask.isDone()) {
      logInfo("cancelling attack task");
      attackTask.cancel(false);
      if (!attackTask.isDone()) {
        attackTask.cancel(true);
      }
      if (runningAttack != null) {
        logInfo("cancelling running attack");
        runningAttack.handleInterrupt();
      }
    }
    runningAttack = null;
    attackExecutor = null;
    attackTask = null;
  }

  protected void scheduleAttack(final AbstractAttack attack, final long delayMs, final long intervalMs,
    final boolean fg) {
    cancelAttackIfRunning();
    runningAttack = attack;
    if (fg) {
      for (final LogCallback c : logCallbacks) {
        runningAttack.addLogCallback(c);
      }
      runningAttack.run();
      for (final LogCallback c : logCallbacks) {
        runningAttack.removeLogCallback(c);
      }
    }
    else {
      attackExecutor = Executors.newSingleThreadScheduledExecutor();
      if (intervalMs > 0) {
        attackTask = attackExecutor.scheduleWithFixedDelay(runningAttack, Math.max(0, delayMs), intervalMs,
          TimeUnit.MILLISECONDS);
      }
      else {
        attackTask = attackExecutor.schedule(runningAttack, delayMs, TimeUnit.MILLISECONDS);
      }
    }
  }

  public void printAttackHelp() {
    logInfo("possible attacks: ");
    for (final String name : getAttackNames()) {
      logInfo("  - {}", name);
    }
  }

  /**
   * Cancel any running attack and then execute the given attack
   *
   * @param attackName name of the attack ({@code null} to cancel any running attack without executing a new attack)
   * @param params extra information required for the specific attack
   */
  public void executeAttackByName(final String attackName, final long delayMs, final long intervalMs,
    final int countdownSec, final boolean fg, final String... params) {
    AbstractAttack attack = null;
    if (attackName != null) {
      switch (attackName) {
        case DummyPrint.NAME: {
          attack = new DummyPrint(countdownSec);
          break;
        }
        case SysCmdExec.NAME: {
          attack = new SysCmdExec(countdownSec);
          break;
        }
        case InfiniteLoop.NAME: {
          attack = new InfiniteLoop(getAppId(), packetService, countdownSec);
          break;
        }
        case FlowTableClear.NAME: {
          attack = new FlowTableClear(deviceService, flowRuleService, countdownSec);
          break;
        }
        case AppEviction.NAME: {
          if (params != null && params.length > 0 && params[0] != null) {
            attack = new AppEviction(params[0], getComponentContext(), countdownSec);
          }
          else {
            logError("attack '{}' requires 1 parameter: appName", attackName);
          }
          break;
        }
        // TODO: add case block for all subclasses of airs.attack.AbstractAttack
        default: {
          logError("unknown attack name: {}", attackName);
          break;
        }
      }
    }
    if (attack != null) {
      scheduleAttack(attack, delayMs, intervalMs, fg);
    }
    else {
      printAttackHelp();
    }
  }

  @Override
  public void doneRunning(final AbstractAttack attack) {
    if (attack != null && attack.equals(runningAttack)) {
      runningAttack = null;
    }
  }

  // TODO: implement all relevant DELTA attacks as subclasses of airs.attack.AbstractAttack

  // TODO: implement attack that flushes Intents ???

  public static List<String> getAttackNames() {
    return Arrays.asList(DummyPrint.NAME, SysCmdExec.NAME, InfiniteLoop.NAME, FlowTableClear.NAME, AppEviction.NAME);
  }
}
