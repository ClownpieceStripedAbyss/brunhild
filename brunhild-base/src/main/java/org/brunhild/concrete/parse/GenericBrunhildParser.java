package org.brunhild.concrete.parse;

import kala.collection.immutable.ImmutableSeq;
import org.brunhild.concrete.Stmt;
import org.brunhild.error.SourceFile;
import org.jetbrains.annotations.NotNull;

public interface GenericBrunhildParser {
  @NotNull ImmutableSeq<Stmt> program(@NotNull SourceFile sourceFile);
}
