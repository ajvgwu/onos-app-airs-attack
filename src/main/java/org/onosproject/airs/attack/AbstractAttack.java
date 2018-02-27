package org.onosproject.airs.attack;

import java.util.ArrayList;
import java.util.List;

import org.onosproject.airs.LogCallback;
import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;

public abstract class AbstractAttack implements Runnable {

  private final List<LogCallback> logCallbacks = new ArrayList<>();
  private final List<AttackDoneCallback> finishCallbacks = new ArrayList<>();

  private final String name;
  private final String description;
  private final int countdownSec;

  private final String logName;

  public AbstractAttack(final String name, final String description, final int countdownSec) {
    this.name = name;
    this.description = description;
    this.countdownSec = countdownSec;

    logName = MessageFormatter.format("{} ({})", getName(), getDescription()).getMessage();
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getLogName() {
    return logName;
  }

  protected abstract Logger getLog();

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

  public boolean addFinishCallback(final AttackDoneCallback c) {
    return finishCallbacks.add(c);
  }

  public boolean removeFinishCallback(final AttackDoneCallback c) {
    return finishCallbacks.remove(c);
  }

  protected abstract void runAttack();

  protected void cleanupAfterAttack() {
    logInfo("AIRS attack '{}' has nothing to clean up", getLogName());
  }

  public void handleInterrupt() {
    logInfo("AIRS attack '{}' handling interrupt, proceeding to cleanup", getLogName());
    cleanupAfterAttack();
  }

  @Override
  public void run() {
    // Countdown
    try {
      for (int i = countdownSec; i >= 1; i--) {
        logInfo("AIRS attack '{}' will execute in {} ...", getLogName(), i);
        Thread.sleep(1000);
      }
    } catch (final InterruptedException e) {
      logInfo("AIRS attack '" + getLogName() + "' was interrupted during countdown", e);
      return;
    }

    // Execute attack
    logInfo("AIRS attack '{}' is now being executed", getLogName());
    try {
      runAttack();
    } catch (final Exception e) {
      logException("AIRS attack '" + getLogName() + "' encountered an error during execution", e);
    }

    // Perform cleanup
    logInfo("AIRS attack '{}' is done, proceeding to cleanup", getLogName());
    cleanupAfterAttack();

    // Notify callback(s)
    for (final AttackDoneCallback c : finishCallbacks) {
      c.doneRunning(this);
    }
  }
}
