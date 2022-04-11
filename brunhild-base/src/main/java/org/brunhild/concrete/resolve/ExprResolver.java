package org.brunhild.concrete.resolve;

import org.brunhild.concrete.Expr;
import org.brunhild.concrete.resolve.context.Context;
import org.brunhild.generic.DefVar;
import org.brunhild.generic.Var;
import org.jetbrains.annotations.NotNull;

public record ExprResolver(@NotNull Context context) {
  public @NotNull Expr resolve(@NotNull Expr expr) {
    return switch (expr) {
      case Expr.RefExpr ref -> ref;
      case Expr.LitFloatExpr lit -> lit;
      case Expr.LitIntExpr lit -> lit;
      case Expr.LitStringExpr lit -> lit;

      case Expr.AppExpr app -> {
        var fn = resolve(app.fn());
        var args = app.args().map(this::resolve);
        if (fn == app.fn() && args.sameElements(app.args())) yield app;
        yield new Expr.AppExpr(app.sourcePos(), fn, args);
      }
      case Expr.IndexExpr index -> {
        var e = resolve(index.expr());
        var i = resolve(index.index());
        if (e == index.expr() && i == index.index()) yield index;
        yield new Expr.IndexExpr(index.sourcePos(), e, i);
      }
      case Expr.UnaryExpr unary -> {
        var e = resolve(unary.expr());
        if (e == unary.expr()) yield unary;
        yield new Expr.UnaryExpr(unary.sourcePos(), unary.op(), e);
      }
      case Expr.BinaryExpr binary -> {
        var l = resolve(binary.lhs());
        var r = resolve(binary.rhs());
        if (l == binary.lhs() && r == binary.rhs()) yield binary;
        yield new Expr.BinaryExpr(binary.sourcePos(), binary.op(), l, r);
      }
      case Expr.LitArrayExpr lit -> {
        var values = lit.values().map(this::resolve);
        if (values.sameElements(lit.values())) yield lit;
        yield new Expr.LitArrayExpr(lit.sourcePos(), values);
      }
      case Expr.UnresolvedExpr unresolved -> {
        var sourcePos = unresolved.sourcePos();
        yield switch (context.get(unresolved.name(), sourcePos)) {
          // TODO: dependency graph?
          case DefVar<?, ?> ref -> new Expr.RefExpr(sourcePos, ref);
          case Var ref -> new Expr.RefExpr(sourcePos, ref);
        };
      }
    };
  }
}
