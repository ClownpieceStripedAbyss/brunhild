package org.brunhild.core;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Either;
import org.brunhild.concrete.Decl;
import org.brunhild.concrete.Expr;
import org.brunhild.generic.DefVar;
import org.brunhild.generic.LocalVar;
import org.brunhild.generic.Type;
import org.brunhild.generic.Var;
import org.jetbrains.annotations.NotNull;

public sealed interface Term {
  sealed interface LValueTerm extends Term {}
  sealed interface CallTerm extends Term {}
  sealed interface ArrayTerm extends Term {}

  record RefTerm(
    @NotNull Var var
  ) implements LValueTerm {}

  record IndexTerm(
    @NotNull Term term,
    @NotNull Term index
  ) implements LValueTerm {}

  record FnCall(
    @NotNull DefVar<Def.FnDef, Decl.FnDecl> fn,
    @NotNull ImmutableSeq<Term> args
  ) implements CallTerm {}

  record PrimCall(
    @NotNull DefVar<Def.PrimDef, ?> prim,
    @NotNull ImmutableSeq<Term> args
  ) implements CallTerm {}

  record LitTerm(
    Either<Either<Integer, Float>, String> literal
  ) implements Term {}

  record InitializedArray(
    @NotNull ImmutableSeq<Term> values
  ) implements ArrayTerm {}

  record UninitializedArray(
    @NotNull Type.Array<Term> format
  ) implements ArrayTerm {}

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
  ) implements LValueTerm {}

  record Param(
    @NotNull LocalVar ref,
    @NotNull Type<Term> type
  ) {
    public Param(@NotNull Term.Param param, @NotNull Type<Term> type) {
      this(param.ref, type);
    }

    public Param(@NotNull Expr.Param param, @NotNull Type<Term> type) {
      this(param.ref(), type);
    }
  }
}
