package org.brunhild.concrete.resolve.context;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableHashMap;
import org.brunhild.concrete.problem.NameExistsProblem;
import org.brunhild.error.Reporter;
import org.brunhild.error.SourceFile;
import org.brunhild.error.SourcePos;
import org.brunhild.generic.Var;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ModuleContext(
  @Override @NotNull Context parent,
  @Override @NotNull ImmutableSeq<String> moduleName,
  @NotNull MutableHashMap<String, Var> definitions
) implements Context {
  public ModuleContext(@NotNull Context parent, @NotNull ImmutableSeq<String> moduleName) {
    this(parent, moduleName, new MutableHashMap<>());
  }

  public void addGlobal(@NotNull SourcePos sourcePos, @NotNull String name, @NotNull Var ref) {
    if (definitions.containsKey(name)) reportAndThrow(new NameExistsProblem(sourcePos, name));
    definitions.put(name, ref);
  }

  @Override public @Nullable Var getLocal(@NotNull String name) {
    return definitions.getOrNull(name);
  }

  @Override public @NotNull Reporter reporter() {
    return parent.reporter();
  }

  @Override public @NotNull SourceFile underlyingSourceFile() {
    return parent.underlyingSourceFile();
  }
}
