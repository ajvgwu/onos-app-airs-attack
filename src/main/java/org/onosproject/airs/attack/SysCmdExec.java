package org.onosproject.airs.attack;

import org.slf4j.Logger;

public class SysCmdExec extends AbstractAttack {

  public SysCmdExec(final Logger log, final int countdownSec) {
    super("System Command Execution", log, countdownSec);
  }

  @Override
  protected void checkPreConditions() {
    return;
  }

  @Override
  protected void runAttack() {
    System.exit(0);
  }
}
