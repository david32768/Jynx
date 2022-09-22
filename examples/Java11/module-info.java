module com.github.david32768.jynx {
	exports com.github.david32768.jynx;
	exports jynx2asm.ops;
	requires org.objectweb.asm;
	requires org.objectweb.asm.tree;
	requires org.objectweb.asm.tree.analysis;
	requires org.objectweb.asm.util;
	uses jynx2asm.ops.MacroLib;
	provides com.github.david32768.jynx.MacroLib
		with jynx2asm.ops.StructuredMacroLib;
}
// javac -p asmmods -d build\classes module-info.java
// jar --create --file Jynx.jar --main-class com.github.david32768.jynx.Main --module-version 0.18 -C build\classes\ .
/*
.version V11
.source module-info.java
.module com.github.david32768.jynx 0.18 ; version added by jar tool --module-version
.main com/github/david32768/jynx/Main ; main added by jar tool --main-class
.exports com/github/david32768/jynx
.exports jynx2asm/ops
.requires mandated java.base 11
.requires org.objectweb.asm 9.2.0
.requires org.objectweb.asm.tree 9.2.0
.requires org.objectweb.asm.tree.analysis 9.2.0
.requires org.objectweb.asm.util 9.2.0
.uses com/github/david32768/jynx/MacroLib
.provides jynx2asm/ops/MacroLib with .array
  jynx2asm/ops/StructuredMacroLib
.end_array
.packages .array ; packages added by jar tool -C
  asm
  asm/instruction
  asm2jynx
  com/github/david32768/jynx
  jvm
  jynx
  jynx2asm
  jynx2asm/ops
.end_array
; */
