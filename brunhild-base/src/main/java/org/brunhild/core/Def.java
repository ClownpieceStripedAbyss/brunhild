package org.brunhild.core;

import kala.collection.immutable.ImmutableSeq;
import org.brunhild.concrete.Decl;
import org.brunhild.concrete.resolve.context.ModuleContext;
import org.brunhild.error.SourcePos;
import org.brunhild.generic.DefVar;
import org.brunhild.generic.LocalVar;
import org.brunhild.generic.Type;
import org.jetbrains.annotations.NotNull;

public abstract sealed class Def implements Proclaim {
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

  public static final class FnDef extends Def {
    public final @NotNull DefVar<FnDef, Decl.FnDecl> ref;
    public final @NotNull Proclaim body;

    public FnDef(
      @NotNull DefVar<FnDef, Decl.FnDecl> ref,
      @NotNull ImmutableSeq<Term.Param> telescope,
      @NotNull Proclaim body,
      @NotNull Type<Term> result
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

  public static final class VarDef extends Def {
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

  public static final class PrimDef extends Def {
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

  public enum PrimFactory {
    GetInt("getint", intT("_")),
    GetChar("getch", intT("_")),
    GetFloat("getfloat", floatT("_")),
    GetArray("getarray", intT("_"), arrayT(intT("array"))),
    GetFloatArray("getfarray", intT("_"), arrayT(floatT("array"))),
    PutInt("putint", voidT(), intT("i")),
    PutChar("putch", voidT(), intT("ch")),
    PutFloat("putfloat", voidT(), floatT("f")),
    PutArray("putarray", voidT(), intT("size"), arrayT(intT("array"))),
    PutFloatArray("putfarray", voidT(), intT("size"), arrayT(floatT("array"))),
    Printf("putf", voidT(), stringT("fmt")),
    StartTime("starttime", "_sysy_starttime", voidT()),
    StopTime("stoptime", "_sysy_stoptime", voidT()),
    StartTimeABI("_sysy_starttime", voidT(), intT("line")),
    StopTimeABI("_sysy_stoptime", voidT(), intT("line")),
    ;

    public static final @NotNull ImmutableSeq<PrimFactory> PRIMITIVES = ImmutableSeq.of(
      GetInt, GetChar, GetFloat, GetArray, GetFloatArray,
      PutInt, PutChar, PutFloat, PutArray, PutFloatArray,
      Printf, StartTime, StopTime, StartTimeABI, StopTimeABI
    );

    public static void install(@NotNull ModuleContext context) {
      PRIMITIVES.forEach(prim -> context.addGlobal(SourcePos.NONE, prim.prim.ref.name(), prim.prim.ref));
    }

    public final @NotNull PrimDef prim;
    public final @NotNull String abiName;

    PrimFactory(@NotNull String name, @NotNull Term.Param result, @NotNull Term.Param... params) {
      this.prim = new PrimDef(name, ImmutableSeq.from(params), result.type());
      this.abiName = name;
    }

    PrimFactory(@NotNull String name, @NotNull String abiName, @NotNull Term.Param result, @NotNull Term.Param... params) {
      this.prim = new PrimDef(name, ImmutableSeq.from(params), result.type());
      this.abiName = abiName;
    }

    private static @NotNull Term.Param intT(@NotNull String name) {
      return new Term.Param(new LocalVar(name), new Type.Int<>());
    }

    private static @NotNull Term.Param floatT(@NotNull String name) {
      return new Term.Param(new LocalVar(name), new Type.Float<>());
    }

    private static @NotNull Term.Param stringT(@NotNull String name) {
      return new Term.Param(new LocalVar(name), new Type.String<>());
    }

    private static @NotNull Term.Param voidT() {
      return new Term.Param(new LocalVar("_"), new Type.Void<>());
    }

    private static @NotNull Term.Param arrayT(@NotNull Term.Param param) {
      return new Term.Param(param, new Type.Array<>(param.type(), new Type.DimInferred()));
    }
  }
  public record Signature(
    @NotNull ImmutableSeq<Term.@NotNull Param> tele,
    @NotNull Type<Term> result
  ) {
  }
}
