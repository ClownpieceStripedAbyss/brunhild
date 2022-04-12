package org.brunhild.cli;

import kala.control.Option;
import kala.function.CheckedSupplier;
import org.brunhild.compiling.Pass;
import org.brunhild.compiling.Pipeline;
import org.brunhild.compiling.generate.Generator;
import org.brunhild.compiling.optimize.TreeFold;
import org.brunhild.concrete.resolve.context.EmptyContext;
import org.brunhild.concrete.resolve.context.ModuleContext;
import org.brunhild.core.Def;
import org.brunhild.error.InterruptException;
import org.brunhild.error.Reporter;
import org.brunhild.error.SourceFile;
import org.brunhild.parser.BrunhildParserImpl;
import org.brunhild.tyck.Gamma;
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
      Def.PrimFactory.install(ctx);

      var artifact = Pipeline.Begin
        .then(Pass.Parsing, new BrunhildParserImpl(reporter))
        .then(Pass.Resolving, ctx)
        .then(Pass.Tycking, reporter)
        .then(TreeFold.Pass, new Gamma.ConstGamma())
        .then(Generator.justForFun())
        .peek(s -> System.out.println(s.joinToString("\n")))
        .perform(sourceFile);

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
