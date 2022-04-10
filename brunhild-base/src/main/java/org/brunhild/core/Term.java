package org.brunhild.core;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Either;
import org.brunhild.concrete.Expr;
import org.brunhild.generic.DefVar;
import org.brunhild.generic.LocalVar;
import org.brunhild.generic.Type;
import org.brunhild.generic.Var;
import org.jetbrains.annotations.NotNull;

public interface Term {
  record RefTerm(
    @NotNull Var var
  ) implements Term {}

  record IndexTerm(
    @NotNull Term term,
    @NotNull Term index
  ) implements Term {}

  record AppTerm(
    @NotNull Term fn,
    @NotNull ImmutableSeq<Term> args
  ) implements Term {}

  record PrimAppTerm(
    @NotNull DefVar<Def.PrimDef, ?> prim,
    @NotNull ImmutableSeq<Term> args
  ) implements Term {}

  record LitTerm(
    Either<Integer, Float> literal
  ) implements Term {}

  record ArrayTerm(
    @NotNull ImmutableSeq<Term> values
  ) implements Term {}

  record BinaryTerm(
    @NotNull Expr.BinOP op,
    @NotNull Term lhs,
    @NotNull Term rhs
  ) implements Term {}

  record UnaryTerm(
    @NotNull Expr.UnaryOP op,
    @NotNull Term term
  ) implements Term {}

  record CoerceTerm(
    @NotNull Term term,
    @NotNull Type<Term> targetType
  ) implements Term {}

  record Param(
    @NotNull LocalVar ref,
    @NotNull Type<Term> type
  ) {
    public Param(@NotNull Term.Param param, @NotNull Type<Term> type) {
      this(param.ref, type);
    }
  }
}
