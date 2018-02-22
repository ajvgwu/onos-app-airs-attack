package org.onosproject.airs;

import java.util.Optional;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;

@Command(scope = "onos", name = "airs-exec-attack", description = "execute an AIRS attack")
public class AirsExecAttackCmd extends AbstractShellCommand {

  private static final String DEFAULT_DELAYMS = "1000";
  private static final String DEFAULT_INTERVALMS = "0";
  private static final String DEFAULT_COUNTDOWNSEC = "3";

  @Argument(index = 0, name = "attackName", description = "name of the attack (omit to cancel any running attack)")
  private final String attackNameStr = null;

  @Option(name = "--delay-ms", description = "start the attack (any any countdown) after the given delay",
    valueToShowInHelp = DEFAULT_DELAYMS)
  private String delayMsStr = DEFAULT_DELAYMS;

  @Option(name = "--interval-ms", description = "repeat the attack at the given interval (0 for a single execution)",
    valueToShowInHelp = DEFAULT_INTERVALMS)
  private String intervalMsStr = DEFAULT_INTERVALMS;

  @Option(name = "--countdown-sec",
    description = "countdown the given number of seconds before the attack (0 for no countdown)",
    valueToShowInHelp = DEFAULT_COUNTDOWNSEC)
  private String countdownSecStr = DEFAULT_COUNTDOWNSEC;

  @Option(name = "--param1", description = "extra information required for the specific attack")
  private String param1Str = null;

  @Option(name = "--param2", description = "extra information required for the specific attack")
  private String param2Str = null;

  @Override
  protected void execute() {
    final AirsApp airsApp = AbstractShellCommand.get(AirsApp.class);

    delayMsStr = Optional.ofNullable(delayMsStr).orElse(DEFAULT_DELAYMS);
    intervalMsStr = Optional.ofNullable(intervalMsStr).orElse(DEFAULT_INTERVALMS);
    countdownSecStr = Optional.ofNullable(countdownSecStr).orElse(DEFAULT_COUNTDOWNSEC);
    param1Str = Optional.ofNullable(param1Str).orElse(null);
    param2Str = Optional.ofNullable(param2Str).orElse(null);

    airsApp.setAttackDelayMs(Long.parseLong(delayMsStr));
    airsApp.setAttackIntervalMs(Long.parseLong(intervalMsStr));
    airsApp.setAttackCountdownSec(Integer.parseInt(countdownSecStr));
    airsApp.executeAttackByName(attackNameStr, param1Str, param2Str);
  }
}
