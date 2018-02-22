package org.onosproject.airs.attack;

import org.slf4j.Logger;

public abstract class AbstractAttack implements Runnable {

  private final String name;
  private final Logger log;
  private final int countdownSec;

  public AbstractAttack(final String name, final Logger log, final int countdownSec) {
    this.name = name;
    this.log = log;
    this.countdownSec = countdownSec;
  }

  public String getName() {
    return name;
  }

  protected Logger getLog() {
    return log;
  }

  protected abstract void checkPreConditions() throws RuntimeException;

  protected abstract void runAttack() throws RuntimeException;

  @Override
  public void run() {
    // Check pre-conditions
    try {
      checkPreConditions();
    } catch (final RuntimeException e) {
      log.warn("AIRS attack '" + name + "': skipping because of failed pre-condition: " + e.getMessage(), e);
      return;
    }

    // Countdown
    try {
      for (int i = countdownSec; i >= 1; i--) {
        log.info("AIRS attack '{}': will execute in {} ...", name, i);
        Thread.sleep(1000);
      }
    } catch (final InterruptedException e) {
      log.error("AIRS attack '" + name + "': interrupted during final countdown", e);
      return;
    }

    // Execute attack
    log.info("AIRS attack '{}': now being executed", name);
    try {
      runAttack();
    } catch (final RuntimeException e) {
      log.warn("AIRS attack '" + name + "': " + e.getMessage(), e);
    }
  }
}
