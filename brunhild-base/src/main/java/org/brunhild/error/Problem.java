package org.brunhild.error;

import org.jetbrains.annotations.NotNull;

public interface Problem {
  enum Severity {
    ERROR, WARNING, INFO
  }

  @NotNull SourcePos sourcePos();
  @NotNull Severity severity();
  @NotNull String describe();
}
