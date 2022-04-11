package org.brunhild.core.ops;

import kala.control.Either;
import kala.function.FloatFunction;
import kala.tuple.Unit;
import org.brunhild.core.Term;
import org.brunhild.generic.Type;
import org.brunhild.tyck.Gamma;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntFunction;

public record TermFold(
  @NotNull Gamma.ConstGamma gamma
) implements TermOps<Unit> {
  @Override public @NotNull Term traverse(@NotNull Term term, Unit unit) {
    return switch (term) {
      case Term.RefTerm ref -> gamma.getOption(ref.var()).map(v -> traverse(v, unit)).getOrDefault(ref);
      case Term.UnaryTerm unary -> switch (unary.op()) {
        case POS -> tryFold(unary.term(), i -> litInt(+i), f -> litFloat(+f));
        case NEG -> tryFold(unary.term(), i -> litInt(-i), f -> litFloat(-f));
        case LOGICAL_NOT -> tryFold(unary.term(), i -> litInt(i == 0 ? 1 : 0), TermFold::litFloat);
      };
      // TODO: flatten binary ops
      case Term.BinaryTerm bin -> switch (bin.op()) {
        case ADD -> tryFoldBin(bin.lhs(), bin.rhs(), (l, r) -> litInt(l + r), (l, r) -> litFloat(l + r));
        case SUB -> tryFoldBin(bin.lhs(), bin.rhs(), (l, r) -> litInt(l - r), (l, r) -> litFloat(l - r));
        case MUL -> tryFoldBin(bin.lhs(), bin.rhs(), (l, r) -> litInt(l * r), (l, r) -> litFloat(l * r));
        case DIV -> tryFoldBin(bin.lhs(), bin.rhs(), (l, r) -> litInt(l / r), (l, r) -> litFloat(l / r));
        case MOD -> tryFoldBin(bin.lhs(), bin.rhs(), (l, r) -> litInt(l % r), this::tyckerBug);
        case EQ -> tryFoldBin(bin.lhs(), bin.rhs(), (l, r) -> litInt(l == r ? 1 : 0), this::tyckerBug);
        case NE -> tryFoldBin(bin.lhs(), bin.rhs(), (l, r) -> litInt(l != r ? 1 : 0), this::tyckerBug);
        case LT -> tryFoldBin(bin.lhs(), bin.rhs(), (l, r) -> litInt(l < r ? 1 : 0), this::tyckerBug);
        case LE -> tryFoldBin(bin.lhs(), bin.rhs(), (l, r) -> litInt(l <= r ? 1 : 0), this::tyckerBug);
        case GT -> tryFoldBin(bin.lhs(), bin.rhs(), (l, r) -> litInt(l > r ? 1 : 0), this::tyckerBug);
        case GE -> tryFoldBin(bin.lhs(), bin.rhs(), (l, r) -> litInt(l >= r ? 1 : 0), this::tyckerBug);
        case LOGICAL_AND ->
          tryFoldBin(bin.lhs(), bin.rhs(), (l, r) -> litInt(l == 1 && r == 1 ? 1 : 0), this::tyckerBug);
        case LOGICAL_OR ->
          tryFoldBin(bin.lhs(), bin.rhs(), (l, r) -> litInt(l == 1 || r == 1 ? 1 : 0), this::tyckerBug);
      };
      case Term.IndexTerm indexTerm -> {
        var array = traverse(indexTerm.term(), unit);
        if (!(array instanceof Term.ArrayTerm arrayTerm))
          yield new Term.IndexTerm(array, traverse(indexTerm.index(), unit));
        yield tryFold(indexTerm.index(), i -> {
          if (arrayTerm instanceof Term.UninitializedArray) return defaultValueOf(arrayTerm.type().elementType());
          if (arrayTerm instanceof Term.InitializedArray arr) {
            // TODO: what if we are targeting JVM? We should throw an exception in that case.
            if (i < 0 || i >= arr.values().size()) return defaultValueOf(arrayTerm.type().elementType());
            else return arr.values().get(i);
          } else throw new IllegalStateException("unreachable");
        });
      }
      case Term.CoerceTerm c -> tryFold(c.term(),
        i -> {
          if (c.toType() instanceof Type.Float<Term>) return litFloat((float) i);
          return litInt(i);
        },
        f -> {
          if (c.toType() instanceof Type.Int<Term>) return litInt((int) f);
          return litFloat(f);
        });
      default -> TermOps.super.traverse(term, unit);
    };
  }

  public static @NotNull Term litInt(int i) {
    return new Term.LitTerm(Either.left(Either.left(i)));
  }

  public static @NotNull Term litFloat(float f) {
    return new Term.LitTerm(Either.left(Either.right(f)));
  }

  private @NotNull Term tryFold(
    @NotNull Term term,
    @NotNull IntFunction<Term> foldI,
    @NotNull FloatFunction<Term> foldF
  ) {
    var folded = traverse(term, Unit.unit());
    if (folded instanceof Term.LitTerm lit) {
      var literal = lit.literal();
      if (literal.isRight()) return lit;
      var left = literal.getLeftValue();
      if (left.isLeft()) return foldI.apply(left.getLeftValue());
      else return foldF.apply(left.getRightValue());
    }
    return folded;
  }

  private @NotNull Term tryFold(
    @NotNull Term term,
    @NotNull IntFunction<Term> fold
  ) {
    return tryFold(term, fold, this::tyckerBug);
  }

  private <R, T> R tyckerBug(T t) {
    throw new IllegalStateException("type checker bug?");
  }

  private <R, T1, T2> R tyckerBug(T1 t1, T2 t2) {
    throw new IllegalStateException("type checker bug?");
  }

  private @NotNull Term tryFoldBin(
    @NotNull Term lhs,
    @NotNull Term rhs,
    @NotNull IntIntBiFunction<Term> foldInt,
    @NotNull FloatFloatBiFunction<Term> foldFloat
  ) {
    return tryFold(lhs,
      li -> tryFold(rhs, ri -> foldInt.apply(li, ri), this::tyckerBug),
      lf -> tryFold(rhs, this::tyckerBug, rf -> foldFloat.apply(lf, rf)));
  }

  public static @NotNull Term defaultValueOf(@NotNull Type<Term> type) {
    return switch (type) {
      case Type.Int<Term> ignored -> litInt(0);
      case Type.Float ignored -> litFloat(0.0f);
      case Type.Array<Term> array -> new Term.UninitializedArray(array);
      default -> throw new IllegalStateException("no default value for type " + type);
    };
  }

  @FunctionalInterface
  public interface IntIntBiFunction<R> {
    R apply(int i1, int i2);
  }

  @FunctionalInterface
  public interface FloatFloatBiFunction<R> {
    R apply(float i1, float i2);
  }
}
