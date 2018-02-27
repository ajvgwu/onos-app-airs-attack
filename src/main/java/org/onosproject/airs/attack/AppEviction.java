package org.onosproject.airs.attack;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppEviction extends AbstractAttack {

  public static final String NAME = "AppEviction";
  public static final String DESCR = "Application eviction";

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final String appName;
  private final ComponentContext ctx;

  public AppEviction(final String appName, final ComponentContext ctx, final int countdownSec) {
    super(NAME, DESCR, countdownSec);

    this.ctx = ctx;
    this.appName = appName;
  }

  @Override
  protected Logger getLog() {
    return log;
  }

  @Override
  protected void runAttack() {
    final BundleContext bundleCtx = ctx.getBundleContext();
    final Bundle[] bundles = bundleCtx.getBundles();
    Bundle matchingBundle = null;
    for (final Bundle bundle : bundles) {
      bundle.getRegisteredServices();
      if (bundle.getSymbolicName().contains(appName)) {
        matchingBundle = bundle;
        break;
      }
    }
    if (matchingBundle != null) {
      try {
        matchingBundle.uninstall();
      } catch (final BundleException e) {
        getLog().error("could not uninstall bundle '" + matchingBundle.getSymbolicName() + "'", e);
      }
    }
    else {
      getLog().error("could not find application bundle matching '{}'", appName);
    }
  }
}
