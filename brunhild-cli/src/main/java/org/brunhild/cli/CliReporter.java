package org.brunhild.cli;

import org.brunhild.error.Problem;
import org.brunhild.error.Reporter;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public record CliReporter(
  @NotNull Consumer<String> out,
  @NotNull Consumer<String> err
) implements Reporter {
  public static @NotNull CliReporter stdio() {
    return new CliReporter(System.out::println, System.err::println);
  }

  @Override public void report(@NotNull Problem problem) {
    var severity = problem.severity();
    var errorMsg = problem.describe();
    if (severity == Problem.Severity.ERROR || severity == Problem.Severity.WARNING) err.accept(errorMsg);
    else out.accept(errorMsg);
  }
}
