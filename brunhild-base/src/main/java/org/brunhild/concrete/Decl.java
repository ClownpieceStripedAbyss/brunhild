package org.brunhild.concrete;

import kala.collection.immutable.ImmutableSeq;
import org.brunhild.core.Def;
import org.brunhild.error.SourcePos;
import org.brunhild.generic.DefVar;
import org.brunhild.generic.Type;
import org.jetbrains.annotations.NotNull;

public sealed abstract class Decl {
  public final @NotNull SourcePos sourcePos;
  public final @NotNull Type result;

  protected Decl(@NotNull SourcePos sourcePos, @NotNull Type result) {
    this.sourcePos = sourcePos;
    this.result = result;
  }

  public @NotNull SourcePos sourcePos() {
    return sourcePos;
  }

  public @NotNull Type result() {
    return result;
  }

  abstract @NotNull DefVar<?, ?> ref();

  public static final class FnDecl extends Decl {
    public final @NotNull DefVar<Def.FnDef, FnDecl> ref;
    public final @NotNull ImmutableSeq<Expr.@NotNull Param> telescope;
    public final @NotNull Stmt body;

    public FnDecl(
      @NotNull SourcePos sourcePos,
      @NotNull String name,
      @NotNull ImmutableSeq<Expr.@NotNull Param> telescope,
      @NotNull Stmt body,
      @NotNull Type result
    ) {
      super(sourcePos, result);
      this.telescope = telescope;
      this.body = body;
      this.ref = DefVar.concrete(this, name);
    }

    @Override @NotNull DefVar<Def.FnDef, FnDecl> ref() {
      return this.ref;
    }
  }

  public static final class VarDecl extends Decl {
    public final @NotNull DefVar<Def.VarDef, VarDecl> ref;
    public final @NotNull Expr body;
    public final boolean constVar;

    public VarDecl(
      @NotNull SourcePos sourcePos,
      @NotNull String name,
      @NotNull Expr body,
      @NotNull Type result,
      boolean constVar
    ) {
      super(sourcePos, result);
      this.constVar = constVar;
      this.body = body;
      this.ref = DefVar.concrete(this, name);
    }

    @Override @NotNull DefVar<Def.VarDef, VarDecl> ref() {
      return this.ref;
    }
  }
}
