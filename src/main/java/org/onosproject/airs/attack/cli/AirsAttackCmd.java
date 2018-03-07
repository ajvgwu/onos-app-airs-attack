package org.onosproject.airs.attack.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.airs.attack.AirsAttackApp;
import org.onosproject.airs.attack.LogCallback;
import org.onosproject.cli.AbstractChoicesCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.slf4j.helpers.MessageFormatter;

@Command(scope = "onos", name = "airs-attack", description = "AIRS attack commands")
public class AirsAttackCmd extends AbstractShellCommand implements LogCallback {

  // Color codes for ONOS CLI
  public static final String ONOSCLI_COLOR_BOLD = "\u001B[1m";
  public static final String ONOSCLI_COLOR_ERROR = "\u001B[31m";
  public static final String ONOSCLI_COLOR_RESET = "\u001B[0m";

  // Commands
  public static final String CMD_EXEC = "exec";
  public static final String CMD_CANCEL = "cancel";
  public static final String CMD_HELP = "help";

  // Default argument/option values
  private static final String DEFAULT_CMD = CMD_HELP;
  private static final String DEFAULT_DELAYMS = "1000";
  private static final String DEFAULT_INTERVALMS = "0";
  private static final String DEFAULT_COUNTDOWNSEC = "3";

  // Arguments/Options
  @Argument(index = 0, name = "command", required = true,
    description = "name of the command (one of {" + ONOSCLI_COLOR_BOLD + CMD_EXEC + ONOSCLI_COLOR_RESET + "|"
      + ONOSCLI_COLOR_BOLD + CMD_CANCEL + ONOSCLI_COLOR_RESET + "|" + ONOSCLI_COLOR_BOLD + CMD_HELP
      + ONOSCLI_COLOR_RESET + "})",
    valueToShowInHelp = DEFAULT_CMD)
  private String commandStr = null;

  @Argument(index = 1, name = "attack", required = false, description = "if command=" + ONOSCLI_COLOR_BOLD + CMD_EXEC
    + ONOSCLI_COLOR_RESET + ", name of the attack to execute (any running attacks will be cancelled)")
  private String attackStr = null;

  @Argument(index = 2, name = "param...", required = false, multiValued = true,
    description = "param(s) required by some attacks")
  private List<String> paramList = new ArrayList<>();

  @Option(name = "-d", aliases = {"--delay-ms"},
    description = "start the attack (plus any additional countdown) after the given delay",
    valueToShowInHelp = DEFAULT_DELAYMS)
  private String delayMsStr = DEFAULT_DELAYMS;

  @Option(name = "-i", aliases = {"--interval-ms"},
    description = "repeat the attack at the given interval (0 for a single execution)",
    valueToShowInHelp = DEFAULT_INTERVALMS)
  private String intervalMsStr = DEFAULT_INTERVALMS;

  @Option(name = "-c", aliases = {"--countdown-sec"},
    description = "countdown the given number of seconds before the attack (0 for no countdown)",
    valueToShowInHelp = DEFAULT_COUNTDOWNSEC)
  private String countdownSecStr = DEFAULT_COUNTDOWNSEC;

  @Option(name = "-f", aliases = {"--fg"}, description = "perform attack in foreground (redirect output to CLI)")
  private boolean fg = false;

  @Override
  protected void execute() {
    final AirsAttackApp airsAttackApp = AbstractShellCommand.get(AirsAttackApp.class);

    // Add log callback
    airsAttackApp.addLogCallback(this);

    // Handle command
    commandStr = Optional.ofNullable(commandStr).orElse(CMD_HELP);
    boolean isExec = false;
    boolean isCancel = false;
    switch (commandStr) {
      case CMD_EXEC: {
        isExec = true;
        break;
      }
      case CMD_CANCEL: {
        isCancel = true;
        break;
      }
      case CMD_HELP: {
        break;
      }
      default: {
        out("{}unknown command: {}{}{}", ONOSCLI_COLOR_ERROR, ONOSCLI_COLOR_BOLD, commandStr, ONOSCLI_COLOR_RESET);
        break;
      }
    }
    if (isExec) {
      attackStr = Optional.ofNullable(attackStr).orElse(null);
      paramList = paramList != null ? paramList : new ArrayList<>();
      delayMsStr = Optional.ofNullable(delayMsStr).orElse(DEFAULT_DELAYMS);
      intervalMsStr = Optional.ofNullable(intervalMsStr).orElse(DEFAULT_INTERVALMS);
      countdownSecStr = Optional.ofNullable(countdownSecStr).orElse(DEFAULT_COUNTDOWNSEC);
      fg = !!fg;

      final String[] params = paramList != null ? paramList.toArray(new String[paramList.size()]) : new String[0];
      final long delayMs = Long.parseLong(delayMsStr);
      final long intervalMs = Long.parseLong(intervalMsStr);
      final int countdownSec = Integer.parseInt(countdownSecStr);
      airsAttackApp.executeAttackByName(attackStr, delayMs, intervalMs, countdownSec, fg, params);
    }
    else if (isCancel) {
      airsAttackApp.cancelAttackIfRunning();
    }
    else {
      airsAttackApp.printAttackHelp();
    }

    // Remove log callback
    airsAttackApp.removeLogCallback(this);
  }

  @Override
  public void out(final String format, final Object... args) {
    System.out.print(MessageFormatter.arrayFormat(format, args).getMessage());
    System.out.println();
    System.out.flush();
  }

  @Override
  public void err(final String format, final Object... args) {
    System.err.print(ONOSCLI_COLOR_ERROR);
    System.err.print(MessageFormatter.arrayFormat(format, args).getMessage());
    System.err.print(ONOSCLI_COLOR_RESET);
    System.err.println();
    System.err.flush();
  }

  @Override
  public void catching(final String msg, final Throwable t) {
    System.err.print(ONOSCLI_COLOR_ERROR);
    System.err.print(msg);
    System.err.println();
    t.printStackTrace(System.err);
    System.err.print(ONOSCLI_COLOR_RESET);
    System.err.println();
    System.err.flush();
  }

  public static List<String> getCommandNames() {
    return Arrays.asList(CMD_EXEC, CMD_CANCEL, CMD_HELP);
  }

  public static class CommandNameCompleter extends AbstractChoicesCompleter {

    @Override
    public List<String> choices() {
      return getCommandNames();
    }
  }

  public static class AttackNameCompleter extends AbstractChoicesCompleter {

    @Override
    public List<String> choices() {
      return AirsAttackApp.getAttackNames();
    }
  }
}
