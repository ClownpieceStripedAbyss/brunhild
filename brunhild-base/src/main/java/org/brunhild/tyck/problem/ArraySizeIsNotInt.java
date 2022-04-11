package org.brunhild.tyck.problem;

import org.brunhild.error.Problem;
import org.brunhild.error.SourcePos;
import org.jetbrains.annotations.NotNull;

public record ArraySizeIsNotInt(
  @NotNull SourcePos sourcePos
) implements Problem {
  @Override public @NotNull Severity severity() {
    return Severity.ERROR;
  }

  @Override public @NotNull String describe() {
    return "Array size is not a integer";
  }
}
