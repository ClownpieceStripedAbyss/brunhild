package org.brunhild.core;

import kala.collection.immutable.ImmutableSeq;
import org.brunhild.concrete.Decl;
import org.brunhild.generic.DefVar;
import org.brunhild.generic.Type;
import org.jetbrains.annotations.NotNull;

public abstract class Def {
  public final @NotNull Type<Term> result;
  public final @NotNull ImmutableSeq<Term.Param> telescope;

  protected Def(@NotNull ImmutableSeq<Term.Param> telescope, @NotNull Type<Term> result) {
    this.telescope = telescope;
    this.result = result;
  }

  abstract @NotNull DefVar<?, ?> ref();

  public @NotNull Type<Term> result() {
    return this.result;
  }

  public @NotNull ImmutableSeq<Term.Param> telescope() {
    return this.telescope;
  }

  public static class FnDef extends Def {
    public final @NotNull DefVar<FnDef, Decl.FnDecl> ref;
    public final @NotNull Proclaim body;

    public FnDef(
      @NotNull DefVar<FnDef, Decl.FnDecl> ref,
      @NotNull ImmutableSeq<Term.Param> telescope,
      @NotNull Type<Term> result,
      @NotNull Proclaim body
    ) {
      super(telescope, result);
      ref.core = this;
      this.ref = ref;
      this.body = body;
    }

    @Override @NotNull DefVar<FnDef, Decl.FnDecl> ref() {
      return this.ref;
    }
  }

  public static class VarDef extends Def {
    public final @NotNull DefVar<Def.VarDef, Decl.VarDecl> ref;
    public @NotNull Term body;

    public VarDef(
      @NotNull DefVar<Def.VarDef, Decl.VarDecl> ref,
      @NotNull Term body,
      @NotNull Type<Term> result
    ) {
      super(ImmutableSeq.empty(), result);
      ref.core = this;
      this.ref = ref;
      this.body = body;
    }

    @Override public @NotNull DefVar<Def.VarDef, Decl.VarDecl> ref() {
      return this.ref;
    }
  }

  public static class PrimDef extends Def {
    public final @NotNull DefVar<Def.PrimDef, ?> ref;

    public PrimDef(
      @NotNull String name,
      @NotNull ImmutableSeq<Term.Param> telescope,
      @NotNull Type<Term> result
    ) {
      super(telescope, result);
      this.ref = DefVar.core(this, name);
    }

    @Override @NotNull DefVar<Def.PrimDef, ?> ref() {
      return this.ref;
    }
  }
}
