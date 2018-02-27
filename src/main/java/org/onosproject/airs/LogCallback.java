package org.onosproject.airs;

public interface LogCallback {

  public void out(final String format, final Object... args);

  public void err(final String format, final Object... args);

  public void catching(final String msg, final Throwable t);
}
