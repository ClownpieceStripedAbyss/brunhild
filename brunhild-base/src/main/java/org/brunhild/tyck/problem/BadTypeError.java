package org.brunhild.tyck.problem;

import org.brunhild.concrete.Expr;
import org.brunhild.core.Term;
import org.brunhild.error.Problem;
import org.brunhild.error.SourcePos;
import org.brunhild.generic.Type;
import org.jetbrains.annotations.NotNull;

public record BadTypeError(
  @Override @NotNull Expr expr,
  @NotNull Type<Term> actualType, @NotNull String action,
  @NotNull String thing, @NotNull String desired
) implements Problem {
  @Override public @NotNull Severity severity() {
    return Severity.ERROR;
  }

  @Override public @NotNull String describe() {
    return String.format(
      "Unable to %s the expression\n  %s\nbecause the type %s is not a %s, but instead: %s",
      action, expr, thing, desired, actualType);
  }

  public static @NotNull BadTypeError fn(@NotNull Expr expr, @NotNull Type<Term> actualType) {
    return new BadTypeError(expr, actualType, "apply", "of what you applied", "function type");
  }

  public static @NotNull BadTypeError array(@NotNull Expr expr, @NotNull Type<Term> actualType) {
    return new BadTypeError(expr, actualType, "index", "of what you indexed", "array type");
  }

  @Override public @NotNull SourcePos sourcePos() {
    return expr.sourcePos();
  }
}
