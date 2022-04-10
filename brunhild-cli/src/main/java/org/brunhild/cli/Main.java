package org.brunhild.cli;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

public class Main {
  public static void main(String @NotNull [] args) throws IOException {
    if (args.length < 1) {
      System.out.println("Usage: brunhild <input-file>");
      System.exit(1);
    }

    var compiler = new SingleFileCompiler(CliReporter.stdio());
    int exit = compiler.compile(Path.of(args[0]), new CompilerFlags(true));
    System.exit(exit);
  }
}
