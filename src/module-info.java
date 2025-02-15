module com.github.david32768x.jynx {
	exports com.github.david32768.jynx;
	exports jynx2asm.ops;
        exports jynx;
        exports jvm;
	requires org.objectweb.asm;
	requires org.objectweb.asm.tree;
	requires org.objectweb.asm.tree.analysis;
	requires org.objectweb.asm.util;
 	uses jynx2asm.ops.MacroLib;
        uses jynx.MainOptionService;
        provides jynx.MainOptionService with asm2jynx.Main2Jynx,
                checker.MainStructure,
                jynx2asm.MainJynx,
                roundtrip.MainRoundTrip;
}
