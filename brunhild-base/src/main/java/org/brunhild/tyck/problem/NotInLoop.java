package org.brunhild.tyck.problem;

import org.brunhild.error.Problem;
import org.brunhild.error.SourcePos;
import org.jetbrains.annotations.NotNull;

public record NotInLoop(
  @Override @NotNull SourcePos sourcePos,
  @NotNull String action
) implements Problem {
  @Override public @NotNull Severity severity() {
    return Severity.ERROR;
  }

  @Override public @NotNull String describe() {
    return "Trying to " + action + " outside of a loop";
  }
}
