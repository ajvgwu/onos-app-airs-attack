package org.onosproject.airs;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Dictionary;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

/**
 * WORK-IN-PROGRESS: ONOS application for AIRS testbed.
 */
@Component(immediate = true)
public class AirsApp {

  private static final long PROPDEFAULT_ATTACKINTERVALMS = 30000;
  private static final boolean PROPDEFAULT_ATTACKSYSCMDEXEC = false;

  private final Logger log = getLogger(getClass());

  @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
  protected CoreService coreService;

  @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
  protected ComponentConfigService cfgService;

  @Property(name = "attackIntervalMs", longValue = PROPDEFAULT_ATTACKINTERVALMS, label = "Period of attack loop")
  private long attackIntervalMs = PROPDEFAULT_ATTACKINTERVALMS;

  @Property(name = "attackSysCmdExec", boolValue = PROPDEFAULT_ATTACKSYSCMDEXEC,
    label = "Whether to perform AIRS attack 'System Command Execution'")
  private boolean attackSysCmdExec = PROPDEFAULT_ATTACKSYSCMDEXEC;

  private ApplicationId appId;

  private ScheduledExecutorService executor;

  public AirsApp() {
    appId = null;
    executor = null;
  }

  @Activate
  public void activate(final ComponentContext context) {
    cfgService.registerProperties(getClass());
    modified(context);
    appId = coreService.registerApplication(getClass().getPackage().getName());
    log.info("Starting appId={}", appId.id());
    startLoop();
  }

  @Deactivate
  public void deactivate(final ComponentContext context) {
    cfgService.unregisterProperties(getClass(), false);
    log.info("Stopping appId={}", appId != null ? appId.id() : appId);
    appId = null;
    stopLoop();
  }

  @Modified
  public void modified(final ComponentContext context) {
    stopLoop();

    final Dictionary<?, ?> properties = context.getProperties();

    String value = Tools.get(properties, "attackIntervalMs");
    if (value != null) {
      value = value.trim();
    }
    attackIntervalMs = value == null || value.length() < 1 ? PROPDEFAULT_ATTACKINTERVALMS : Long.parseLong(value);

    value = Tools.get(properties, "attackSysCmdExec");
    if (value != null) {
      value = value.trim();
    }
    attackSysCmdExec = value == null || value.length() < 1 ? PROPDEFAULT_ATTACKSYSCMDEXEC : Boolean.parseBoolean(value);

    if (getAppId() != null && getAttackIntervalMs() > 0) {
      startLoop();
    }
  }

  protected long getAttackIntervalMs() {
    return attackIntervalMs;
  }

  protected boolean isAttackSysCmdExec() {
    return attackSysCmdExec;
  }

  protected ApplicationId getAppId() {
    return appId;
  }

  protected void startLoop() {
    stopLoop();
    executor = Executors.newSingleThreadScheduledExecutor();
    executor.scheduleWithFixedDelay(new AttackTasks(), getAttackIntervalMs(), getAttackIntervalMs(),
      TimeUnit.MILLISECONDS);
  }

  protected void stopLoop() {
    if (executor != null) {
      executor.shutdownNow();
    }
    executor = null;
  }

  protected void runAttackSysCmdExec() {
    for (int i = 5; i >= 0; i++) {
      log.info("AIRS attack 'System Command Execution': will call System.exit(0) in {} ...", i);
      try {
        Thread.sleep(1000);
      } catch (final InterruptedException e) {
        log.error("could not sleep", e);
      }
    }
    log.warn("AIRS attack 'System Command Execution' is now being executed");
    System.exit(0);
  }

  // TODO: implement all DELTA attacks

  protected class AttackTasks implements Runnable {

    private int runCount;

    public AttackTasks() {
      runCount = 0;
    }

    public int getRunCount() {
      return runCount;
    }

    @Override
    public void run() {
      if (getAppId() != null && getAttackIntervalMs() > 0) {
        runAttackSysCmdExec();
        // TODO: run all DELTA attacks
        runCount++;
      }
    }
  }
}
