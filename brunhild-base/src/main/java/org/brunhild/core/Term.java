package org.brunhild.core;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Either;
import org.brunhild.concrete.Decl;
import org.brunhild.concrete.Expr;
import org.brunhild.core.ops.TermFold;
import org.brunhild.generic.DefVar;
import org.brunhild.generic.LocalVar;
import org.brunhild.generic.Type;
import org.brunhild.generic.Var;
import org.brunhild.tyck.Gamma;
import org.jetbrains.annotations.NotNull;

public sealed interface Term {
  @NotNull Type<Term> type();

  sealed interface CallTerm extends Term {}
  sealed interface ArrayTerm extends Term {
    @Override @NotNull Type.Array<Term> type();
  }

  record RefTerm(
    @Override @NotNull Type<Term> type,
    @NotNull Var var
  ) implements Term {
    @Override public @NotNull String toString() {
      return var.name();
    }
  }

  record IndexTerm(
    @NotNull Term term,
    @NotNull Term index
  ) implements Term {
    @Override public @NotNull Type<Term> type() {
      var arrayType = term.type();
      assert arrayType instanceof Type.Array<Term> : "type checker bug?";
      return ((Type.Array<Term>) arrayType).elementType();
    }

    @Override public @NotNull String toString() {
      return String.format("%s[%s]", term, index);
    }
  }

  record FnCall(
    @NotNull DefVar<Def.FnDef, Decl.FnDecl> fn,
    @NotNull ImmutableSeq<Term> args
  ) implements CallTerm {
    @Override public @NotNull Type<Term> type() {
      return fn.core.result();
    }

    @Override public @NotNull String toString() {
      return String.format("%s(%s)", fn.core.ref.name(), args.joinToString(", "));
    }
  }

  record PrimCall(
    @NotNull DefVar<Def.PrimDef, ?> prim,
    @NotNull ImmutableSeq<Term> args
  ) implements CallTerm {
    @Override public @NotNull Type<Term> type() {
      return prim.core.result();
    }

    @Override public @NotNull String toString() {
      return String.format("%s(%s)", prim.core.ref.name(), args.joinToString(", "));
    }
  }

  record LitTerm(
    Either<Either<Integer, Float>, String> literal
  ) implements Term {
    @Override public @NotNull Type<Term> type() {
      if (literal.isRight()) return new Type.String<>();
      if (literal.getLeftValue().isLeft()) return new Type.Int<>();
      return new Type.Float<>();
    }

    @Override public @NotNull String toString() {
      return literal.fold(l -> l.fold(String::valueOf, String::valueOf), r -> String.format("\"%s\"", r));
    }
  }

  record InitializedArray(
    @Override @NotNull Type.Array<Term> type,
    @NotNull ImmutableSeq<Term> values
  ) implements ArrayTerm {
    @Override public @NotNull String toString() {
      return String.format("{%s}", values.joinToString(", "));
    }
  }

  record UninitializedArray(
    @Override @NotNull Type.Array<Term> type
  ) implements ArrayTerm {
    @Override public @NotNull String toString() {
      return "{<uninitialized>}";
    }
  }

  record BinaryTerm(
    @NotNull Expr.BinOP op,
    @NotNull Term lhs,
    @NotNull Term rhs
  ) implements Term {
    @Override public @NotNull Type<Term> type() {
      assert lhs.type().equals(rhs.type()) : "type checker bug?";
      return lhs.type();
    }

    @Override public @NotNull String toString() {
      return String.format("(%s %s %s)", lhs, op.symbol, rhs);
    }
  }

  record UnaryTerm(
    @NotNull Expr.UnaryOP op,
    @NotNull Term term
  ) implements Term {
    @Override public @NotNull Type<Term> type() {
      return term().type();
    }

    @Override public String toString() {
      return String.format("(%s %s)", op.symbol, term);
    }
  }

  record CoerceTerm(
    @NotNull Term term,
    @NotNull Type<Term> fromType,
    @NotNull Type<Term> toType
  ) implements Term {
    @Override public @NotNull Type<Term> type() {
      return toType;
    }

    @Override public @NotNull String toString() {
      return String.format("(%s as %s)", term, toType);
    }
  }

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

    @Override public String toString() {
      return String.format("%s: %s", ref.name(), type);
    }
  }

  default @NotNull Term fold(@NotNull Gamma.ConstGamma gamma) {
    return new TermFold.DefaultFold().traverse(this, gamma);
  }
}
