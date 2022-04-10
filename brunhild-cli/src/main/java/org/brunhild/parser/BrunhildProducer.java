package org.brunhild.parser;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Option;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.brunhild.concrete.Decl;
import org.brunhild.concrete.Expr;
import org.brunhild.concrete.Stmt;
import org.brunhild.error.Reporter;
import org.brunhild.error.SourceFile;
import org.brunhild.error.SourcePos;
import org.brunhild.generic.LocalVar;
import org.brunhild.generic.Type;
import org.brunhild.parser.problem.ConstNotInitializedProblem;
import org.brunhild.parser.problem.ParsingInterrupted;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public record BrunhildProducer(
  @NotNull SourceFile sourceFile,
  @NotNull Reporter reporter
) {
  public @NotNull ImmutableSeq<Stmt> program(@NotNull BrunhildParser.ProgramContext ctx) {
    return ctx.programItem().stream().flatMap(this::programItem).collect(ImmutableSeq.factory());
  }

  public @NotNull Stream<Stmt> programItem(@NotNull BrunhildParser.ProgramItemContext ctx) {
    var varDecl = ctx.varDecl();
    var fnDecl = ctx.fnDecl();
    if (varDecl != null) return varDecl(varDecl).stream();
    if (fnDecl != null) return Stream.of(fnDecl(fnDecl));
    return unreachable();
  }

  private @NotNull ImmutableSeq<Stmt> varDecl(@NotNull BrunhildParser.VarDeclContext ctx) {
    var isConst = ctx.KW_CONST() != null;
    var type = primitiveType(ctx.primitiveType());
    return ctx.varDeclItem().stream().map(c -> varDeclItem(c, isConst, type)).collect(ImmutableSeq.factory());
  }

  private @NotNull Stmt varDeclItem(@NotNull BrunhildParser.VarDeclItemContext ctx, boolean isConst, @NotNull Type type) {
    var id = ctx.ID().getText();
    var maybeArray = arrayType(ctx.arrayTypeSuffix().stream(), type);
    var maybeConst = isConst ? new Type.Const(maybeArray) : maybeArray;
    if (isConst && ctx.ASSIGN() == null) {
      reporter.report(new ConstNotInitializedProblem(sourcePosOf(ctx), id));
      throw new ParsingInterrupted();
    }
    var initVal = ctx.varInitVal();
    return new Decl.VarDecl(
      sourcePosOf(ctx),
      id,
      initVal != null ? Option.some(varInitVal(initVal)) : Option.none(),
      maybeConst
    );
  }

  private @NotNull Expr varInitVal(@NotNull BrunhildParser.VarInitValContext ctx) {
    var expr = ctx.expr();
    if (expr != null) return expr(expr);
    var values = ctx.varInitVal().stream().map(this::varInitVal).collect(ImmutableSeq.factory());
    return new Expr.LitArrayExpr(sourcePosOf(ctx), values);
  }

  private @NotNull Stmt fnDecl(@NotNull BrunhildParser.FnDeclContext ctx) {
    var returnType = returnType(ctx.returnType());
    var id = ctx.ID().getText();
    var tele = fnParams(ctx.fnParams());
    var block = block(ctx.block());
    var blockSourcePos = sourcePosOf(ctx.block());
    return new Decl.FnDecl(
      sourcePosOf(ctx.ID()),
      id,
      tele,
      new Stmt.BlockStmt(blockSourcePos, block),
      returnType
    );
  }

  private @NotNull ImmutableSeq<Expr.Param> fnParams(@NotNull BrunhildParser.FnParamsContext ctx) {
    return ctx.fnParam().stream().map(this::fnParam).collect(ImmutableSeq.factory());
  }

  private @NotNull Expr.Param fnParam(@NotNull BrunhildParser.FnParamContext ctx) {
    var primitiveType = primitiveType(ctx.primitiveType());
    var id = ctx.ID().getText();
    var arrayType = ctx.arrayParamTypeSuffix();
    var type = arrayType != null ? arrayParamType(arrayType, primitiveType) : primitiveType;
    return new Expr.Param(sourcePosOf(ctx), new LocalVar(id), type);
  }

  private @NotNull Type arrayParamType(@NotNull BrunhildParser.ArrayParamTypeSuffixContext arrayType, @NotNull Type elementType) {
    // first dimension is always inferred according to Brunhild.g4
    var type = new Type.Array(elementType, new Type.DimInferred());
    return arrayType.expr().stream()
      .map(this::expr)
      .map(Type.DimExpr::new)
      .collect(ImmutableSeq.factory())
      .foldLeft(type, Type.Array::new);
  }

  private @NotNull Type arrayType(@NotNull Stream<BrunhildParser.ArrayTypeSuffixContext> arrayType, @NotNull Type elementType) {
    return arrayType
      .map(c -> expr(c.expr()))
      .map(Type.DimExpr::new)
      .collect(ImmutableSeq.factory())
      .foldLeft(elementType, Type.Array::new);
  }

  private @NotNull Type returnType(@NotNull BrunhildParser.ReturnTypeContext ctx) {
    if (ctx.KW_VOID() != null) return new Type.Void();
    return primitiveType(ctx.primitiveType());
  }

  private @NotNull Type primitiveType(@NotNull BrunhildParser.PrimitiveTypeContext ctx) {
    if (ctx.KW_INT() != null) return new Type.Int();
    if (ctx.KW_FLOAT() != null) return new Type.Float();
    return unreachable();
  }

  private @NotNull ImmutableSeq<Stmt> block(@NotNull BrunhildParser.BlockContext ctx) {
    return ctx.blockItem().stream().flatMap(this::blockItem).collect(ImmutableSeq.factory());
  }

  private @NotNull Stream<Stmt> blockItem(@NotNull BrunhildParser.BlockItemContext ctx) {
    var varDecl = ctx.varDecl();
    var stmt = ctx.stmt();
    if (varDecl != null) return varDecl(varDecl).stream();
    if (stmt != null) return Stream.of(stmt(stmt));
    return unreachable();
  }

  private @NotNull Stmt stmt(@NotNull BrunhildParser.StmtContext ctx) {
    var sourcePos = sourcePosOf(ctx);
    if (ctx instanceof BrunhildParser.AssignContext stmt)
      return new Stmt.AssignStmt(sourcePos, lval(stmt.lval()), expr(stmt.expr()));
    if (ctx instanceof BrunhildParser.ExprStmtContext stmt)
      return new Stmt.ExprStmt(expr(stmt.expr()));
    if (ctx instanceof BrunhildParser.BlockStmtContext stmt)
      return new Stmt.BlockStmt(sourcePos, block(stmt.block()));
    if (ctx instanceof BrunhildParser.IfContext stmt)
      return new Stmt.IfStmt(sourcePos, cond(stmt.cond()), stmt(stmt.stmt(0)), stmt.KW_ELSE() == null ? Option.none() : Option.some(stmt(stmt.stmt(1))));
    if (ctx instanceof BrunhildParser.WhileContext stmt)
      return new Stmt.WhileStmt(sourcePos, cond(stmt.cond()), stmt(stmt.stmt()));
    if (ctx instanceof BrunhildParser.BreakContext)
      return new Stmt.BreakStmt(sourcePos);
    if (ctx instanceof BrunhildParser.ContinueContext)
      return new Stmt.ContinueStmt(sourcePos);
    if (ctx instanceof BrunhildParser.ReturnContext stmt)
      return new Stmt.ReturnStmt(sourcePos, stmt.expr() == null ? Option.none() : Option.some(expr(stmt.expr())));
    return unreachable();
  }

  private @NotNull Expr expr(@NotNull BrunhildParser.ExprContext ctx) {
    return addExpr(ctx.addExpr());
  }

  private @NotNull Expr addExpr(@NotNull BrunhildParser.AddExprContext ctx) {
    var sourcePos = sourcePosOf(ctx);
    var mulExpr = mulExpr(ctx.mulExpr());
    if (ctx.ADD() != null) return new Expr.BinaryExpr(sourcePos, Expr.BinOP.ADD, addExpr(ctx.addExpr()), mulExpr);
    if (ctx.SUB() != null) return new Expr.BinaryExpr(sourcePos, Expr.BinOP.SUB, addExpr(ctx.addExpr()), mulExpr);
    return mulExpr;
  }

  private @NotNull Expr mulExpr(@NotNull BrunhildParser.MulExprContext ctx) {
    var sourcePos = sourcePosOf(ctx);
    var unaryExpr = unaryExpr(ctx.unaryExpr());
    if (ctx.MUL() != null) return new Expr.BinaryExpr(sourcePos, Expr.BinOP.MUL, mulExpr(ctx.mulExpr()), unaryExpr);
    if (ctx.DIV() != null) return new Expr.BinaryExpr(sourcePos, Expr.BinOP.DIV, mulExpr(ctx.mulExpr()), unaryExpr);
    if (ctx.MOD() != null) return new Expr.BinaryExpr(sourcePos, Expr.BinOP.MOD, mulExpr(ctx.mulExpr()), unaryExpr);
    return unaryExpr;
  }

  private @NotNull Expr unaryExpr(@NotNull BrunhildParser.UnaryExprContext ctx) {
    if (ctx.primaryExpr() != null) return primaryExpr(ctx.primaryExpr());
    if (ctx.ID() != null) {
      var sourcePos = sourcePosOf(ctx);
      var id = ctx.ID().getText();
      var appArg = ctx.appArg();
      var args = appArg == null ? ImmutableSeq.<Expr>empty() : appArg.expr().stream().map(this::expr).collect(ImmutableSeq.factory());
      return new Expr.AppExpr(sourcePos, new Expr.UnresolvedExpr(sourcePosOf(ctx.ID()), id), args);
    } else {
      var sourcePos = sourcePosOf(ctx);
      if (ctx.ADD() != null) return new Expr.UnaryExpr(sourcePos, Expr.UnaryOP.POS, unaryExpr(ctx.unaryExpr()));
      if (ctx.SUB() != null) return new Expr.UnaryExpr(sourcePos, Expr.UnaryOP.NEG, unaryExpr(ctx.unaryExpr()));
      if (ctx.LOGICAL_NOT() != null) return new Expr.UnaryExpr(sourcePos, Expr.UnaryOP.LOGICAL_NOT, unaryExpr(ctx.unaryExpr()));
      return unreachable();
    }
  }

  private @NotNull Expr primaryExpr(@NotNull BrunhildParser.PrimaryExprContext ctx) {
    if (ctx.expr() != null) return expr(ctx.expr());
    if (ctx.lval() != null) return lval(ctx.lval());
    if (ctx.number() != null) {
      var number = ctx.number();
      if (number.INT_LITERAL() != null) return new Expr.LitIntExpr(sourcePosOf(number), Integer.parseInt(number.INT_LITERAL().getText()));
      if (number.FLOAT_LITERAL() != null) return new Expr.LitFloatExpr(sourcePosOf(number), Float.parseFloat(number.FLOAT_LITERAL().getText()));
    }
    return unreachable();
  }

  private @NotNull Expr lval(@NotNull BrunhildParser.LvalContext ctx) {
    var id = ctx.ID().getText();
    var sourcePos = sourcePosOf(ctx);
    var expr = ctx.expr();
    var unresolved = new Expr.UnresolvedExpr(sourcePos, id);
    if (expr.isEmpty()) return unresolved;
    return expr.stream()
      .map(this::expr)
      .collect(ImmutableSeq.factory())
      .foldLeft((Expr) unresolved, (acc, idx) -> new Expr.IndexExpr(acc.sourcePos().union(idx.sourcePos()), acc, idx));
  }

  private @NotNull Expr cond(@NotNull BrunhildParser.CondContext ctx) {
    return lOrExpr(ctx.lOrExpr());
  }

  private @NotNull Expr lOrExpr(@NotNull BrunhildParser.LOrExprContext ctx) {
    var lAnd = lAndExpr(ctx.lAndExpr());
    if (ctx.LOGICAL_OR() != null) return new Expr.BinaryExpr(sourcePosOf(ctx), Expr.BinOP.LOGICAL_OR, lOrExpr(ctx.lOrExpr()), lAnd);
    return lAnd;
  }

  private @NotNull Expr lAndExpr(@NotNull BrunhildParser.LAndExprContext ctx) {
    var eq = eqExpr(ctx.eqExpr());
    if (ctx.LOGICAL_AND() != null) return new Expr.BinaryExpr(sourcePosOf(ctx), Expr.BinOP.LOGICAL_AND, lAndExpr(ctx.lAndExpr()), eq);
    return eq;
  }

  private @NotNull Expr eqExpr(@NotNull BrunhildParser.EqExprContext ctx) {
    var rel = relExpr(ctx.relExpr());
    if (ctx.EQ() != null) return new Expr.BinaryExpr(sourcePosOf(ctx), Expr.BinOP.EQ, eqExpr(ctx.eqExpr()), rel);
    if (ctx.NE() != null) return new Expr.BinaryExpr(sourcePosOf(ctx), Expr.BinOP.NE, eqExpr(ctx.eqExpr()), rel);
    return rel;
  }

  private @NotNull Expr relExpr(@NotNull BrunhildParser.RelExprContext ctx) {
    var add = addExpr(ctx.addExpr());
    if (ctx.LT() != null) return new Expr.BinaryExpr(sourcePosOf(ctx), Expr.BinOP.LT, relExpr(ctx.relExpr()), add);
    if (ctx.LE() != null) return new Expr.BinaryExpr(sourcePosOf(ctx), Expr.BinOP.LE, relExpr(ctx.relExpr()), add);
    if (ctx.GT() != null) return new Expr.BinaryExpr(sourcePosOf(ctx), Expr.BinOP.GT, relExpr(ctx.relExpr()), add);
    if (ctx.GE() != null) return new Expr.BinaryExpr(sourcePosOf(ctx), Expr.BinOP.GE, relExpr(ctx.relExpr()), add);
    return add;
  }

  private <T> T unreachable() {
    throw new IllegalStateException("unreachable");
  }

  private @NotNull SourcePos sourcePosOf(@NotNull TerminalNode node) {
    var token = node.getSymbol();
    var line = token.getLine();
    return new SourcePos(
      sourceFile,
      token.getStartIndex(),
      token.getStopIndex(),
      line,
      token.getCharPositionInLine(),
      line,
      token.getCharPositionInLine() + token.getText().length() - 1
    );
  }

  private @NotNull SourcePos sourcePosOf(@NotNull ParserRuleContext ctx) {
    var start = ctx.getStart();
    var end = ctx.getStop();
    return new SourcePos(
      sourceFile,
      start.getStartIndex(),
      end.getStopIndex(),
      start.getLine(),
      start.getCharPositionInLine(),
      end.getLine(),
      end.getCharPositionInLine() + end.getText().length() - 1
    );
  }
}
