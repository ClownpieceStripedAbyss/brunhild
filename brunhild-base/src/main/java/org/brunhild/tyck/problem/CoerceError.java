package org.brunhild.tyck.problem;

import org.brunhild.core.Term;
import org.brunhild.error.Problem;
import org.brunhild.error.SourcePos;
import org.brunhild.generic.Type;
import org.jetbrains.annotations.NotNull;

public record CoerceError(
  @NotNull SourcePos sourcePos,
  @NotNull String from,
  @NotNull Type<Term> to
) implements Problem {
  @Override public @NotNull Severity severity() {
    return Severity.ERROR;
  }

  @Override public @NotNull String describe() {
    return String.format("Cannot coerce %s to %s", from, to);
  }
}
