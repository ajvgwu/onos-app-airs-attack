package org.onosproject.airs.attack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SysCmdExec extends AbstractAttack {

  private final Logger log = LoggerFactory.getLogger(getClass());

  public SysCmdExec(final int countdownSec) {
    super("System Command Execution", countdownSec);
  }

  @Override
  protected Logger getLog() {
    return log;
  }

  @Override
  protected void runAttack() {
    System.exit(0);
  }
}
