module com.github.david32768.jynx {
	requires org.objectweb.asm;
	requires org.objectweb.asm.tree;
	requires org.objectweb.asm.tree.analysis;
	requires org.objectweb.asm.util;
	exports com.github.david32768.jynx;
	exports jynx2asm.ops;
	uses jynx2asm.ops.MacroLib;
	provides jynx2asm.ops.MacroLib with textifier.ASMTextMacroLib;
}
// javac -p asmmods -d build\classes module-info.java
// jar --create --file Jynx.jar --main-class com.github.david32768.jynx.Main --module-version 0.19 -C build\classes\ .
/*
.version V11
.define_module
.source module-info.java
.module com.github.david32768.jynx 0.19 ; version added by jar tool --module-version
.main com/github/david32768/jynx/Main ; main added by jar tool --main-class
.requires mandated java.base 11
.requires org.objectweb.asm 9.3.0
.requires org.objectweb.asm.tree 9.3.0
.requires org.objectweb.asm.tree.analysis 9.3.0
.requires org.objectweb.asm.util 9.3.0
.exports com/github/david32768/jynx
.exports jynx2asm/ops
.uses jynx2asm/ops/MacroLib
.provides jynx2asm/ops/MacroLib with .array
  textifier/ASMTextMacroLib
.end_array
.packages .array ; packages added by jar tool -C
  asm
  asm/instruction
  asm2jynx
  com/github/david32768/jynx
  jvm
  jynx
  jynx2asm
  jynx2asm/handles
  jynx2asm/ops
  textifier
.end_array
.end_module
; */
