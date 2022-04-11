package org.brunhild.cli;

import kala.control.Option;
import kala.function.CheckedSupplier;
import org.brunhild.concrete.Stmt;
import org.brunhild.concrete.resolve.context.EmptyContext;
import org.brunhild.concrete.resolve.context.ModuleContext;
import org.brunhild.core.Def;
import org.brunhild.error.InterruptException;
import org.brunhild.error.Reporter;
import org.brunhild.error.SourceFile;
import org.brunhild.parser.BrunhildParserImpl;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

public record SingleFileCompiler(
  @NotNull Reporter reporter
) {
  public <E extends IOException> int compile(
    @NotNull Path sourceFile,
    @NotNull CompilerFlags flags
  ) throws IOException {
    var source = new SourceFile(sourceFile.getFileName().toString(), Files.readString(sourceFile), Option.some(sourceFile));
    return compile(source, reporter -> new EmptyContext(source, reporter).derive("Main"), flags);
  }

  public <E extends IOException> int compile(
    @NotNull SourceFile sourceFile,
    @NotNull Function<Reporter, ModuleContext> context,
    @NotNull CompilerFlags flags
  ) throws IOException {
    var ctx = context.apply(reporter);
    return catching(reporter, flags, () -> {
      var parser = new BrunhildParserImpl(reporter);
      var program = parser.program(sourceFile);
      Def.PrimFactory.install(ctx);
      var resolved = Stmt.resolve(program, ctx);
      var tycked = Stmt.tyck(reporter, resolved);
      System.out.println(tycked.joinToString("\n"));
      return 0;
    });
  }

  public static int catching(
    @NotNull Reporter reporter,
    @NotNull CompilerFlags flags,
    @NotNull CheckedSupplier<Integer, IOException> block
  ) throws IOException {
    try {
      return block.getChecked();
    } catch (InterruptException e) {
      reporter.reportString(e.stage().name() + " interrupted.");
      if (flags.interruptedTrace()) e.printStackTrace();
    }
    return 1;
  }
}
