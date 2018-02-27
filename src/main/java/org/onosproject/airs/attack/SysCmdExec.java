package org.onosproject.airs.attack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SysCmdExec extends AbstractAttack {

  public static final String NAME = "SysCmdExec";
  public static final String DESCR = "System command execution";

  private final Logger log = LoggerFactory.getLogger(getClass());

  public SysCmdExec(final int countdownSec) {
    super(NAME, DESCR, countdownSec);
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
