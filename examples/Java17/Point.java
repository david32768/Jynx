record Point(int x, int y){}
/*
; options = []
; Jynx DISASSEMBLY 0.20
.version V17
.source Point.java
.record final Point
  .super java/lang/Record
  .inner_class public static final java/lang/invoke/MethodHandles$Lookup outer java/lang/invoke/MethodHandles innername Lookup
.component x I
.component y I

.field private final 'x' I

.field private final 'y' I

.method <init>(II)V
  .parameter 0 'x'
  .parameter 1 'y'
    @L0:
    .line 1
    aload_0
    invokespecial java/lang/Record.<init>()V
    aload_0
    iload_1
    putfield Point.x I
    aload_0
    iload_2
    putfield Point.y I
    return
    .limit locals 3
    .limit stack 2
.end_method

.method public final toString()Ljava/lang/String;
    @L0:
    .line 1
    aload_0
    invokedynamic { toString (LPoint;)Ljava/lang/String; ST:java/lang/runtime/ObjectMethods.bootstrap(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object; Point "x;y" GF:Point.x()I GF:Point.y()I }
    areturn
    .limit locals 1
    .limit stack 1
.end_method

.method public final hashCode()I
    @L0:
    .line 1
    aload_0
    invokedynamic { hashCode (LPoint;)I ST:java/lang/runtime/ObjectMethods.bootstrap(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object; Point "x;y" GF:Point.x()I GF:Point.y()I }
    ireturn
    .limit locals 1
    .limit stack 1
.end_method

.method public final equals(Ljava/lang/Object;)Z
    @L0:
    .line 1
    aload_0
    aload_1
    invokedynamic { equals (LPoint;Ljava/lang/Object;)Z ST:java/lang/runtime/ObjectMethods.bootstrap(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object; Point "x;y" GF:Point.x()I GF:Point.y()I }
    ireturn
    .limit locals 2
    .limit stack 2
.end_method

.method public x()I
    @L0:
    .line 1
    aload_0
    getfield Point.x I
    ireturn
    .limit locals 1
    .limit stack 1
.end_method

.method public y()I
    @L0:
    .line 1
    aload_0
    getfield Point.y I
    ireturn
    .limit locals 1
    .limit stack 1
.end_method
;*/
