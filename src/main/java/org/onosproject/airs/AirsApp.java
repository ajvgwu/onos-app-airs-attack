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
      if (fg) {
        executeAttack(attack, delayMs, intervalMs);
      }
      else {
        scheduleAttack(attack, delayMs, intervalMs);
      }
    }
    else {
      printAttackHelp();
    }
  }

  public void executeAttack(final AbstractAttack attack, final long delayMs, final long intervalMs) {
    // Cancel running attack
    cancelAttackIfRunning();
    runningAttack = attack;

    // Add log callback(s) to attack
    for (final LogCallback c : logCallbacks) {
      runningAttack.addLogCallback(c);
    }

    // Delay
    if (delayMs > 0) {
      try {
        Thread.sleep(delayMs);
      } catch (final InterruptedException e) {
        logException("interrupted during delay sleep", e);
      }
    }

    // Execute attack
    if (intervalMs > 0) {
      boolean doRunAgain = true;
      while (doRunAgain) {
        doRunAgain = false;
        try {
          runningAttack.run();
          Thread.sleep(delayMs);
          doRunAgain = true;
        } catch (final InterruptedException e) {
          logException("interrupted during interval sleep", e);
          doRunAgain = false;
        }
      }
    }
    else {
      runningAttack.run();
    }

    // Remove log callback(s) from attack
    for (final LogCallback c : logCallbacks) {
      runningAttack.removeLogCallback(c);
    }
  }

  public void scheduleAttack(final AbstractAttack attack, final long delayMs, final long intervalMs) {
    cancelAttackIfRunning();
    runningAttack = attack;
    attackExecutor = Executors.newSingleThreadScheduledExecutor();
    if (intervalMs > 0) {
      attackTask = attackExecutor.scheduleWithFixedDelay(runningAttack, Math.max(0, delayMs), intervalMs,
        TimeUnit.MILLISECONDS);
    }
    else {
      attackTask = attackExecutor.schedule(runningAttack, delayMs, TimeUnit.MILLISECONDS);
    }
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
        if (runningAttack.isRunning()) {
          logInfo("cancelling running attack");
          runningAttack.handleInterrupt();
        }
      }
    }
    runningAttack = null;
    attackExecutor = null;
    attackTask = null;
  }

  public void printAttackHelp() {
    logInfo("possible attacks: ");
    for (final String name : getAttackNames()) {
      logInfo("  - {}", name);
    }
  }

  // TODO: implement all relevant DELTA attacks as subclasses of airs.attack.AbstractAttack

  // TODO: implement attack that flushes Intents ???

  public static List<String> getAttackNames() {
    return Arrays.asList(DummyPrint.NAME, SysCmdExec.NAME, InfiniteLoop.NAME, FlowTableClear.NAME, AppEviction.NAME);
  }
}
