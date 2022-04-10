package org.brunhild.concrete;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Option;
import org.brunhild.concrete.resolve.context.Context;
import org.brunhild.core.Def;
import org.brunhild.error.SourcePos;
import org.brunhild.generic.DefVar;
import org.brunhild.generic.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public sealed abstract class Decl implements Stmt {
  public final @NotNull SourcePos sourcePos;
  public final @NotNull Type<Expr> result;
  public @Nullable Context context;
  public @Nullable Def.Signature signature;

  protected Decl(@NotNull SourcePos sourcePos, @NotNull Type<Expr> result) {
    this.sourcePos = sourcePos;
    this.result = result;
  }

  public @NotNull SourcePos sourcePos() {
    return sourcePos;
  }

  public @NotNull Type<Expr> result() {
    return result;
  }

  public abstract @NotNull DefVar<?, ?> ref();

  public static final class FnDecl extends Decl {
    public final @NotNull DefVar<Def.FnDef, FnDecl> ref;
    public @NotNull ImmutableSeq<Expr.@NotNull Param> telescope;
    public @NotNull Stmt body;

    public FnDecl(
      @NotNull SourcePos sourcePos,
      @NotNull String name,
      @NotNull ImmutableSeq<Expr.@NotNull Param> telescope,
      @NotNull Stmt body,
      @NotNull Type<Expr> result
    ) {
      super(sourcePos, result);
      this.telescope = telescope;
      this.body = body;
      this.ref = DefVar.concrete(this, name);
    }

    @Override public @NotNull DefVar<Def.FnDef, FnDecl> ref() {
      return this.ref;
    }
  }

  public static final class VarDecl extends Decl {
    public final @NotNull DefVar<Def.VarDef, VarDecl> ref;
    public @NotNull Option<Expr> body;

    public VarDecl(
      @NotNull SourcePos sourcePos,
      @NotNull String name,
      @NotNull Option<Expr> body,
      @NotNull Type<Expr> result
    ) {
      super(sourcePos, result);
      this.body = body;
      this.ref = DefVar.concrete(this, name);
    }

    @Override public @NotNull DefVar<Def.VarDef, VarDecl> ref() {
      return this.ref;
    }
  }
}
