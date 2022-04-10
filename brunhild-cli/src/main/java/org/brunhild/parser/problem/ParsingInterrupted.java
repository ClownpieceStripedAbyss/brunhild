package org.brunhild.parser.problem;

import org.brunhild.error.InterruptException;
import org.jetbrains.annotations.NotNull;

public class ParsingInterrupted extends InterruptException {
  @Override public @NotNull Stage stage() {
    return Stage.Parsing;
  }
}
