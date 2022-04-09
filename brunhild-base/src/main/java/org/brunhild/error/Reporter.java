package org.brunhild.error;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public interface Reporter {
  void report(@NotNull Problem problem);

  @ApiStatus.Internal
  default void reportString(@NotNull String string) {
    report(new Problem() {
      @Override public @NotNull SourcePos sourcePos() {return SourcePos.NONE;}

      @Override public @NotNull Severity severity() {return Severity.INFO;}

      @Override public @NotNull String describe() {return string;}
    });
  }
}
