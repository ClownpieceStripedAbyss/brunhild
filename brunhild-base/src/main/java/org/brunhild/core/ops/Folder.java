package org.brunhild.core.ops;

import kala.control.Either;
import kala.function.FloatFunction;
import kala.tuple.Unit;
import org.brunhild.core.Term;
import org.brunhild.generic.Type;
import org.brunhild.tyck.Gamma;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntFunction;

public record Folder(
  @NotNull Gamma.ConstGamma gamma
) implements TermOps<Unit> {
  @Override public @NotNull Term traverse(@NotNull Term term, Unit unit) {
    return switch (term) {
      case Term.RefTerm ref -> gamma.getOption(ref.var()).map(v -> traverse(v, unit)).getOrDefault(ref);
      case Term.UnaryTerm unary -> switch (unary.op()) {
        case POS -> tryFold(unary.term(), i -> litInt(+i), f -> litFloat(+f));
        case NEG -> tryFold(unary.term(), i -> litInt(-i), f -> litFloat(-f));
        case LOGICAL_NOT -> tryFold(unary.term(), i -> litInt(i == 0 ? 1 : 0), Folder::litFloat);
      };
      // TODO: flatten binary ops
      case Term.IndexTerm indexTerm -> {
        var array = traverse(indexTerm.term(), unit);
        if (!(array instanceof Term.ArrayTerm arrayTerm))
          yield new Term.IndexTerm(array, traverse(indexTerm.index(), unit));
        yield tryFold(indexTerm.index(), i -> {
          if (arrayTerm instanceof Term.UninitializedArray) return defaultValueOf(arrayTerm.type().elementType());
          if (arrayTerm instanceof Term.InitializedArray arr) {
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

  private static @NotNull Term litInt(int i) {
    return new Term.LitTerm(Either.left(Either.left(i)));
  }

  private static @NotNull Term litFloat(float f) {
    return new Term.LitTerm(Either.left(Either.right(f)));
  }

  private @NotNull Term tryFold(
    @NotNull Term term,
    @NotNull IntFunction<Term> foldInt,
    @NotNull FloatFunction<Term> foldFloat
  ) {
    var folded = traverse(term, Unit.unit());
    if (folded instanceof Term.LitTerm lit) {
      var literal = lit.literal();
      if (literal.isRight()) return lit;
      var left = literal.getLeftValue();
      if (left.isLeft()) return foldInt.apply(left.getLeftValue());
      else return foldFloat.apply(left.getRightValue());
    }
    return folded;
  }

  private @NotNull Term tryFold(
    @NotNull Term term,
    @NotNull IntFunction<Term> fold
  ) {
    return tryFold(term, fold, f -> {
      throw new IllegalStateException("type checker bug?");
    });
  }

  public static @NotNull Term defaultValueOf(@NotNull Type<Term> type) {
    return switch (type) {
      case Type.Int<Term> ignored -> litInt(0);
      case Type.Float ignored -> litFloat(0.0f);
      case Type.Array<Term> array -> new Term.UninitializedArray(array);
      default -> throw new IllegalStateException("no default value for type " + type);
    };
  }
}
