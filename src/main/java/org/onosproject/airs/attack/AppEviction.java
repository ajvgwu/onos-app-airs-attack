package org.onosproject.airs.attack;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

public class AppEviction extends AbstractAttack {

  private final String appName;
  private final ComponentContext ctx;

  public AppEviction(final String appName, final ComponentContext ctx, final Logger log, final int countdownSec) {
    super("Application Eviction", log, countdownSec);

    this.ctx = ctx;
    this.appName = appName;
  }

  @Override
  protected void checkPreConditions() throws IllegalArgumentException {
    if (appName == null) {
      throw new IllegalArgumentException("application to evict is " + String.valueOf(appName));
    }
  }

  @Override
  protected void runAttack() throws RuntimeException {
    final BundleContext bundleCtx = ctx.getBundleContext();
    final Bundle[] bundles = bundleCtx.getBundles();
    for (final Bundle bundle : bundles) {
      bundle.getRegisteredServices();
      if (bundle.getSymbolicName().contains(appName)) {
        try {
          bundle.uninstall();
        } catch (final BundleException e) {
          throw new RuntimeException(
            "while evicting app '" + appName + "', could not uninstall bundle '" + bundle.getSymbolicName() + "'", e);
        }
      }
    }
  }
}
