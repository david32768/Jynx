public class Hi {

	public static void main(String[] args) {
		String name = args.length == 0?"":args[0];
		String msg = "Hi " + name;
		System.out.println(msg);
	}

}
/*
; options = [SKIP_FRAMES]
; Jynx DISASSEMBLY 0+18
.version V11
.source Hi.java
.class public Hi
  .super java/lang/Object
  .inner_class public static final java/lang/invoke/MethodHandles$Lookup outer java/lang/invoke/MethodHandles innername Lookup

.method public <init>()V
    @L0:
    .line 1
    aload_0
    invokespecial java/lang/Object/<init>()V
    return
    .limit locals 1
    .limit stack 1
.end_method

.method public static main([Ljava/lang/String;)V
    @L0:
    .line 4
    aload_0
    arraylength
    ifne @L1
    ldc ""
    goto @L2
    @L1:
    aload_0
    iconst_0
    aaload
    @L2:
    astore_1
    @L3:
    .line 5
    aload_1
    invokedynamic { makeConcatWithConstants (Ljava/lang/String;)Ljava/lang/String; ST:java/lang/invoke/StringConcatFactory/makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; "Hi \u0001" }
    astore_2
    @L4:
    .line 6
    getstatic java/lang/System/out Ljava/io/PrintStream;
    aload_2
    invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
    @L5:
    .line 7
    return
    .limit locals 3
    .limit stack 2
.end_method
;*/
