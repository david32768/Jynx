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
.version V1_8
.source Hi.java
.class public Hi
  .super java/lang/Object ; assumed if not supplied

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
    new java/lang/StringBuilder
    dup
    invokespecial java/lang/StringBuilder/<init>()V
    ldc "Hi "
    invokevirtual java/lang/StringBuilder/append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    aload_1
    invokevirtual java/lang/StringBuilder/append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invokevirtual java/lang/StringBuilder/toString()Ljava/lang/String;
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
