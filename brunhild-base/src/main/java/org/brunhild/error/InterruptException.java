package org.brunhild.error;

import org.jetbrains.annotations.NotNull;

public abstract class InterruptException extends RuntimeException {
  public enum Stage {
    Parsing,
    Resolving,
    Tycking,
  }

  public abstract @NotNull Stage stage();
}
