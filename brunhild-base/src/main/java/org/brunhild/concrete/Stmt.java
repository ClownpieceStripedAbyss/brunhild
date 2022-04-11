package org.brunhild.concrete;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Option;
import org.brunhild.concrete.resolve.StmtResolver;
import org.brunhild.concrete.resolve.StmtShallowResolver;
import org.brunhild.concrete.resolve.context.ModuleContext;
import org.brunhild.core.Proclaim;
import org.brunhild.error.Reporter;
import org.brunhild.error.SourcePos;
import org.brunhild.tyck.ExprTycker;
import org.brunhild.tyck.Gamma;
import org.brunhild.tyck.StmtTycker;
import org.jetbrains.annotations.NotNull;

public sealed interface Stmt permits Decl, Stmt.AssignStmt, Stmt.BlockStmt, Stmt.BreakStmt,
  Stmt.ContinueStmt, Stmt.ExprStmt, Stmt.IfStmt, Stmt.ReturnStmt, Stmt.WhileStmt {
  @NotNull SourcePos sourcePos();

  record AssignStmt(
    @Override @NotNull SourcePos sourcePos,
    @NotNull Expr lvalue,
    @NotNull Expr rvalue
  ) implements Stmt {}

  record ExprStmt(
    @NotNull Expr expr
  ) implements Stmt {
    @Override public @NotNull SourcePos sourcePos() {
      return expr.sourcePos();
    }
  }

  record BlockStmt(
    @Override @NotNull SourcePos sourcePos,
    @NotNull ImmutableSeq<Stmt> block
  ) implements Stmt {}

  record IfStmt(
    @Override @NotNull SourcePos sourcePos,
    @NotNull Expr cond,
    @NotNull Stmt thenBranch,
    @NotNull Option<Stmt> elseBranch
  ) implements Stmt {}

  record WhileStmt(
    @Override @NotNull SourcePos sourcePos,
    @NotNull Expr cond,
    @NotNull Stmt body
  ) implements Stmt {}

  record ReturnStmt(
    @Override @NotNull SourcePos sourcePos,
    @NotNull Option<Expr> expr
  ) implements Stmt {}

  record BreakStmt(
    @Override @NotNull SourcePos sourcePos
  ) implements Stmt {}

  record ContinueStmt(
    @Override @NotNull SourcePos sourcePos
  ) implements Stmt {}

  static @NotNull ImmutableSeq<Stmt> resolve(@NotNull ImmutableSeq<Stmt> stmts, @NotNull ModuleContext context) {
    StmtShallowResolver.resolveStmts(stmts, context);
    return StmtResolver.resolveStmts(stmts);
  }

  static @NotNull ImmutableSeq<Proclaim> tyck(@NotNull Reporter reporter, @NotNull ImmutableSeq<Stmt> resolved) {
    var stmtTycker = new StmtTycker(reporter);
    var rootTypeGamma = new Gamma.TypeGamma();
    var rootConstGamma = new Gamma.ConstGamma();
    var exprTycker = new ExprTycker(reporter, rootTypeGamma, rootConstGamma);
    return stmtTycker.tyckStmts(resolved, exprTycker);
  }
}
