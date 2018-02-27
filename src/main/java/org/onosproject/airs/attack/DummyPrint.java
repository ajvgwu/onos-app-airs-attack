package org.onosproject.airs.attack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyPrint extends AbstractAttack {

  public static final String NAME = "DummyPrint";
  public static final String DESCR = "Dummy print";

  private final Logger log = LoggerFactory.getLogger(getClass());

  public DummyPrint(final int countdownSec) {
    super(NAME, DESCR, countdownSec);
  }

  @Override
  protected Logger getLog() {
    return log;
  }

  @Override
  protected void runAttack() {
    logInfo("dummy print attack (to INFO)");
    logError("dummy print attack (to ERROR)");
    System.out.println("dummy print attack (to STDOUT)");
    System.err.println("dummy print attack (to STDERR)");
  }
}
