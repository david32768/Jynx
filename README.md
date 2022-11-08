# Jynx
[Jynx(bird)](https://en.wikipedia.org/wiki/Wryneck)


This is a rewritten version of [Jasmin](https://github.com/davidar/jasmin)
 using [ASM](https://asm.ow2.io) version 9.3 as a back end.
It is written in Java V1_8 (apart from module-info.java)
 and supports all features up to V19 except user attributes.

More checking is done before using ASM. For example
 stack and local variables types are checked assuming
 all objects are Object.

ASM is used to generate stack maps where required. However a stack map
must be provided if any label after an unconditional branch is
not previously branched to or is not an exception handler.

The opportunity has beeen taken to change the syntax of some statements.

It supports "macros" as a service.

## WARNING

*	There will be invalid classes which will not produce errors.
*	There will be "valid" classes which will fail.

## Usage

Usage:

 jynx {options} .jx_file
   (produces a class file from a .jx file)

Options are:

*	--SYSIN use SYSIN as input file. (omit .jx_file)
*	--USE_STACK_MAP use user stack map instead of ASM generated
*	--WARN_UNNECESSARY_LABEL warn if label unreferenced or alias
*	--WARN_STYLE warn if names non-standard
*	--GENERATE_LINE_NUMBERS generate line numbers
*	--BASIC_VERIFIER use ASM BasicVerifier
*	--SIMPLE_VERIFIER use ASM SimpleVerifier (default)
*	--ALLOW_CLASS_FORNAME let simple verifier use Class.forName()
*	--CHECK_REFERENCES check that called methods or used fields exist (on class path)
*	--VALIDATE_ONLY do not output class file
*	--JVM_OPS_ONLY only JVM specified ops
*	--DEBUG exit with stack trace if error

 2jynx {options}  class-name|class_file > .jx_file
   (produces a .jx file from a class)

Options are:

*	--SKIP_CODE do not produce code
*	--SKIP_DEBUG do not produce debug info
*	--SKIP_FRAMES do not produce stack map
*	--SKIP_ANNOTATIONS do not produce annotations
*	--DOWN_CAST if necessary reduces JVM release to maximum supported by ASM version

## Jasmin 1.0

Reference: **Java Virtual Machine** by Jon Meyer and Troy Downing; O'Reilly 1997

Changes are:

*	unicode escape sequences are actioned before parsing line
*	.end_method instead of .end method
*	tableswitch - new format
```
	; <high> is always omitted
	; tableswitch <minimum> default <default_label> <label-array>
	; <label-array> = .array[\n<labael>]+\n.end_array
	tableswitch 0 default DefaultLabel .array
		ZeroLabel
		OneLabel
	.end_array
```		
*	lookupswitch - new format
```
	; lookupswitch default <default_label> <switch-array>
	; <switch-array> = .array[\n<num> -> <label>]+\n.end_array
	lookupswitch default DefaultLabel .array
		1 -> Label1
		10 -> Label2
	.end_array
```
*	invokeinterface; omit number as will be calculated and precede method_name with '#'
```
	; invokeinterface java/util/Enumeration/hasMoreElements()Z 1
	invokeinterface #java/util/Enumeration/hasMoreElements()Z
```
*	if .limit is omitted it will be calculated rather than 1
*	class names etc. must be valid Java names
*	labels are constrained to be a Java Id or if generated start with an @
*	.interface must be used to declare an interface rather than .class interface
```
	; .class interface abstract anInterface
	.interface anInterface
```
*	labels in .catch must not be previously defined
*	if .var labels are omitted then from start_method to end_method is assumed
*	float constants must be suffixed by 'F' and long constants by 'L'
*	hexadecimal constants are supported
*	default version is V17(61.0)
  
## Jasmin 2.4

Changes are

*	offsets instead of labels are NOT supported
*	user attributes are NOT supported
*	.bytecode -> .version
```
	; .bytecoode 61.0 ; Jasmin 2.4
	.version V17 ; Jynx
```
*	options (without -- prefix) can be on .version directive
```
	.version V17 GENERATE_LINE_NUMBERS JVM_OPS_ONLY
```
*	.deprecated removed; use "deprecated" pseudo-access_flag
```
	; .class public aClass
	; .deprecated ; Jasmin 2.4
	.class public deprecated aClass ; Jynx
	; etc.
```
*	.enum instead of .class enum
```
	; .class enum anEnum ; Jasmin 2.4
	.enum anEnum [ Jynx
```
*	.define_annotation instead of .class annotation
```
	; .class annotation interface abstract anAnnotationClass ; Jasmin 2.4
	.define_annotation anAnnotationClass ; Jynx
```
*	.inner class -> .inner_class, .inner interface -> .inner_interface etc.
```
	; class file (jvms 4.7.6)
	; InnerClass Attribute is inner_class, outer_class, inner_name, inner_class_flags
	;	inner_class must be present but outer_class and inner_name may be absent

	: ASM
	; visitInnerClass(name,outer_name,inner_name,access)
	;	name is inner_class

	; Jasmin 2,4
	; .inner class [<access-spec>] [<name>] [inner <classname>] [outer <name>]
	;	name after access-spec is inner_name (which can be absent)
	;	classname is inner_class

	; Jynx
	; .inner_class [<access-spec>] <inner_class> [outer <outer_class>] [innername <innername>]
	;	i.e. change 

	; .inner class x inner y$z outer w ; Jasmin 2.4
	.inner_class y$z outer w innername x ; Jynx
```
*	.enclosing method -> .enclosing_method or .outer_class
*	invokedynamic boot method and parameters must be specified
```
	; (a boot method parameter may be dynamic) 
	; invokedynamic { name desc  boot_method_and_parameters }
	; see examples/Java11/Hi.java
```
*	An interface method name should be preceded with a '#' in invoke ops and handles
```
	; invokestatic anInterfaceMethod ; Jasmin 2.4
	invokestatic #anInterfaceMethod ; Jynx
```
*	if signature of a field is present must use .signature directive (NOT appear in .field directive) 
*	.package
```
	; .class interface abstract aPackage/package-info ; Jasmin 2.4
	.package aPackage ; Jynx
```

### ANNOTATIONS

*	.end_annotation instead of .end annotation
*	parameter annotations start at zero instead of 1
*	default maxparms annotation is numparms(ASM)
*	[ annotation values must use .array e.g
```
	; intArrayValue [I = 0 1 ; Jasmin 2,4
	intArrayValue [I = .array ; Jynx
		0
		1
	.end_array
```
*	annotation of array of annotations must end line with .annotation_array not .annotation
*	.end_annotation_array NOT .end_annotation after .annotation_array at end of line


## Additions

*	.nesthost
```
	; .nesthost <host-class-name>
	.nesthost x/y
```
*	.nestmember
```
	; .nestmember <member-class-name>
	.nestmember x/y
```
*	.permittedSubclass
```
	; .permittedSubclass <subclass-name>
	.permittedSubclass x/y
```
*	add support for method-handle to ldc
```
	; grammar
	; ldc <method-handle>
	; <method-handle> = [<handle-to-method>|<handle-to-field>]

	; <handle-to-method> = <method-handle-type>:<method-name-desc>
	; <method-handle-type> = [VL|ST|SP|NW|IN]
	ldc ST:java/lang/Integer/getInteger(Ljava/lang/String;)java/lang/Integer

	; <handle-to-field> = <field-handle-type>:<field-name-desc>
	; <field-handle-type> = [GF|GS|PF|PS]
	; <field-nam-desc> = <field-name>()<field-desc>
	ldc GS:java/lang/Float/MIN_VALUE()F ; handle for smallest POSITIVE float
	
```
*	dynamic ldc
```
	; (a boot method parameter may be dynamic) 
	; ldc { name desc boot_method_and_parameters } 
```
*	.record ; see examples/Java17/Point.java
```
	; grammar
	; .record <access-spec> <record-name>
	; [<class-hdr-directive>]*
	; [<component>]*
	; [<field>]* ; .field for each component must be present
	; [<method>]* ; .method for each component must be present
```
*	.component
```
	; grammar
	; <component> = [<simple-component>|<compound-component>]
	; <simple-component> = .component <component-name> <desc>
	.component x I

	; <compond-component> = .component <component-name> <desc>
	;	[<signature>]?
	;	<annotation>|<type-annotation>]*
	; 	.end_component
```
*	.module ; see examples/Java11/module-info.java
```
	; grammar
	; .class acc_module module-info ; assumed
	; .module <access-spec> <module-name> [<version>]?
	; [<class-hdr-directives>]* ; for those which are valid for module (end of jvms 4.1)
	; [<main>|<requires>|<exports>|<open>|<uses>|<supports>|<packages>]*
	; ; end of file

	; <main>? = .main <class-name>

	; <requires>* = .requires <access-spec> <module-name> [module-version]?

	; <exports> = [<unqualified-export>|<qualified export>]*
	; <unqualified-export> = .exports <access-spec> <package-name>
	; <qualified-export> = .exports <access-spec> <package-name> to <module-name-array>
	; <module-name-array> = .array[\n<module-name>]+\n.end_array
	
	; <open>=[<unqualified-open>|<qualified open>]*
	; <unqualified-open> = open <access-spec> <package-name>
	; <qualified-open> = .open <access-spec> <package-name> to <module-name-array>
	
	; <uses>* = .uses <service-class-name>

	; <provides>* = .provides <service-class-name> with <class-name-array>
	; <class-name-array> = .array[\n<class-name>]+\n.end_array
	
	; <packeges>* = .packages <package-name-array>
	; <package-name-array> = .array[\n<package-name>]+\n.end_array
	
```
*	alias ops
```
	; e.g.
	ildc 3 : alias for iconst_3
	ildc 240 ; alias for bipush 240
	ildc -32767 ; alias for sipush -32767
	ildc 32768 ; alias for ldc 32768
	; also lldc, fldc and dldc
```
*	extended ops
```
	; e.g.
	if_fcmpge label
	; fcmpl
	; ifge labal
	swap2
	; dup2_x2
	; pop2
```
*	call common java methods
```
	; e.g.
	iabs ; invokestatic java/lang/Math/iabs(I)I
```
*	.macrolib <macro-library-name>
*	type_annotations
*	.hints ; used to help verification if class(es) not available
```
	; grammar
	; .hints .array
	; <subtype-class-name> subtypes <class_name> ; x is subtype of y
	; <common-class_name> common <class-name1> class_name2> ; x is common of y and z
	; .end_array
```
