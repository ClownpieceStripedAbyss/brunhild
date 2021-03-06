package org.brunhild.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Either;
import kala.control.Option;
import org.brunhild.concrete.Decl;
import org.brunhild.concrete.Expr;
import org.brunhild.core.Def;
import org.brunhild.core.Term;
import org.brunhild.core.ops.TermFold;
import org.brunhild.error.InterruptException;
import org.brunhild.error.Problem;
import org.brunhild.error.Reporter;
import org.brunhild.error.SourcePos;
import org.brunhild.generic.DefVar;
import org.brunhild.generic.LocalVar;
import org.brunhild.generic.Type;
import org.brunhild.tyck.problem.*;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record ExprTycker(
  @NotNull Reporter reporter,
  @NotNull Gamma.TypeGamma gamma,
  @NotNull Gamma.ConstGamma constGamma
) {
  public @NotNull ExprTycker derive() {
    return new ExprTycker(reporter, gamma, constGamma);
  }

  public @NotNull Result infer(@NotNull Expr expr) {
    return switch (expr) {
      case Expr.LitIntExpr lit -> new Result(TermFold.litInt(lit.value()));
      case Expr.LitFloatExpr lit -> new Result(TermFold.litFloat(lit.value()));
      case Expr.LitStringExpr lit -> new Result(new Term.LitTerm(Either.right(lit.value())));
      case Expr.UnaryExpr unaryExpr -> switch (unaryExpr.op()) {
        case POS, NEG -> {
          var operand = infer(unaryExpr.expr());
          yield new Result(new Term.UnaryTerm(unaryExpr.op(), operand.wellTyped));
        }
        case LOGICAL_NOT -> {
          var operand = check(unaryExpr.expr(), new Type.Int<>());
          yield new Result(new Term.UnaryTerm(unaryExpr.op(), operand.wellTyped));
        }
      };
      case Expr.BinaryExpr binaryExpr -> switch (binaryExpr.op()) {
        case ADD, SUB, MUL, DIV -> {
          var lhs = infer(binaryExpr.lhs());
          var rhs = infer(binaryExpr.rhs());
          var result = unifyMaybeCoerce(binaryExpr.sourcePos(), lhs, rhs);
          yield new Result(new Term.BinaryTerm(binaryExpr.op(), result.lhs.wellTyped, result.rhs.wellTyped));
        }
        case MOD, LOGICAL_AND, LOGICAL_OR, EQ, NE, LT, LE, GT, GE -> {
          var lhs = check(binaryExpr.lhs(), new Type.Int<>());
          var rhs = check(binaryExpr.rhs(), new Type.Int<>());
          yield new Result(new Term.BinaryTerm(binaryExpr.op(), lhs.wellTyped, rhs.wellTyped));
        }
      };
      case Expr.AppExpr appExpr -> {
        var fnRes = infer(appExpr.fn());
        if (!(fnRes.type instanceof Type.Fn<Term> fnType)) yield fail(BadTypeError.fn(appExpr.fn(), fnRes.type));
        var fn = fnRes.wellTyped;
        // TODO: more cases if we support indirect call in the future (like pointers)
        var callRef = switch (fn) {
          case Term.RefTerm ref && ref.var() instanceof DefVar<?, ?> defVar -> defVar;
          default -> throw new UnsupportedOperationException("we need indirect call now!");
        };
        if (callRef.core != null && callRef.core instanceof Def.PrimDef prim) {
          yield tyckPrimCall(appExpr, fnType, prim);
        } else if (callRef.concrete instanceof Decl.FnDecl) {
          @SuppressWarnings("unchecked")
          var fnRef = (DefVar<Def.FnDef, Decl.FnDecl>) callRef;
          yield makeCall(appExpr, fnType, true, args -> new Term.FnCall(fnRef, args));
        } else {
          throw new UnsupportedOperationException("we need indirect call now!");
        }
      }
      case Expr.IndexExpr indexExpr -> {
        var array = infer(indexExpr.expr());
        if (!(array.type instanceof Type.Array<Term> arrayType))
          yield fail(BadTypeError.array(indexExpr.expr(), array.type));
        var index = check(indexExpr.index(), new Type.Int<>()).wellTyped;
        yield new Result(new Term.IndexTerm(array.wellTyped, index));
      }
      case Expr.RefExpr ref -> switch (ref.resolved()) {
        case LocalVar var -> {
          var ty = gamma.get(var);
          yield new Result(new Term.RefTerm(ty, var));
        }
        case DefVar<?, ?> defVar -> switch (defVar.concrete) {
          case Decl.FnDecl fnDecl -> {
            var signature = fnDecl.signature;
            assert signature != null : "we need dependency graph now!";
            var tele = signature.tele().map(Term.Param::type);
            var type = new Type.Fn<>(tele, signature.result());
            yield new Result(new Term.RefTerm(type, fnDecl.ref));
          }
          case Decl.VarDecl varDecl -> {
            var signature = varDecl.signature;
            assert signature != null : "we need dependency graph now!";
            yield new Result(new Term.RefTerm(signature.result(), varDecl.ref));
          }
          case null -> {
            // we are referencing primitives
            var prim = ((Def.PrimDef) defVar.core);
            var tele = prim.telescope().map(Term.Param::type);
            var type = new Type.Fn<>(tele, prim.result());
            yield new Result(new Term.RefTerm(type, prim.ref));
          }
          case default -> throw new IllegalStateException("Unknown concrete: " + defVar.concrete.getClass());
        };
        default -> throw new IllegalStateException("Unknown var: " + ref.resolved().getClass());
      };
      default -> throw new IllegalStateException("no rule");
    };
  }

  private @NotNull Result tyckPrimCall(@NotNull Expr.AppExpr appExpr, @NotNull Type.Fn<Term> fnType, @NotNull Def.PrimDef prim) {
    if (prim == Def.PrimFactory.StartTime.prim || prim == Def.PrimFactory.StopTime.prim) {
      var desugarPrim = prim == Def.PrimFactory.StartTime.prim
        ? Def.PrimFactory.StartTimeABI.prim
        : Def.PrimFactory.StopTimeABI.prim;
      var arg = TermFold.litInt(appExpr.sourcePos().startLine());
      return new Result(new Term.PrimCall(desugarPrim.ref, ImmutableSeq.of(arg)));
    } else {
      var checkArgSize = prim != Def.PrimFactory.Printf.prim;
      return makeCall(appExpr, fnType, checkArgSize, args -> new Term.PrimCall(prim.ref, args));
    }
  }

  private @NotNull Result makeCall(
    @NotNull Expr.AppExpr appExpr,
    @NotNull Type.Fn<Term> fnType,
    boolean checkArgSize,
    @NotNull Function<ImmutableSeq<Term>, Term.CallTerm> make
  ) {
    var args = appExpr.args();
    if (checkArgSize && !args.sizeEquals(fnType.paramTypes()))
      return fail(new ArgSizeMismatchError(appExpr.sourcePos(), fnType.paramTypes().size(), args.size()));
    var argsElab = args.zipView(fnType.paramTypes()).map(t -> check(t._1, t._2).wellTyped);
    return new Result(make.apply(argsElab.toImmutableSeq()));
  }

  public @NotNull Result check(@NotNull Expr expr, @NotNull Type<Term> type) {
    return switch (expr) {
      case Expr.LitArrayExpr array -> {
        if (!(type instanceof Type.Array<Term> arrayType))
          yield fail(new CoerceError(array.sourcePos(), "array type", type, "to"));
        // TODO: reorganize the array literal to fit the type
        var values = array.values().map(v -> check(v, arrayType.elementType()).wellTyped);
        yield new Result(new Term.InitializedArray(arrayType, values));
      }
      default -> {
        var infer = infer(expr);
        var result = unifyMaybeCoerce(expr.sourcePos(), infer, type);
        yield result.lhs;
      }
    };
  }

  public @NotNull Result check(@NotNull Option<Expr> expr, @NotNull Type<Term> type) {
    if (expr.isDefined()) return check(expr.get(), type);
    // fill it with a default value
    return new Result(TermFold.defaultValueOf(type));
  }

  private @NotNull UnifyResult unifyMaybeCoerce(@NotNull SourcePos sourcePos, @NotNull Result lhs, @NotNull Result rhs) {
    if (lhs.type.equals(rhs.type)) return new UnifyResult(lhs, rhs, lhs.type);
    var lhsType = lhs.type;
    var rhsType = rhs.type;
    var lhsTerm = lhs.wellTyped;
    var rhsTerm = rhs.wellTyped;
    if (lhsType instanceof Type.Int<Term> && rhsType instanceof Type.Float<Term>) {
      return new UnifyResult(new Result(new Term.CoerceTerm(lhsTerm, lhsType, rhsType)), rhs, rhsType);
    }
    if (lhsType instanceof Type.Float<Term> && rhsType instanceof Type.Int<Term>) {
      return new UnifyResult(lhs, new Result(new Term.CoerceTerm(rhsTerm, rhsType, lhsType)), lhsType);
    }
    return fail(new CoerceError(sourcePos, lhsType.toString(), rhsType, "between"));
  }

  private @NotNull UnifyResult unifyMaybeCoerce(@NotNull SourcePos sourcePos, @NotNull Result inferred, @NotNull Type<Term> against) {
    if (inferred.type.equals(against)) return new UnifyResult(inferred, inferred, against);
    var type = inferred.type;
    var term = inferred.wellTyped;
    while (!type.equals(against)) {
      var coerced = type.coerced();
      if (coerced == type) {
        // cannot coerce further
        return fail(new CoerceError(sourcePos, inferred.type.toString(), against, "to"));
      }
      term = new Term.CoerceTerm(term, type, coerced);
      type = coerced;
    }
    var coercedResult = new Result(term);
    return new UnifyResult(coercedResult, coercedResult, against);
  }

  public <T> T fail(@NotNull Problem problem) {
    reporter.report(problem);
    throw new TyckInterrupted();
  }

  public @NotNull ExprTycker.TResult infer(@NotNull Type<Expr> expr) {
    return switch (expr) {
      case Type.Void ignored -> new TResult(new Type.Void<>(), new Type.Univ<>());
      case Type.Float ignored -> new TResult(new Type.Float<>(), new Type.Univ<>());
      case Type.Int ignored -> new TResult(new Type.Int<>(), new Type.Univ<>());
      case Type.String ignored -> new TResult(new Type.String<>(), new Type.Univ<>());
      case Type.Const<Expr> constType -> new TResult(infer(constType.type()).wellTyped(), new Type.Univ<>());
      case Type.Array<Expr> arrayType -> {
        var elem = infer(arrayType.elementType()).wellTyped();
        var dim = switch (arrayType.dimension()) {
          case Type.DimInferred ignored -> new Type.DimInferred();
          case Type.DimConst dimConst -> new Type.DimConst(dimConst.dimension());
          case Type.DimExpr dimExpr -> {
            var dimE = (Expr) dimExpr.term();
            var term = check(dimE, new Type.Int<>()).wellTyped();
            var folded = term.fold(constGamma);
            // Java's type inference sucks
            if (!(folded instanceof Term.LitTerm lit))
              yield (Type.Dimension) fail(new ArraySizeIsNotConst(dimE.sourcePos()));
            if (lit.literal().isRight()) yield (Type.Dimension) fail(new ArraySizeIsNotInt(dimE.sourcePos()));
            if (lit.literal().getLeftValue().isRight())
              yield (Type.Dimension) fail(new ArraySizeIsNotInt(dimE.sourcePos()));
            yield new Type.DimConst(lit.literal().getLeftValue().getLeftValue());
          }
        };
        yield new TResult(new Type.Array<>(elem, dim), new Type.Univ<>());
      }
      case Type.Fn<Expr> fn -> new TResult(new Type.Fn<>(
        fn.paramTypes().map(p -> infer(p).wellTyped),
        infer(fn.returnType()).wellTyped),
        new Type.Univ<>());
      case Type.Univ ignored -> throw new IllegalStateException("what do you think of this type system?");
    };
  }

  public record Result(@NotNull Term wellTyped, @NotNull Type<Term> type) {
    public Result(@NotNull Term wellTyped) {
      this(wellTyped, wellTyped.type());
    }
  }
  public record TResult(@NotNull Type<Term> wellTyped, @NotNull Type<Term> type) {}
  public record UnifyResult(@NotNull Result lhs, @NotNull Result rhs, @NotNull Type<Term> type) {}

  public static class TyckInterrupted extends InterruptException {
    @Override public @NotNull Stage stage() {
      return Stage.Tycking;
    }
  }
}
