package org.brunhild.parser;

import kala.collection.immutable.ImmutableSeq;
import org.antlr.v4.runtime.CodePointBuffer;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.brunhild.concrete.Stmt;
import org.brunhild.concrete.parse.GenericBrunhildParser;
import org.brunhild.error.Reporter;
import org.brunhild.error.SourceFile;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.IntBuffer;

public record BrunhildParserImpl(@NotNull Reporter reporter) implements GenericBrunhildParser {
  private static @NotNull BrunhildLexer lexer(@NotNull String text) {
    var intBuffer = IntBuffer.wrap(text.codePoints().toArray());
    var codePointBuffer = CodePointBuffer.withInts(intBuffer);
    var charStream = CodePointCharStream.fromBuffer(codePointBuffer);
    return new BrunhildLexer(charStream);
  }

  @Contract("_, _ -> new")
  private static @NotNull BrunhildParser parser(@NotNull SourceFile sourceFile, @NotNull Reporter reporter) {
    var lexer = lexer(sourceFile.sourceCode());
    lexer.removeErrorListeners();
    var listener = new ReporterErrorListener(sourceFile, reporter);
    lexer.addErrorListener(listener);
    var parser = new BrunhildParser(new CommonTokenStream(lexer));
    parser.removeErrorListeners();
    parser.addErrorListener(listener);
    return parser;
  }

  @Override public @NotNull ImmutableSeq<Stmt> program(@NotNull SourceFile sourceFile) {
    var program = parser(sourceFile, reporter);
    return new BrunhildProducer(sourceFile, reporter).program(program.program());
  }
}
