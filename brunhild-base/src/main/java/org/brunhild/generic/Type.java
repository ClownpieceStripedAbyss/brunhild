package org.brunhild.generic;

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

  record Array(@NotNull Type elementType, int dimension) implements Type {}
}
