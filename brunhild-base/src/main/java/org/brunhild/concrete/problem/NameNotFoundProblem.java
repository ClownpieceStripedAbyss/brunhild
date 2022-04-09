package org.brunhild.concrete.problem;

import org.brunhild.error.Problem;
import org.brunhild.error.SourcePos;
import org.jetbrains.annotations.NotNull;

public record NameNotFoundProblem(
  @Override @NotNull SourcePos sourcePos,
  @NotNull String name
) implements Problem {
  @Override public @NotNull Severity severity() {
    return Severity.ERROR;
  }

  @Override public @NotNull String describe() {
    return "The name `" + name + "` was not found in scope.";
  }
}
