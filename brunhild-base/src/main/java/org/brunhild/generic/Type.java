package org.brunhild.generic;

import org.brunhild.concrete.Expr;
import org.jetbrains.annotations.NotNull;

public sealed interface Type {
  default @NotNull Type coerced() {
    return this;
  }

  record Void() implements Type {}
  record Int() implements Type {}
  record Float() implements Type {}
  record Boolean() implements Type {
    @Override public @NotNull Type coerced() {
      return new Int();
    }
  }

  record Const(@NotNull Type type) implements Type {}

  record Array(@NotNull Type elementType, @NotNull Dimension dimension) implements Type {}

  sealed interface Dimension {}
  record DimInferred() implements Dimension {}
  record DimConst(int dimension) implements Dimension {}
  record DimExpr(@NotNull Expr expr) implements Dimension {}
}
