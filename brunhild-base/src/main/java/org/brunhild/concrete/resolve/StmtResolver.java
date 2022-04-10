package org.brunhild.concrete.resolve;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.tuple.Tuple2;
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
        decl.body = resolveStmt(decl.body, local._2);
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

  private static @NotNull Stmt resolveStmt(@NotNull Stmt stmt, @NotNull Context context) {
    return switch (stmt) {
      case Stmt.WhileStmt whi -> {
        var cond = whi.cond().resolve(context);
        var body = resolveStmt(whi.body(), context);
        if (cond == whi.cond() && body == whi.body()) yield whi;
        yield new Stmt.WhileStmt(whi.sourcePos(), cond, body);
      }
      case Stmt.AssignStmt assign -> {
        var lhs = assign.lvalue().resolve(context);
        var rhs = assign.rvalue().resolve(context);
        if (lhs == assign.lvalue() && rhs == assign.rvalue()) yield assign;
        yield new Stmt.AssignStmt(assign.sourcePos(), lhs, rhs);
      }
      case Stmt.BlockStmt block -> {
        var ctx = context.derive(":block");
        var stmts = block.block().map(s -> resolveStmt(s, ctx));
        if (stmts.sameElements(block.block())) yield block;
        yield new Stmt.BlockStmt(block.sourcePos(), stmts);
      }
      case Stmt.IfStmt ih -> {
        var cond = ih.cond().resolve(context);
        var thenBranch = resolveStmt(ih.thenBranch(), context);
        var elseBranch = ih.elseBranch().map(s -> resolveStmt(s, context));
        if (cond == ih.cond() && thenBranch == ih.thenBranch() && elseBranch == ih.elseBranch())
          yield ih;
        yield new Stmt.IfStmt(ih.sourcePos(), cond, thenBranch, elseBranch);
      }
      case Stmt.ExprStmt expr -> {
        var e = expr.expr().resolve(context);
        if (e == expr.expr()) yield expr;
        yield new Stmt.ExprStmt(e);
      }
      case Stmt.ReturnStmt ret -> {
        var retE = ret.expr().map(e -> e.resolve(context));
        if (retE == ret.expr()) yield ret;
        yield new Stmt.ReturnStmt(ret.sourcePos(), retE);
      }
      case Stmt.ContinueStmt ignored -> ignored;
      case Stmt.BreakStmt ignored -> ignored;
      case Decl.VarDecl varDecl -> {
        // note: this should be done in shallow resolver, but that would require one more traversal of the AST
        varDecl.context = context;
        yield resolveTopLevel(varDecl);
      }
      case Decl.FnDecl ignored -> throw new IllegalStateException("Functions can only be declared in top level" + stmt);
    };
  }
}
