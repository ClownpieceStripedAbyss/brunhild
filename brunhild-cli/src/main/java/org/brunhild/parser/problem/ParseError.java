package org.brunhild.parser.problem;

import org.brunhild.error.Problem;
import org.brunhild.error.SourcePos;
import org.jetbrains.annotations.NotNull;

public record ParseError(@Override @NotNull SourcePos sourcePos, @NotNull String message) implements Problem {
  @Override public @NotNull String describe() {
    return "Parser error: " + message;
  }

  @Override public @NotNull Severity severity() {
    return Severity.ERROR;
  }
}
