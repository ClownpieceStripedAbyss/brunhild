package org.brunhild.error;

import org.jetbrains.annotations.NotNull;

public abstract class InterruptedException extends RuntimeException {
  public enum Stage {
    Parsing,
    Resolving,
  }

  public abstract @NotNull Stage stage();
}
