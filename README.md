# Jynx

This is a rewritten version of [Jasmin](https://github.com/davidar/jasmin)
 using [ASM](https://asm.ow2.io) version 9.2 as a back end.
It is written in Java V1_8 (apart from module-info.java)
 and supports all features up to V18 except user attributes.

More checking is done before using ASM. For example
 stack and local variables types are checked assuming
 all objects are Object.

ASM is used to generate stack maps where required. However a stack map
must be provided if any label after an unconditional branch is
not previously branched to or is not an exception handler.

The opportunity has beeen taken to change the syntax of some statements.

It supports "macros" as a service with structured macros (cf WebAssembly MVP) as an example.

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
*	--CHECK_METHOD_REFERENCES check that called methods exist (on class path)
*	--VALIDATE_ONLY do not output class file
*	--JVM_OPS_ONLY only JVM specified ops
*	--DEBUG exit with stack trace if error

 2jynx {options}  class-name|class_file > .jx_file
   (produces a .jx file from a class)

Options are:

*	--SKIP_CODE do not produce code
*	--SKIP_DEBUG do not produce debug info
*	--SKIP_FRAMES do not produce stack map
*	--DOWN_CAST if necessary reduces JVM release to maximum supported by ASM version

## Jasmin 1.0

Reference: **Java Virtual Machine** by Jon Meyer and Troy Downing; O'Reilly 1997

Changes are:

*	unicode escape sequences are actioned before parsing line
*	.end_method instead of .end method
*	tableswitch - new format
```
	; <high> is always omitted
	; tableswitch <minimum> default <default_label> <array of labels>
	tableswitch 0 default DefaultLabel .array
		ZeroLabel
		OneLabel
	.end_array
```		
*	lookupswitch - new format
```
	; lookupswitch default <default_label> <array of <num -> <label>>
	lookupswitch default DefaultLabel .array
		1 -> Label1
		10 -> Label2
	.end_array
```
*	invokeinterface; omit number as will be calculated and precede method_name with '@'
```
	; invokeinterface java/util/Enumeration/hasMoreElements()Z 1
	invokeinterface @java/util/Enumeration/hasMoreElements()Z
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
	; .bytecoode 61.0 ; change to
	.version V17
```
*	options (without -- prefix) can be on .version directive
```
	.version V17 GENERATE_LINE_NUMBERS JVM_OPS_ONLY
```
*	.deprecated removed; use deprecated pseudo-access_flag
```
	; .class public aClass
	; .deprecated
	.class public deprecated aClass
	; etc.
```
*	.enum instead of .class enum
```
	; .class enum anEnum
	.enum anEnum
```
*	.define_annotation instead of .class annotation
```
	; .class annotation interface abstract anAnnotationClass ; change to
	.define_annotation anAnnotationClass
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
	; .inner class [<access>] [<name>] [inner <classname>] [outer <name>]
	;	name after access is inner_name (which can be absent)
	;	classname is inner_class

	; Jynx
	; .inner_class [<access>] <inner_class> [outer <outer_class>] [innername <innername>]
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
*	An interface method name should be preceded with a '@' in invoke ops and handles
```
	; invokestatic anInterfaceMethod
	invokestatic @anInterfaceMethod
```
*	if signature of a field is present must use .signature directive (NOT appear in .field directive) 
*	.package
```
	; .class interface abstract aPackage/package-info ; change to
	.package aPackage
```

### ANNOTATIONS

*	.end_annotation instead of .end annotation
*	parameter annotations start at zero instead of 1
*	default maxparms annotation is numparms(ASM)
*	[ annotation values must use .array e.g
```
	intArrayValue [I = .array
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
	; ldc <method-handle>
	; grammar for method-handle
	<handle-type>:<method-name-desc>
	<handle-type> = [VL|ST|SP|NW|IN]

	<handle-type>:<field-name-desc>
	<handle-type> = [GF|GS|PF|PS]
	<field-nam-desc> = <field-name>()<field-desc>
	
```
*	dynamic ldc
```
	; (a boot method parameter may be dynamic) 
	; ldc { name desc boot_method_and_parameters } 
```
*	.record
```
	; grammar
	.record <access-flags> <record-name>
	[class-hdr-directive]*
	[component]*
	[field]* ; .field for each component must be present
	[method]* ; .method for each component must be present
```
*	.component
```
	; grammar
	.component <component-name> <desc>
	[annotation|type-annotation]*
	.end_component ; only necessary if any annotations or type annotations
```
*	.module ; see examples/Java11/module-info.java
*	alias ops e.g. ildc 3 ; load integer 3
*	extended ops e.g. if_fcmpge label
*	call common java methods e.g. iabs instead of invokestatic java/lang/Math/iabs(I)I
*	.macrolib <macro-library-name>
*	.macrolib structured ; gives access to structured ops e.g. BLOCK, LOOP, END
*	type_annotations
*	.catch block (.catch .end_catch) is required if it has an .except_type_annotation
*	.hint ; used to help verification if class(es) not available
```
	; grammar
	.hint <subtype-class-name> subtype <class_name>
	.hint <common-class_name> common <class-name1> class_name2>
```
