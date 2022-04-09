package org.brunhild.concrete;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Either;
import kala.control.Option;
import org.brunhild.error.SourcePos;
import org.jetbrains.annotations.NotNull;

public sealed interface Stmt {
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
    @NotNull ImmutableSeq<Either<Stmt, Decl>> block
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
}
