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
import org.onosproject.airs.attack.AppEviction;
import org.onosproject.airs.attack.SysCmdExec;
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
  private static final int PROPDEFAULT_ATTACKCOUNTDOWNSEC = 3;
  private static final boolean PROPDEFAULT_ATTACKSYSCMDEXEC = false;
  private static final boolean PROPDEFAULT_ATTACKAPPEVICTION = false;
  private static final String PROPDEFAULT_ATTACKAPPEVICTIONNAME = "fwd";

  private final Logger log = getLogger(getClass());

  @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
  protected CoreService coreService;

  @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
  protected ComponentConfigService cfgService;

  @Property(name = "attackIntervalMs", longValue = PROPDEFAULT_ATTACKINTERVALMS, label = "Period of attack loop")
  private long attackIntervalMs = PROPDEFAULT_ATTACKINTERVALMS;

  @Property(name = "attackCountdownSec", intValue = PROPDEFAULT_ATTACKCOUNTDOWNSEC,
    label = "Number of seconds to countdown before an attack")
  private int attackCountdownSec = PROPDEFAULT_ATTACKCOUNTDOWNSEC;

  @Property(name = "attackSysCmdExec", boolValue = PROPDEFAULT_ATTACKSYSCMDEXEC,
    label = "Whether to perform AIRS attack 'System Command Execution'")
  private boolean attackSysCmdExec = PROPDEFAULT_ATTACKSYSCMDEXEC;

  @Property(name = "attackAppEviction", boolValue = PROPDEFAULT_ATTACKAPPEVICTION,
    label = "Whether to perform AIRS attack 'App Eviction'")
  private boolean attackAppEviction = PROPDEFAULT_ATTACKAPPEVICTION;

  @Property(name = "attackAppEvictionName", value = PROPDEFAULT_ATTACKAPPEVICTIONNAME,
    label = "Name of app to evict for AIRS attack 'App Eviction'")
  private String attackAppEvictionName = PROPDEFAULT_ATTACKAPPEVICTIONNAME;

  private ApplicationId appId;
  private ComponentContext contextRef;
  private ScheduledExecutorService executor;

  public AirsApp() {
    appId = null;
    contextRef = null;
    executor = null;
  }

  @Activate
  public void activate(final ComponentContext context) {
    cfgService.registerProperties(getClass());
    modified(context);

    appId = coreService.registerApplication(getClass().getPackage().getName());
    final Short id = appId != null ? appId.id() : null;

    contextRef = context;

    startLoop();

    log.info("Started appId={}", id);
  }

  @Deactivate
  public void deactivate(final ComponentContext context) {
    cfgService.unregisterProperties(getClass(), false);

    final Short id = appId != null ? appId.id() : null;
    appId = null;

    contextRef = null;

    stopLoop();

    log.info("Stopped appId={}", id);
  }

  @Modified
  public void modified(final ComponentContext context) {
    stopLoop();

    final Dictionary<?, ?> properties = context.getProperties();

    attackIntervalMs = getPropAsLong(properties, "attackIntervalMs", PROPDEFAULT_ATTACKINTERVALMS);
    attackCountdownSec = getPropAsInt(properties, "attackCountdownSec", PROPDEFAULT_ATTACKCOUNTDOWNSEC);
    attackSysCmdExec = getPropAsBoolean(properties, "attackSysCmdExec", PROPDEFAULT_ATTACKSYSCMDEXEC);
    attackAppEviction = getPropAsBoolean(properties, "attackAppEviction", PROPDEFAULT_ATTACKAPPEVICTION);
    attackAppEvictionName = getPropAsString(properties, "attackAppEvictionName", PROPDEFAULT_ATTACKAPPEVICTIONNAME);

    if (getAppId() != null && getAttackIntervalMs() > 0) {
      startLoop();
    }
  }

  protected Logger getLog() {
    return log;
  }

  protected long getAttackIntervalMs() {
    return attackIntervalMs;
  }

  protected int getAttackCountdownSec() {
    return attackCountdownSec;
  }

  protected boolean isAttackSysCmdExec() {
    return attackSysCmdExec;
  }

  protected boolean isAttackAppEviction() {
    return attackAppEviction;
  }

  protected String getAttackAppEvictionName() {
    return attackAppEvictionName;
  }

  protected ApplicationId getAppId() {
    return appId;
  }

  protected ComponentContext getContextRef() {
    return contextRef;
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

  // TODO: implement all relevant DELTA attacks as subclasses of airs.attack.AbstractAttack

  protected static long getPropAsLong(final Dictionary<?, ?> properties, final String name, final long propDefault) {
    String value = Tools.get(properties, name);
    if (value != null) {
      value = value.trim();
    }
    return value == null || value.length() < 1 ? propDefault : Long.parseLong(value);
  }

  protected static int getPropAsInt(final Dictionary<?, ?> properties, final String name, final int propDefault) {
    String value = Tools.get(properties, name);
    if (value != null) {
      value = value.trim();
    }
    return value == null || value.length() < 1 ? propDefault : Integer.parseInt(value);
  }

  protected static boolean getPropAsBoolean(final Dictionary<?, ?> properties, final String name,
    final boolean propDefault) {
    String value = Tools.get(properties, name);
    if (value != null) {
      value = value.trim();
    }
    return value == null || value.length() < 1 ? propDefault : Boolean.parseBoolean(value);
  }

  protected static String getPropAsString(final Dictionary<?, ?> properties, final String name,
    final String propDefault) {
    String value = Tools.get(properties, name);
    if (value != null) {
      value = value.trim();
    }
    return value == null || value.length() < 1 ? propDefault : value;
  }

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
        if (isAttackSysCmdExec()) {
          new SysCmdExec(getLog(), getAttackCountdownSec()).run();
        }
        if (isAttackAppEviction()) {
          new AppEviction(getAttackAppEvictionName(), getContextRef(), getLog(), getAttackCountdownSec()).run();
        }
        // TODO: run all implemented and enabled DELTA attacks
        runCount++;
      }
    }
  }
}
