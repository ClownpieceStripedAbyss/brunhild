package org.brunhild.parser.problem;

import org.brunhild.error.Problem;
import org.brunhild.error.SourcePos;
import org.jetbrains.annotations.NotNull;

public record ConstNotInitializedProblem(
  @Override @NotNull SourcePos sourcePos,
  @NotNull String name
) implements Problem {
  @Override public @NotNull Severity severity() {
    return Severity.ERROR;
  }

  @Override public @NotNull String describe() {
    return "Const variable `" + name + "` must be initialized.";
  }
}
