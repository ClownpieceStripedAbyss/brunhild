package org.brunhild.core.ops;

import kala.control.Either;
import kala.function.FloatFunction;
import org.brunhild.core.Term;
import org.brunhild.generic.Type;
import org.brunhild.tyck.Gamma;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.IntFunction;

public interface TermFold extends TermOps<Gamma.ConstGamma> {
  default @NotNull Term traverse(@NotNull Term term, @NotNull Gamma.ConstGamma gamma) {
    return switch (term) {
      case Term.RefTerm ref -> gamma.getOption(ref.var()).map(v -> traverse(v, gamma)).getOrDefault(ref);
      case Term.UnaryTerm unary -> switch (unary.op()) {
        case POS -> tryFold(gamma, unary.term(),
          t -> new Term.UnaryTerm(unary.op(), t),
          i -> litInt(+i),
          f -> litFloat(+f));
        case NEG -> tryFold(gamma, unary.term(),
          t -> new Term.UnaryTerm(unary.op(), t),
          i -> litInt(-i),
          f -> litFloat(-f));
        case LOGICAL_NOT -> tryFold(gamma, unary.term(),
          t -> new Term.UnaryTerm(unary.op(), t),
          i -> litInt(i == 0 ? 1 : 0),
          this::tyckerBug);
      };
      // TODO: flatten binary ops
      case Term.BinaryTerm bin -> switch (bin.op()) {
        case ADD -> tryFoldBin(gamma, bin, (l, r) -> litInt(l + r), (l, r) -> litFloat(l + r));
        case SUB -> tryFoldBin(gamma, bin, (l, r) -> litInt(l - r), (l, r) -> litFloat(l - r));
        case MUL -> tryFoldBin(gamma, bin, (l, r) -> litInt(l * r), (l, r) -> litFloat(l * r));
        case DIV -> tryFoldBin(gamma, bin, (l, r) -> litInt(l / r), (l, r) -> litFloat(l / r));
        case MOD -> tryFoldBin(gamma, bin, (l, r) -> litInt(l % r), this::tyckerBug);
        case EQ -> tryFoldBin(gamma, bin, (l, r) -> litInt(l == r ? 1 : 0), this::tyckerBug);
        case NE -> tryFoldBin(gamma, bin, (l, r) -> litInt(l != r ? 1 : 0), this::tyckerBug);
        case LT -> tryFoldBin(gamma, bin, (l, r) -> litInt(l < r ? 1 : 0), this::tyckerBug);
        case LE -> tryFoldBin(gamma, bin, (l, r) -> litInt(l <= r ? 1 : 0), this::tyckerBug);
        case GT -> tryFoldBin(gamma, bin, (l, r) -> litInt(l > r ? 1 : 0), this::tyckerBug);
        case GE -> tryFoldBin(gamma, bin, (l, r) -> litInt(l >= r ? 1 : 0), this::tyckerBug);
        case LOGICAL_AND -> tryFoldBin(gamma, bin, (l, r) -> litInt(l == 1 && r == 1 ? 1 : 0), this::tyckerBug);
        case LOGICAL_OR -> tryFoldBin(gamma, bin, (l, r) -> litInt(l == 1 || r == 1 ? 1 : 0), this::tyckerBug);
      };
      case Term.IndexTerm indexTerm -> {
        var array = traverse(indexTerm.term(), gamma);
        if (!(array instanceof Term.ArrayTerm arrayTerm))
          yield new Term.IndexTerm(array, traverse(indexTerm.index(), gamma));
        yield tryFold(gamma, indexTerm.index(), i -> {
          if (arrayTerm instanceof Term.UninitializedArray) return defaultValueOf(arrayTerm.type().elementType());
          if (arrayTerm instanceof Term.InitializedArray arr) {
            // TODO: what if we are targeting JVM? We should throw an exception in that case.
            if (i < 0 || i >= arr.values().size()) return defaultValueOf(arrayTerm.type().elementType());
            else return arr.values().get(i);
          } else throw new IllegalStateException("unreachable");
        });
      }
      case Term.CoerceTerm c -> tryFold(gamma, c.term(),
        t -> new Term.CoerceTerm(t, c.fromType(), c.toType()),
        i -> {
          if (c.toType() instanceof Type.Float<Term>) return litFloat((float) i);
          return litInt(i);
        },
        f -> {
          if (c.toType() instanceof Type.Int<Term>) return litInt((int) f);
          return litFloat(f);
        });
      default -> TermOps.super.traverse(term, gamma);
    };
  }

  static @NotNull Term litInt(int i) {
    return new Term.LitTerm(Either.left(Either.left(i)));
  }

  static @NotNull Term litFloat(float f) {
    return new Term.LitTerm(Either.left(Either.right(f)));
  }

  private @NotNull Term tryFold(
    @NotNull Gamma.ConstGamma gamma,
    @NotNull Term term,
    @NotNull Function<Term, Term> restore,
    @NotNull IntFunction<Term> foldI,
    @NotNull FloatFunction<Term> foldF
  ) {
    var folded = traverse(term, gamma);
    if (folded instanceof Term.LitTerm lit) {
      var literal = lit.literal();
      if (literal.isRight()) return lit;
      var left = literal.getLeftValue();
      if (left.isLeft()) return foldI.apply(left.getLeftValue());
      else return foldF.apply(left.getRightValue());
    }
    return restore.apply(folded);
  }

  private @NotNull Term tryFold(
    @NotNull Gamma.ConstGamma gamma,
    @NotNull Term term,
    @NotNull IntFunction<Term> fold
  ) {
    return tryFold(gamma, term, t -> t, fold, this::tyckerBug);
  }

  private <R, T> R tyckerBug(T t) {
    throw new IllegalStateException("type checker bug?");
  }

  private <R, T1, T2> R tyckerBug(T1 t1, T2 t2) {
    throw new IllegalStateException("type checker bug?");
  }

  private @NotNull Term tryFoldBin(
    @NotNull Gamma.ConstGamma gamma,
    @NotNull Term.BinaryTerm bin,
    @NotNull IntIntBiFunction<Term> foldInt,
    @NotNull FloatFloatBiFunction<Term> foldFloat
  ) {
    var lhs = traverse(bin.lhs(), gamma);
    var rhs = traverse(bin.rhs(), gamma);
    if (lhs == bin.lhs() && rhs == bin.rhs()) return bin;
    if (lhs instanceof Term.LitTerm lhsLit && rhs instanceof Term.LitTerm rhsLit) {
      return tryFold(gamma, lhsLit,
        this::tyckerBug,
        li -> tryFold(gamma, rhsLit, this::tyckerBug, ri -> foldInt.apply(li, ri), this::tyckerBug),
        lf -> tryFold(gamma, rhsLit, this::tyckerBug, this::tyckerBug, rf -> foldFloat.apply(lf, rf)));
    }
    return new Term.BinaryTerm(bin.op(), lhs, rhs);
  }

  static @NotNull Term defaultValueOf(@NotNull Type<Term> type) {
    return switch (type) {
      case Type.Int<Term> ignored -> litInt(0);
      case Type.Float ignored -> litFloat(0.0f);
      case Type.Array<Term> array -> new Term.UninitializedArray(array);
      default -> throw new IllegalStateException("no default value for type " + type);
    };
  }

  @FunctionalInterface
  interface IntIntBiFunction<R> {
    R apply(int i1, int i2);
  }

  @FunctionalInterface
  interface FloatFloatBiFunction<R> {
    R apply(float i1, float i2);
  }

  class DefaultFold implements TermFold {}
}
