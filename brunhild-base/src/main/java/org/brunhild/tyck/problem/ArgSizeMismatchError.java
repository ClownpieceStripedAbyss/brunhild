package org.brunhild.tyck.problem;

import org.brunhild.error.Problem;
import org.brunhild.error.SourcePos;
import org.jetbrains.annotations.NotNull;

public record ArgSizeMismatchError(
  @Override @NotNull SourcePos sourcePos,
  int expected,
  int actual
) implements Problem {
  @Override public @NotNull Severity severity() {
    return Severity.ERROR;
  }

  @Override public @NotNull String describe() {
    return String.format("Argument size mismatch: expected: %d, provided: %d", expected, actual);
  }
}
