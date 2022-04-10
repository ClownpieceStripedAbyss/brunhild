package org.brunhild.parser;

import kala.collection.immutable.ImmutableSeq;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.brunhild.concrete.Stmt;
import org.brunhild.error.Reporter;
import org.brunhild.error.SourceFile;
import org.brunhild.error.SourcePos;
import org.jetbrains.annotations.NotNull;

public record BrunhildProducer(
  @NotNull SourceFile sourceFile,
  @NotNull Reporter reporter
) {
  public @NotNull ImmutableSeq<Stmt> program(BrunhildParser.ProgramContext ctx) {
    return ImmutableSeq.empty();
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
