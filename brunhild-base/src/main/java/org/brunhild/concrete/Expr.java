package org.brunhild.concrete;

import kala.collection.immutable.ImmutableSeq;
import org.brunhild.error.SourcePos;
import org.brunhild.generic.LocalVar;
import org.brunhild.generic.Type;
import org.brunhild.generic.Var;
import org.jetbrains.annotations.NotNull;

public sealed interface Expr {
  @NotNull SourcePos sourcePos();

  record RefExpr(
    @Override @NotNull SourcePos sourcePos,
    @NotNull Var resolved
  ) implements Expr {}

  record UnresolvedExpr(
    @Override @NotNull SourcePos sourcePos,
    @NotNull String name
  ) implements Expr {}

  record IndexExpr(
    @Override @NotNull SourcePos sourcePos,
    @NotNull Expr expr,
    @NotNull Expr index
  ) implements Expr {}

  record AppExpr(
    @Override @NotNull SourcePos sourcePos,
    @NotNull Expr expr,
    @NotNull ImmutableSeq<Expr> args
  ) implements Expr {}

  record BinaryExpr(
    @Override @NotNull SourcePos sourcePos,
    @NotNull BinOP op,
    @NotNull Expr lhs,
    @NotNull Expr rhs
  ) implements Expr {}

  record UnaryExpr(
    @Override @NotNull SourcePos sourcePos,
    @NotNull UnaryOP op,
    @NotNull Expr expr
  ) implements Expr {}

  record LitIntExpr(
    @Override @NotNull SourcePos sourcePos,
    int value
  ) implements Expr {}

  record LitFloatExpr(
    @Override @NotNull SourcePos sourcePos,
    float value
  ) implements Expr {}

  record LitArrayExpr(
    @Override @NotNull SourcePos sourcePos,
    @NotNull ImmutableSeq<Expr> values
  ) implements Expr {}

  record Param(
    @NotNull SourcePos sourcePos,
    @NotNull LocalVar ref,
    @NotNull Type type
  ) {
    public Param(@NotNull Param param, @NotNull Type type) {
      this(param.sourcePos, param.ref, type);
    }
  }

  enum BinOP {
    ADD("+"),
    SUB("-"),
    MUL("*"),
    DIV("/"),
    MOD("%"),
    EQ("=="),
    NE("!="),
    LT("<"),
    LE("<="),
    GT(">"),
    GE(">="),
    LOGICAL_AND("&&"),
    LOGICAL_OR("||");

    public final @NotNull String symbol;

    BinOP(@NotNull String symbol) {
      this.symbol = symbol;
    }
  }

  enum UnaryOP {
    POS("+"),
    NEG("-"),
    LOGICAL_NOT("!");
    public final @NotNull String symbol;

    UnaryOP(@NotNull String symbol) {
      this.symbol = symbol;
    }
  }
}
