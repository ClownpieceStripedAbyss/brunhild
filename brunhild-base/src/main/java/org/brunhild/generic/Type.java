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

  record Univ<Term>() implements Type<Term> {
    @Override public @NotNull java.lang.String toString() {
      return "Type";
    }
  }

  record Void<Term>() implements Type<Term> {
    @Override public @NotNull java.lang.String toString() {
      return "void";
    }
  }

  record Int<Term>() implements Type<Term> {
    @Override public @NotNull Type<Term> coerced() {
      return new Type.Float<>();
    }

    @Override public @NotNull java.lang.String toString() {
      return "i32";
    }
  }

  record Float<Term>() implements Type<Term> {
    @Override public @NotNull Type<Term> coerced() {
      return new Type.Int<>();
    }

    @Override public @NotNull java.lang.String toString() {
      return "f32";
    }
  }

  record String<Term>() implements Type<Term> {
    @Override public @NotNull java.lang.String toString() {
      return "String";
    }
  }

  record Const<Term>(@NotNull Type<Term> type) implements Type<Term> {
    @Override public @NotNull Type<Term> mkConst() {
      return this;
    }

    @Override public @NotNull java.lang.String toString() {
      return "const " + type;
    }
  }

  record Array<Term>(@NotNull Type<Term> elementType, @NotNull Dimension dimension) implements Type<Term> {
    @Override public @NotNull java.lang.String toString() {
      return java.lang.String.format("Array<%s, %s>", elementType, dimension);
    }
  }

  record Fn<Term>(
    @NotNull ImmutableSeq<Type<Term>> paramTypes,
    @NotNull Type<Term> returnType
  ) implements Type<Term> {
    @Override public @NotNull java.lang.String toString() {
      return java.lang.String.format("(%s) -> %s", paramTypes.joinToString(", "), returnType);
    }
  }

  sealed interface Dimension {}
  record DimInferred() implements Dimension {
    @Override public @NotNull java.lang.String toString() {
      return "?";
    }
  }
  record DimConst(int dimension) implements Dimension {
    @Override public @NotNull java.lang.String toString() {
      return java.lang.String.valueOf(dimension);
    }
  }
  record DimExpr<Term>(@NotNull Term term) implements Dimension {
    @Override public @NotNull java.lang.String toString() {
      return term.toString();
    }
  }
}
