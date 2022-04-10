package org.brunhild.concrete.resolve;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.tuple.Tuple;
import kala.tuple.Tuple2;
import kala.value.Ref;
import org.brunhild.concrete.Decl;
import org.brunhild.concrete.Expr;
import org.brunhild.concrete.Stmt;
import org.brunhild.concrete.resolve.context.Context;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface StmtResolver {
  static @NotNull ImmutableSeq<Stmt> resolveStmts(@NotNull ImmutableSeq<Stmt> stmts) {
    return stmts.map(StmtResolver::resolveTopLevel);
  }

  private static @NotNull Stmt resolveTopLevel(@NotNull Stmt stmt) {
    return switch (stmt) {
      case Decl.FnDecl decl -> {
        var context = decl.context;
        assert context != null : "no shallow resolver?";
        var local = resolveParams(decl.telescope.view(), context);
        decl.telescope = local._1.toImmutableSeq();
        decl.body = resolveStmt(decl.body, local._2)._1;
        yield decl;
      }
      case Decl.VarDecl decl -> {
        var context = decl.context;
        assert context != null : "no shallow resolver?";
        decl.body = decl.body.map(e -> e.resolve(context));
        yield decl;
      }
      default -> throw new IllegalStateException("Top level cannot have statements" + stmt);
    };
  }

  @Contract(pure = true)
  private static @NotNull Tuple2<SeqView<Expr.Param>, Context>
  resolveParams(@NotNull SeqView<Expr.Param> params, @NotNull Context ctx) {
    if (params.isEmpty()) return Tuple2.of(SeqView.empty(), ctx);
    var first = params.first();
    var type = first.type();
    var newCtx = ctx.bind(first.ref());
    var result = resolveParams(params.drop(1), newCtx);
    return Tuple2.of(result._1.prepended(new Expr.Param(first, type)), result._2);
  }

  private static @NotNull Tuple2<Stmt, Context> resolveStmt(@NotNull Stmt stmt, @NotNull Context context) {
    return switch (stmt) {
      case Stmt.WhileStmt whi -> {
        var cond = whi.cond().resolve(context);
        var body = resolveStmt(whi.body(), context)._1;
        if (cond == whi.cond() && body == whi.body()) yield Tuple.of(whi, context);
        yield Tuple.of(new Stmt.WhileStmt(whi.sourcePos(), cond, body), context);
      }
      case Stmt.AssignStmt assign -> {
        var lhs = assign.lvalue().resolve(context);
        var rhs = assign.rvalue().resolve(context);
        if (lhs == assign.lvalue() && rhs == assign.rvalue()) yield Tuple.of(assign, context);
        yield Tuple.of(new Stmt.AssignStmt(assign.sourcePos(), lhs, rhs), context);
      }
      case Stmt.BlockStmt block -> {
        var ctx = new Ref<Context>(context.derive(":block"));
        var stmts = block.block().map(s -> {
          var local = resolveStmt(s, ctx.value);
          ctx.value = local._2;
          return local._1;
        });
        if (stmts.sameElements(block.block())) yield Tuple.of(block, context);
        yield Tuple.of(new Stmt.BlockStmt(block.sourcePos(), stmts), context);
      }
      case Stmt.IfStmt ih -> {
        var cond = ih.cond().resolve(context);
        var thenBranch = resolveStmt(ih.thenBranch(), context)._1;
        var elseBranch = ih.elseBranch().map(s -> resolveStmt(s, context)._1);
        if (cond == ih.cond() && thenBranch == ih.thenBranch() && elseBranch == ih.elseBranch())
          yield Tuple.of(ih, context);
        yield Tuple.of(new Stmt.IfStmt(ih.sourcePos(), cond, thenBranch, elseBranch), context);
      }
      case Stmt.ExprStmt expr -> {
        var e = expr.expr().resolve(context);
        if (e == expr.expr()) yield Tuple.of(expr, context);
        yield Tuple.of(new Stmt.ExprStmt(e), context);
      }
      case Stmt.ReturnStmt ret -> {
        var retE = ret.expr().map(e -> e.resolve(context));
        if (retE == ret.expr()) yield Tuple.of(ret, context);
        yield Tuple.of(new Stmt.ReturnStmt(ret.sourcePos(), retE), context);
      }
      case Stmt.ContinueStmt ignored -> Tuple.of(ignored, context);
      case Stmt.BreakStmt ignored -> Tuple.of(ignored, context);
      case Decl.VarDecl decl -> {
        decl.context = context;
        decl.body = decl.body.map(e -> e.resolve(context));
        var newCtx = context.bind(decl.ref);
        yield Tuple.of(decl, newCtx);
      }
      case Decl.FnDecl ignored -> throw new IllegalStateException("Functions can only be declared in top level" + stmt);
    };
  }
}
