# Jynx

This is a rewritten version of [Jasmin](https://github.com/davidar/jasmin)
 using [ASM](https://asm.ow2.io) version 9.2 as a back end.
It is written in Java V1_8 and supports all features up to V18 except user attributes.

More checking is done before using ASM. For example
 stack and local variables types are checked assuming
 all objects are Object.

ASM is used to generate stack maps where required. However a stack map
must be provided if any label after an unconditional branch is
not previously branched to or is not an exception handler.

The opportunity has beeen taken to change the syntax of some statements.

It supports "macros" as a service with WebAssembly MVP instructions as an example.

## WARNING

*	There will be invalid classes which will not produce errors.
*	There will be "valid" classes which will fail.

## Usage

Usage:

 jynx {options} .jx_file
   (produces a class file from a .jx file)

Options are:

*	--SYSIN use SYSIN as input file
*	--USE_STACK_MAP use user stack map instead of ASM generated
*	--WARN_UNNECESSARY_LABEL warn if label unreferenced or alias
*	--WARN_STYLE warn if names non-standard
*	--GENERATE_LINE_NUMBERS generate line numbers
*	--WARN_INDENT check indent for structured code
*	--BASIC_VERIFIER use ASM BasicVerifier
*	--SIMPLE_VERIFIER use ASM SimpleVerifier (default)
*	--ALLOW_CLASS_FORNAME let simple verifier use Class.forName()
*	--CHECK_METHOD_REFERENCES check that called methods exist (on class path)
*	--PREPEND_CLASSNAME prepend class name to methods and fields if neccessary
*	--VALIDATE_ONLY do not output class file

 2jynx {options}  class-name|class_file > .jx_file
   (produces a .jx file from a class)

Options are:

*	--SKIP_CODE do not produce code
*	--SKIP_DEBUG do not produce debug info
*	--SKIP_FRAMES do not produce stack map

## Jasmin 1.0

Reference: **Java Virtual Machine** by Jon Meyer and Troy Downing; O'Reilly 1997

Changes are:

*	unicode escape sequences are actioned before parsing line
*	.end_method instead of .end method
*	table switch - new format
```
		; <high> is always omitted
		tableswitch 0 DefaultLabel .array
			ZeroLabel
			OneLabel
		.end_array
```		
*	lookup switch - new format
```
		lookupswitch DLabel .array
			1 : Label1
			10 : Label2
		.end_array
```
*	invokeinterface n -> invvokeinterface ; omit number as will be calculated
*	default for .limit is calculated rather than 1
*	labels are constrained to be a Java Id or if generated start with an @
*	.interface must be used to declare an interface rather than .class interface
```
		; .class interface abstract anInterface ; change to
		.interface anInterface
```
*	labels in .catch must not be previously defined (ASM)
*	if .var labels are omitted then from start_method to end_method is assumed
*	float constants must be suffixed by 'F' and long constants by 'L'.
*	hexadecimal constants are supported.
*	default version is V18(62.0)
  
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
		.version V17 GENERATE_LINE_NUMBERS PREPEND_CLASSNAME
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
		; .class enum anEnum ; change to
		.enum anEnum
```
*	.define_annotation instead of .class annotation
```
		; .class annotation interface abstract anAnnotationClass ; change to
		.define_annotation anAnnotationClass
```
*	.inner
```
		; swap "name" x and "inner" y as "name" may be omitted
		; .inner class x inner y ... ; change to
		.inner_class y innername x ...
		; also .inner_interface etc.
```
*	.enclosing method -> .enclosing_method or .enclosing_class
*	invokedynamic boot method and parameters must be specified
```
		; (a boot method parameter may be dynamic) 
		; invokedynamic { name desc  boot_method_and_parameters }
```
*	An interface method name should be preceded with a '@' in invoke ops and handles
'''
		; invokestatic anInterfaceMethod
		invokestatic @anInterfaceMethod
'''
*	if signature of a field is present must use .signature directive (NOT appear in .field directive) 
*	.package
```
		; .class interface abstract aPackage/package-info ; change to
		.package aPackage
```

### ANNOTATIONS

*	.X_annotation instead of .annotation X e.g. .annotation visible -> .visible_annotation
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

*	.visible_type_annotation
*	.invisible_type_annotation
*	.nesthost
*	.nestmember
*	.record
*	.component
*	.module
*	.hint
*	.permittedSubclass
*	dynamic ldc
```
		; (a boot method parameter may be dynamic) 
		; ldc { name desc boot_method_and_parameters } 
```
*	alias ops e.g. ildc 3 ; load integer 3
*	extended ops e.g. if_fcmpge label
*	call common java methods e.g. iabs instead of invokestatic java/lang/Math/iabs(I)I
*	structured ops e.g. BLOCK, LOOP, END
*	.macrolib
*	.catch block with (in)visible_except_annotation
