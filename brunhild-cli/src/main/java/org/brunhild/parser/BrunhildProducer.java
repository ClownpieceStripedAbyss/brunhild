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
    // TODO: implement
    throw new UnsatisfiedLinkError("TODO");
  }

  private @NotNull Expr expr(@NotNull BrunhildParser.ExprContext ctx) {
    // TODO: implement
    throw new UnsupportedOperationException("TODO");
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
