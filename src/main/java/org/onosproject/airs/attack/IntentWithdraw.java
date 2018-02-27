package org.onosproject.airs.attack;

import java.util.Iterator;

import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntentWithdraw extends AbstractAttack {

  public static final String NAME = "IntentWithdraw";
  public static final String DESCR = "Intent withdrawal";

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final IntentService intentService;

  public IntentWithdraw(final IntentService intentService, final int countdownSec) {
    super(NAME, DESCR, countdownSec);

    this.intentService = intentService;
  }

  @Override
  protected Logger getLog() {
    return log;
  }

  @Override
  protected void runAttack() {
    logInfo("will try to withdraw all intents");
    int numIntents = 0;

    // Iterate over intents
    final Iterable<Intent> intents = intentService.getIntents();
    final Iterator<Intent> intentIter = intents.iterator();
    while (intentIter.hasNext()) {
      final Intent intent = intentIter.next();
      numIntents++;

      intentService.withdraw(intent);
      // TODO: can we purge them later ???
      // intentService.purge(intent);
    }

    // Done.
    logInfo("withdrew {} intents", numIntents);
  }
}
