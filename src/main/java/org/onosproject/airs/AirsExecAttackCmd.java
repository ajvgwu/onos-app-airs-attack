package org.onosproject.airs;

import java.util.Optional;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;

@Command(scope = "onos", name = "airs-exec-attack", description = "execute an AIRS attack")
public class AirsExecAttackCmd extends AbstractShellCommand implements LogCallback {

  public static final String CMD_RUN = "run";
  public static final String CMD_CANCEL = "cancel";
  public static final String CMD_HELP = "help";

  private static final String DEFAULT_CMD = CMD_HELP;
  private static final String DEFAULT_DELAYMS = "1000";
  private static final String DEFAULT_INTERVALMS = "0";
  private static final String DEFAULT_COUNTDOWNSEC = "3";
  private static final String DEFAULT_FG = "false";

  @Argument(index = 0, name = "cmd", required = true,
    description = "command (one of {" + CMD_RUN + "|" + CMD_CANCEL + "|" + CMD_HELP + "})",
    valueToShowInHelp = DEFAULT_CMD)
  private String cmdStr = null;

  @Argument(index = 1, name = "attackName", required = false,
    description = "if cmd is '" + CMD_RUN + "', name of the attack to run (any running attacks will be cancelled)")
  private String attackNameStr = null;

  @Option(name = "--delay-ms", aliases = {"-d"},
    description = "start the attack (plus any additional countdown) after the given delay",
    valueToShowInHelp = DEFAULT_DELAYMS)
  private String delayMsStr = DEFAULT_DELAYMS;

  @Option(name = "--interval-ms", aliases = {"-i"},
    description = "repeat the attack at the given interval (0 for a single execution)",
    valueToShowInHelp = DEFAULT_INTERVALMS)
  private String intervalMsStr = DEFAULT_INTERVALMS;

  @Option(name = "--countdown-sec", aliases = {"-c"},
    description = "countdown the given number of seconds before the attack (0 for no countdown)",
    valueToShowInHelp = DEFAULT_COUNTDOWNSEC)
  private String countdownSecStr = DEFAULT_COUNTDOWNSEC;

  @Option(name = "--fg", aliases = {"-f"}, description = "perform attack in foreground (redirect output to CLI)",
    valueToShowInHelp = DEFAULT_FG)
  private String fgStr = DEFAULT_FG.toString();

  @Option(name = "--param1", description = "extra information required for the specific attack")
  private String param1Str = null;

  @Option(name = "--param2", description = "extra information required for the specific attack")
  private String param2Str = null;

  @Override
  protected void execute() {
    final AirsApp airsApp = AbstractShellCommand.get(AirsApp.class);

    // Add log callback
    airsApp.addLogCallback(this);

    // Handle command
    cmdStr = Optional.ofNullable(cmdStr).orElse(CMD_HELP);
    boolean isRun = false;
    boolean isCancel = false;
    switch (cmdStr) {
      case CMD_RUN: {
        isRun = true;
        break;
      }
      case CMD_CANCEL: {
        isCancel = true;
        break;
      }
    }
    if (isRun) {
      attackNameStr = Optional.ofNullable(attackNameStr).orElse(null);
      delayMsStr = Optional.ofNullable(delayMsStr).orElse(DEFAULT_DELAYMS);
      intervalMsStr = Optional.ofNullable(intervalMsStr).orElse(DEFAULT_INTERVALMS);
      countdownSecStr = Optional.ofNullable(countdownSecStr).orElse(DEFAULT_COUNTDOWNSEC);
      fgStr = Optional.ofNullable(fgStr).orElse(DEFAULT_FG);
      param1Str = Optional.ofNullable(param1Str).orElse(null);
      param2Str = Optional.ofNullable(param2Str).orElse(null);

      final long delayMs = Long.parseLong(delayMsStr);
      final long intervalMs = Long.parseLong(intervalMsStr);
      final int countdownSec = Integer.parseInt(countdownSecStr);
      final boolean fg = Boolean.parseBoolean(fgStr);
      final String[] params = param2Str != null ? new String[] {param1Str, param2Str}
        : param1Str != null ? new String[] {param1Str} : new String[] {};
      airsApp.executeAttackByName(attackNameStr, delayMs, intervalMs, countdownSec, fg, params);
    }
    else if (isCancel) {
      airsApp.cancelAttackIfRunning();
    }
    else {
      airsApp.printAttackHelp();
    }

    // Remove log callback
    airsApp.removeLogCallback(this);
  }

  @Override
  public void out(final String format, final Object... args) {
    System.out.printf(format, args);
  }

  @Override
  public void err(final String format, final Object... args) {
    System.err.printf(format, args);
  }

  @Override
  public void catching(final String msg, final Throwable t) {
    System.err.println(msg);
    t.printStackTrace();
  }
}
