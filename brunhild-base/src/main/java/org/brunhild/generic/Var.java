package org.brunhild.generic;

import org.jetbrains.annotations.Debug;
import org.jetbrains.annotations.NotNull;

@Debug.Renderer(hasChildren = "false", text = "name")
public interface Var {
  @NotNull String name();
}
