package org.brunhild.tyck;

import kala.control.Either;
import kala.control.Option;
import org.brunhild.concrete.Decl;
import org.brunhild.concrete.Expr;
import org.brunhild.core.Def;
import org.brunhild.core.Term;
import org.brunhild.error.InterruptException;
import org.brunhild.error.Problem;
import org.brunhild.error.Reporter;
import org.brunhild.generic.DefVar;
import org.brunhild.generic.LocalVar;
import org.brunhild.generic.Type;
import org.brunhild.tyck.problem.BadTypeError;
import org.jetbrains.annotations.NotNull;

public record ExprTycker(
  @NotNull Reporter reporter,
  @NotNull Gamma gamma
) {
  public @NotNull ExprResult infer(@NotNull Expr expr) {
    return switch (expr) {
      case Expr.LitIntExpr lit -> new ExprResult(new Term.LitTerm(Either.left(lit.value())), new Type.Int<>());
      case Expr.LitFloatExpr lit -> new ExprResult(new Term.LitTerm(Either.right(lit.value())), new Type.Float<>());
      case Expr.UnaryExpr unaryExpr -> switch (unaryExpr.op()) {
        case POS, NEG -> {
          var operand = infer(unaryExpr.expr());
          yield new ExprResult(new Term.UnaryTerm(unaryExpr.op(), operand.wellTyped), operand.type);
        }
        case LOGICAL_NOT -> {
          var operand = check(unaryExpr.expr(), new Type.Bool<>());
          yield new ExprResult(new Term.UnaryTerm(unaryExpr.op(), operand.wellTyped), operand.type);
        }
      };
      case Expr.BinaryExpr binaryExpr -> switch (binaryExpr.op()) {
        case ADD, SUB, MUL, DIV -> {
          var lhs = infer(binaryExpr.lhs());
          var rhs = infer(binaryExpr.rhs());
          var result = unifyMaybeCoerce(lhs.type, rhs.type);
          yield new ExprResult(new Term.BinaryTerm(binaryExpr.op(), lhs.wellTyped, rhs.wellTyped), result.wellTyped);
        }
        case MOD -> {
          var lhs = check(binaryExpr.lhs(), new Type.Int<>());
          var rhs = check(binaryExpr.rhs(), new Type.Int<>());
          yield new ExprResult(new Term.BinaryTerm(binaryExpr.op(), lhs.wellTyped, rhs.wellTyped), new Type.Int<>());
        }
        case LOGICAL_AND, LOGICAL_OR, EQ, NE, LT, LE, GT, GE -> {
          var lhs = check(binaryExpr.lhs(), new Type.Bool<>());
          var rhs = check(binaryExpr.rhs(), new Type.Bool<>());
          yield new ExprResult(new Term.BinaryTerm(binaryExpr.op(), lhs.wellTyped, rhs.wellTyped), new Type.Bool<>());
        }
      };
      case Expr.AppExpr appExpr -> {
        var fnRes = infer(appExpr.fn());
        if (!(fnRes.type instanceof Type.Fn<Term> fnType)) yield fail(BadTypeError.fn(appExpr.fn(), fnRes.type));
        var fn = fnRes.wellTyped;
        var args = appExpr.args();
        var argsElab = args.zipView(fnType.paramTypes()).map(t -> check(t._1, t._2).wellTyped);
        yield new ExprResult(new Term.AppTerm(fn, argsElab.toImmutableSeq()), fnType.returnType());
      }
      case Expr.IndexExpr indexExpr -> {
        var array = infer(indexExpr.expr());
        if (!(array.type instanceof Type.Array<Term> arrayType))
          yield fail(BadTypeError.array(indexExpr.expr(), array.type));
        var index = check(indexExpr.index(), new Type.Int<>()).wellTyped;
        yield new ExprResult(new Term.IndexTerm(array.wellTyped, index), arrayType.elementType());
      }
      case Expr.RefExpr ref -> switch (ref.resolved()) {
        case LocalVar var -> {
          var ty = gamma.get(var);
          yield new ExprResult(new Term.RefTerm(var), ty);
        }
        case DefVar<?, ?> defVar -> switch (defVar.concrete) {
          case Decl.FnDecl fnDecl -> {
            var signature = fnDecl.signature;
            assert signature != null : "we need dependency graph now!";
            var tele = signature.tele().map(Term.Param::type);
            yield new ExprResult(new Term.RefTerm(fnDecl.ref), new Type.Fn<>(tele, signature.result()));
          }
          case Decl.VarDecl varDecl -> {
            var signature = varDecl.signature;
            assert signature != null : "we need dependency graph now!";
            yield new ExprResult(new Term.RefTerm(varDecl.ref), signature.result());
          }
          case null -> {
            // we are referencing primitives
            var prim = ((Def.PrimDef) defVar.core);
            yield new ExprResult(new Term.RefTerm(prim.ref), prim.result);
          }
          case default -> throw new IllegalStateException("Unknown concrete: " + defVar.concrete.getClass());
        };
        default -> throw new IllegalStateException("Unknown var: " + ref.resolved().getClass());
      };
      default -> throw new IllegalStateException("no rule");
    };
  }

  public @NotNull ExprResult check(@NotNull Expr expr, @NotNull Type<Term> type) {
    throw new UnsupportedOperationException("not implemented");
  }

  public @NotNull ExprResult check(@NotNull Option<Expr> expr, @NotNull Type<Term> type) {
    throw new UnsupportedOperationException("not implemented");
  }

  private @NotNull TypeResult unifyMaybeCoerce(@NotNull Type<Term> lhs, @NotNull Type<Term> rhs) {
    throw new UnsupportedOperationException("not implemented");
  }

  private <T> T fail(@NotNull Problem problem) {
    reporter.report(problem);
    throw new TyckInterrupted();
  }

  public @NotNull TypeResult infer(@NotNull Type<Expr> expr) {
    return switch (expr) {
      case Type.Void ignored -> new TypeResult(new Type.Void<>(), new Type.Univ<>());
      case Type.Float ignored -> new TypeResult(new Type.Float<>(), new Type.Univ<>());
      case Type.Int ignored -> new TypeResult(new Type.Int<>(), new Type.Univ<>());
      case Type.Bool ignored -> new TypeResult(new Type.Bool<>(), new Type.Univ<>());
      case Type.Const<Expr> constType -> new TypeResult(infer(constType.type()).wellTyped(), new Type.Univ<>());
      case Type.Array<Expr> arrayType -> {
        var elem = infer(arrayType.elementType()).wellTyped();
        var dim = switch (arrayType.dimension()) {
          case Type.DimInferred ignored -> new Type.DimInferred();
          case Type.DimConst dimConst -> new Type.DimConst(dimConst.dimension());
          case Type.DimExpr dimExpr ->
            new Type.DimExpr<>(check((Expr) dimExpr.term(), new Type.Int<Term>().mkConst()).wellTyped());
        };
        yield new TypeResult(new Type.Array<>(elem, dim), new Type.Univ<>());
      }
      case Type.Fn<Expr> fn -> new TypeResult(new Type.Fn<>(
        fn.paramTypes().map(p -> infer(p).wellTyped),
        infer(fn.returnType()).wellTyped),
        new Type.Univ<>());
      case Type.Univ ignored -> throw new IllegalStateException("what do you think of this type system?");
    };
  }

  public record ExprResult(@NotNull Term wellTyped, @NotNull Type<Term> type) {}
  public record TypeResult(@NotNull Type<Term> wellTyped, @NotNull Type<Term> type) {}

  public static class TyckInterrupted extends InterruptException {
    @Override public @NotNull Stage stage() {
      return Stage.Tycking;
    }
  }
}
