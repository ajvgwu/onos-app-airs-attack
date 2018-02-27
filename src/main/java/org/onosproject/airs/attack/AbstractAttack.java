package org.onosproject.airs.attack;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;

public abstract class AbstractAttack implements Runnable {

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

  public boolean addFinishCallback(final AttackDoneCallback c) {
    return finishCallbacks.add(c);
  }

  public boolean removeFinishCallback(final AttackDoneCallback c) {
    return finishCallbacks.remove(c);
  }

  protected abstract Logger getLog();

  protected abstract void runAttack();

  protected void cleanupAfterAttack() {
    getLog().info("AIRS attack '{}' has nothing to clean up", getLogName());
  }

  public void handleInterrupt() {
    getLog().info("AIRS attack '{}' handling interrupt, proceeding to cleanup", getLogName());
    cleanupAfterAttack();
  }

  @Override
  public void run() {
    // Countdown
    try {
      for (int i = countdownSec; i >= 1; i--) {
        getLog().info("AIRS attack '{}' will execute in {} ...", getLogName(), i);
        Thread.sleep(1000);
      }
    } catch (final InterruptedException e) {
      getLog().error("AIRS attack '" + getLogName() + "' was interrupted during countdown", e);
      return;
    }

    // Execute attack
    getLog().info("AIRS attack '{}' is now being executed", getLogName());
    try {
      runAttack();
    } catch (final Exception e) {
      getLog().error("AIRS attack '" + getLogName() + "' encountered an error during execution", e);
    }

    // Perform cleanup
    getLog().info("AIRS attack '{}' is done, proceeding to cleanup", getLogName());
    cleanupAfterAttack();

    // Notify callback(s)
    for (final AttackDoneCallback c : finishCallbacks) {
      c.doneRunning(this);
    }
  }
}
