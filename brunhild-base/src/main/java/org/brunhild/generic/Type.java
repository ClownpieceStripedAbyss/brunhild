package org.brunhild.generic;

import kala.collection.immutable.ImmutableSeq;
import org.jetbrains.annotations.NotNull;

public sealed interface Type<Term> {
  default @NotNull Type<Term> coerced() {
    return this;
  }
  default @NotNull Type<Term> mkConst() {
    return new Const<>(this);
  }
  default @NotNull Type<Term> mkArray(@NotNull Dimension dimension) {
    return new Array<>(this, dimension);
  }

  record Univ<Term>() implements Type<Term> {}
  record Void<Term>() implements Type<Term> {}
  record Int<Term>() implements Type<Term> {
    @Override public @NotNull Type<Term> coerced() {
      return new Type.Float<>();
    }
  }
  record Float<Term>() implements Type<Term> {
    @Override public @NotNull Type<Term> coerced() {
      return new Type.Int<>();
    }
  }
  record String<Term>() implements Type<Term> {}

  record Const<Term>(@NotNull Type<Term> type) implements Type<Term> {
    @Override public @NotNull Type<Term> mkConst() {
      return this;
    }
  }

  record Array<Term>(@NotNull Type<Term> elementType, @NotNull Dimension dimension) implements Type<Term> {}

  record Fn<Term>(
    @NotNull ImmutableSeq<Type<Term>> paramTypes,
    @NotNull Type<Term> returnType
  ) implements Type<Term> {}

  sealed interface Dimension {}
  record DimInferred() implements Dimension {}
  record DimConst(int dimension) implements Dimension {}
  record DimExpr<Term>(@NotNull Term term) implements Dimension {}
}
