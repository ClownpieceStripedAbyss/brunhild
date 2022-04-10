package org.brunhild.parser.problem;

import org.brunhild.error.InterruptedException;
import org.jetbrains.annotations.NotNull;

public class ParsingInterrupted extends InterruptedException {
  @Override public @NotNull Stage stage() {
    return Stage.Parsing;
  }
}
