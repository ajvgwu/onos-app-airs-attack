package org.onosproject.airs.attack;

import org.slf4j.Logger;

public abstract class AbstractAttack implements Runnable {

  private final String name;
  private final int countdownSec;

  public AbstractAttack(final String name, final int countdownSec) {
    this.name = name;
    this.countdownSec = countdownSec;
  }

  public String getName() {
    return name;
  }

  protected abstract Logger getLog();

  protected abstract void runAttack();

  protected void cleanupAfterAttack() {
    getLog().info("AIRS attack '{}' has nothing to clean up", getName());
  }

  @Override
  public void run() {
    // Countdown
    try {
      for (int i = countdownSec; i >= 1; i--) {
        getLog().info("AIRS attack '{}' will execute in {} ...", getName(), i);
        Thread.sleep(1000);
      }
    } catch (final InterruptedException e) {
      getLog().error("AIRS attack '" + getName() + "' was interrupted during countdown", e);
      return;
    }

    // Execute attack
    getLog().info("AIRS attack '{}' is now being executed", getName());
    try {
      runAttack();
    } catch (final Exception e) {
      getLog().error("AIRS attack '" + getName() + "' encountered an error during execution", e);
    }

    // Perform cleanup
    getLog().info("AIRS attack '{}' is done, proceeding to cleanup", getName());
    cleanupAfterAttack();
  }
}
