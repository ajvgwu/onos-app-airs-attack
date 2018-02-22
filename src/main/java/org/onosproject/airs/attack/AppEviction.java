package org.onosproject.airs.attack;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppEviction extends AbstractAttack {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final String appName;
  private final ComponentContext ctx;

  public AppEviction(final String appName, final ComponentContext ctx, final int countdownSec) {
    super("Application Eviction", countdownSec);

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
    for (final Bundle bundle : bundles) {
      bundle.getRegisteredServices();
      if (bundle.getSymbolicName().contains(appName)) {
        try {
          bundle.uninstall();
        } catch (final BundleException e) {
          getLog().error("could not uninstall bundle '" + bundle.getSymbolicName() + "'", e);
        }
      }
    }
  }
}
