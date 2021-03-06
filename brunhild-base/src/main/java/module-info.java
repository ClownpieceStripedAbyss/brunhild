module org.brunhild.base {
  requires static org.jetbrains.annotations;
  requires transitive kala.base;
  requires transitive kala.collection;

  exports org.brunhild.concrete;
  exports org.brunhild.concrete.problem;
  exports org.brunhild.concrete.resolve;
  exports org.brunhild.concrete.resolve.context;
  exports org.brunhild.concrete.parse;

  exports org.brunhild.core;
  exports org.brunhild.error;
  exports org.brunhild.generic;
  exports org.brunhild.tyck;
  exports org.brunhild.compiling;
  exports org.brunhild.compiling.optimize;
  exports org.brunhild.compiling.generate;
}
