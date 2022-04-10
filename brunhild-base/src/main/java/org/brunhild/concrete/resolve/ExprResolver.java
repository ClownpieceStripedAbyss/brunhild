package org.brunhild.concrete.resolve;

import org.brunhild.concrete.Expr;
import org.brunhild.concrete.resolve.context.Context;
import org.jetbrains.annotations.NotNull;

public record ExprResolver(
  @NotNull Context context
) {
  public @NotNull Expr resolve(@NotNull Expr expr) {
    // TODO: implement
    return expr;
  }
}
