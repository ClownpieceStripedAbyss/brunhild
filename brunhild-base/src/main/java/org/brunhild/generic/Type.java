package org.brunhild.generic;

import org.jetbrains.annotations.NotNull;

public sealed interface Type<Term> {
  default @NotNull Type<Term> coerced() {
    return this;
  }

  record Void<Term>() implements Type<Term> {}
  record Int<Term>() implements Type<Term> {}
  record Float<Term>() implements Type<Term> {}

  record Const<Term>(@NotNull Type<Term> type) implements Type<Term> {}

  record Array<Term>(@NotNull Type<Term> elementType, @NotNull Dimension<Term> dimension) implements Type<Term> {}

  sealed interface Dimension<Term> {}
  record DimInferred<Term>() implements Dimension<Term> {}
  record DimConst<Term>(int dimension) implements Dimension<Term> {}
  record DimExpr<Term>(@NotNull Term term) implements Dimension<Term> {}
}
