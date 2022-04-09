package org.brunhild.error;

import kala.control.Option;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public record SourceFile(@NotNull String name, @NotNull Option<Path> path) {
  public static final @NotNull SourceFile NONE = new SourceFile("<unknown>", Option.none());
}
