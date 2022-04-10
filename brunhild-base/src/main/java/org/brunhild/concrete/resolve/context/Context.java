package org.brunhild.concrete.resolve.context;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Option;
import org.brunhild.concrete.problem.NameNotFoundProblem;
import org.brunhild.error.*;
import org.brunhild.error.InterruptedException;
import org.brunhild.generic.LocalVar;
import org.brunhild.generic.Var;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public interface Context {
  ImmutableSeq<String> TOP_LEVEL_MOD = ImmutableSeq.empty();

  @NotNull Reporter reporter();
  @NotNull SourceFile underlyingSourceFile();
  @Nullable Context parent();
  @Nullable Var getLocal(@NotNull String name);

  default @NotNull ImmutableSeq<String> moduleName() {
    var p = parent();
    return p != null ? p.moduleName() : TOP_LEVEL_MOD;
  }

  default @NotNull Option<Var> getOption(@NotNull String name) {
    var ctx = this;
    while (ctx != null) {
      var v = ctx.getLocal(name);
      if (v != null) return Option.some(v);
      ctx = ctx.parent();
    }
    return Option.none();
  }

  default @NotNull Var get(@NotNull String name, @NotNull SourcePos sourcePos) {
    var v = getOption(name);
    if (v.isEmpty()) reportAndThrow(new NameNotFoundProblem(sourcePos, name));
    return v.get();
  }

  default @NotNull BindContext bind(@NotNull String name, @NotNull LocalVar ref) {
    return new BindContext(this, name, ref);
  }

  default @NotNull BindContext bind(@NotNull LocalVar ref) {
    return bind(ref.name(), ref);
  }

  default @NotNull ModuleContext derive(@NotNull String modName) {
    return new ModuleContext(this, moduleName().appended(modName));
  }

  @Contract("_->fail")
  default void reportAndThrow(@NotNull Problem problem) {
    reporter().report(problem);
    throw new ResolvingInterrupted();
  }

  class ResolvingInterrupted extends InterruptedException {
    @Override public InterruptedException.@NotNull Stage stage() {
      return Stage.Resolving;
    }
  }
}