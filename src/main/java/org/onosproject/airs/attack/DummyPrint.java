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
    getLog().trace("dummy print attack (to TRACE)");
    getLog().debug("dummy print attack (to DEBUG)");
    getLog().info("dummy print attack (to INFO)");
    getLog().warn("dummy print attack (to WARN)");
    getLog().error("dummy print attack (to ERROR)");
    System.out.println("dummy print attack (to STDOUT)");
    System.err.println("dummy print attack (to STDERR)");
  }
}
