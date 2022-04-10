package org.brunhild.concrete;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Either;
import kala.control.Option;
import org.brunhild.concrete.resolve.StmtResolver;
import org.brunhild.concrete.resolve.StmtShallowResolver;
import org.brunhild.concrete.resolve.context.Context;
import org.brunhild.concrete.resolve.context.ModuleContext;
import org.brunhild.error.SourcePos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
}
