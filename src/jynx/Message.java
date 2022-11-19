package jynx;

import static jynx.LogMsgType.*;

public enum Message {

    M0(BLANK,"Jynx version %s; maximum Java version is %s"),
    M1(BLANK,"display help message"),
    M2(BLANK,"display version information"),
    M3(BLANK,"program terminated because of errors"),
    M4(BLANK,"%n%s; Java runtime version %s"),
    M5(BLANK,"only JVM specified ops"),
    M6(BLANK,"Options are:%n"),
    M7(BLANK,"use SYSIN as input file"),
    M8(BLANK,"check that called methods or used fields exist (on class path)"),
    M9(BLANK,"generate line numbers"),
    M10(BLANK,"warn if label unreferenced or alias"),
    M11(BLANK,"let simple verifier use Class.forName() for non-java classes"),
    M12(BLANK,"%nUsage:%n"),
    M13(BLANK,"exit with stack trace if error"),
    M14(BLANK,"if necessary reduces JVM release to maximum supported by ASM version"),
    M15(BLANK,"warn if names non-standard"),
    M16(BLANK,"use ASM BasicVerifier"),
    M17(BLANK,"use ASM SimpleVerifier (default)"),
    M18(BLANK,"do not produce annotations"),
    M19(BLANK,"use user stack map instead of ASM generated"),
    M20("invalid major version(%d)"),
    M21("invalid minor version(%d) - spec table 4.1A"),
    M22(WARNING,"value required (%d) for %s is more than limit value (%d)"),    

    M24(INFO,"ambiguous option %s: %s assumed"),
    M25(BLANK,"treat warnings as errors"),
    M26(BLANK,"invalid main-option name - %s"),
    M27("length of label node chain exceeds %d"),
    M28("no args have been specified for main option %s"),
    M29(BLANK,"do not produce debug info"),
    M30(BLANK,"do not produce stack map"),
    M31("%s already set in line%n    %s"),
    M32("%s is not a valid option"),
    M33("%s limit has already been set by line:%n  %s"),
    M34(ENDINFO,"some generated line numbers have been reduced mod %d as exceed unsigned short max"),
    M35("current value(%d) for %s excedes limit(%d)"),
    M36("label already defined in line:%n  %s"),
    M37("invalid stack frame type - %s"),
    M38("%s is not a valid char literal - blank assumed"),
    M39(BLANK,"do not produce code"),
    M40("duplicate %s: %s already defined at line %d"),
    M41("component can only appear in a record"),
    M42("%s invalid in context"),
    M43("line contains newline or carriage return character"),

    M45("value(%d) for %s exceeds that(%d) set by line:%n  %s"),
    M46("method %s has no body"),
    M47("Invalid component name - %s"),
    M48("number of Record components %d disagrees with number of instance fields %d"),
    M49("Invalid label - %s"),
    M50("Array type must be last - type %s found after array type %s"),
    M51(BLANK,"do not output class file"),
    M52(WARNING,"class name contains '$' but is not an internal class"),
    M53(WARNING,"as %s is not a static field, ' = %s' is silently ignored by JVM "),
    M54("variable %d has not been written to"),
    M55("duplicate %s: %s %s already defined at line %d"),
    M56(WARNING,"gap %d between local variables: %d - %d"),
    M57("Version %s does not support %s (supported %s)"),
    M58(ENDINFO,"used hint: %s %s %s"),
    M59("method %s cannot be abstract in final class"),
    M60(WARNING,"local variables [%s ] are written but not read"),
    M61("invalid stack frame type(%s) - %s assumed"),
    M62("macro nest level exceeds %d"),
    M63("Loading of %s not supported in %s"),
    M64(STYLE,"final static field name (%s) is not in uppercase"),
    M65("local variables [%s ] are read but not written"),
    M66("invalid %s: %s"),
    M67(ENDINFO,"poolsz = %d"),
    M68(WARNING,"Quoted string not followed by white space; blank inserted before '%c'"),
    M69("Embedded naked quote"),
    M70("cannot range check floating point numbers"),
    M71(WARNING,"as %s is not a final static field, ' = %s' may be silently ignored by JVM (JVMS 5.5 6)"),
    M72(WARNING,"version %s may not be fully supported"),

    M74("Invalid variable number (%d + %d is not unsigned short)"),
    M75("Method %s failed %s check:%n    %s"),
    M76("unknown handle tag: %d"),
    M77("%s value %d is not in range [%d,%d]"),
    M78("%s has different signature %s to component %s"),
    M79("Trying to read beyond end of file"),
    M80("Bad octal sequence"),
    M81("Directive must be first token but line = %s"),
    M82(ENDINFO,"used hint: %s %s %s %s"),
    M83("Bad escape sequence"),
    M84(INFO,"%s terminated because of severe error"),
    M85(INFO,"%s terminated because of too many errors"),
    M86("invalid op - %s"),

    M88(INFO,"options = %s"),
    M89(INFO,"file = %s version = %s"),
    M90("unused tokens - starting at %s"),

    M92("%s count(%d) must be in range [%d,%d]"),
    M93(STYLE,"class name (%s) does not start with uppercase letter"),

    M95(ENDINFO,".line directives ignored as %s specified"),
    M96("syntax error in annotation field type"),
    M97("file(%s) does not have %s suffix"),
    M98("Unsupported version of %s: first parms are not compatible with:%n   %s"),
    M99("Separator \'%s\' not found in %s"),

    M101("unknown handle mnemonic: %s"),

    M104(BLANK,"class %s %s completed successfully"),
    M105("unknown option %s - ignored"),
    M106("labels in %s must not be defined yet"),
    M107("unknown access flag (%#04x) in context %s ignored"),
    M108("type index (%d) outside range [0 - %d]"),
    M109("reserved word %s expected but found %s"),
    M110("access flag(s) %s not valid for version %s"),
    M111(ENDINFO,"instructions = %d labels = %d directives = %d pre_comments = %d"),

    M113(WARNING,"value %s requires 'L' suffix as is a long constant"),
    M114("Requires at most one of {%s} specified"),
    M115("Not first token - token = %s"),
    M116(BLANK,"%s created - size %d bytes"),

    M118("Requires all of {%s} specified"),
    M119("this and following lines skipped until %s"),
    M120("Requires only one of {%s} specified"),
    M121("Instruction \'%s\' dropped as unreachable after '%s' without intervening label"),
    M122(WARNING,"Instruction \'%s\' dropped as unreachable after '%s' without intervening label"),
    M123("compilation of %s failed because of %s"),
    M124("stack frame is definitely required here"),
    M125("Requires none of {%s} specified"),
    M126(INFO,"\'%s %s\' is required and has been added"),
    M127("directive %s reached before %s"),
    M128("% directive not allowed for component method %s"),
    M129("invalid typecode - %d"),

    M131(BLANK,"class %s %s completed  unsuccesfully - number of errors is %d"),
    M132("%s must have a %s method of type %s"),
    M133("package %s has already appeared in %s"),
    M134("boolean value is neither Boolean or Integer 0/1: value = %s class = %s"),
    M135(ENDINFO,"for consistency add %s prefix to method name for %s"),
    M136("Extraneous directive %s"),

    M138("%s cannot be used for constant - %s"),
    M139("%s prefix is invalid for %s"),
    M140("reading next token after reaching last"),
    M141("unknown constant; class = %s"),
    M142(WARNING,"comment contains unprintable characters (replaced by '?'); comment = \'%s\'"),
    M143(INFO,"%s %s assumed"),
    M144("unknown value class = %s"),
    M145("Invalid method description %s"),
    M146("constant type = %s not valid in this context"),
    M147(WARNING,"unknown Java version %s - %s used"),
    
    M150("expected equal values for index length = %d numind = %d"),
    M151(ENDINFO,"Class.forName(%s) has been used"),
    M152("zero length name"),
    M153(WARNING,"as class has a %s method it should have a %s method"),
    M154("%s: key %d has duplicate entries %s and %s"),
    M155("code is not allowed as method is abstract or native"),
    M156("instance variables or methods with no %s method"),

    M158(STYLE,"components of package %s are not all lowercase"),
    M159("Invalid type - %s"),
    M160("invalid access flags %s for %s are dropped"),
    M161("%s: asm value (%d) does not agree with jvm value(%d)"),
    M162("some annotation values for %s are null; annotation ignored"),
    M163("stack underflow"),
    M164("stack overflow"),
    M165(SEVERE,"Directive in wrong place; Current state = %s%n  Expected state was one of %s"),
    M166("top of stack('%c') is not a 32 bit type"),

    M168("unexpected directive(%s) in annotation"),
    M169(WARNING,"package %s used in %s has been added to %s"),
    M170("invalid type annotation directive - %s"),
    M171(WARNING,"version %s outside range [%s,%s] - %s used"),

    M175("unknown Jynx desc = %c"),
    M176("%s not found as a macro library service"),
    M177("missing %s"),
    M178("invalid type ref sort - %d"),
    M179("String length of %d exceeds maximum %d"),
    M180("top of stack('%c') and next on stack('%c') are not both 32 bit types"),
    M181("%s directive is invalid for MODULE - value specifued was %s"),
    M182("top of stack is %s but required is %s"),
    M183("Type is not known - %s"),
    M184("current stack is %s but %s is %s"),
    M185("%s required for label %s is %s but currently is %s"),
    M186("%s directive for %s must be %s but is %s"),
    M187("annotation parameter count(%d) not in range[1,%d]"),
    M188("n (%d) is greater than current local size(%d)"),

    M190("mismatched local %d: required %s but found %s"),
    M191("method requires %s but found %s"),

    M194("annotation parameter count already been set"),
    M195(WARNING,"inner class name (%s) does not contain '$'"),
    M196(WARNING,"no %s instruction found"),
    M197("inner class cannot be module"),
    M198("empty line - should not occur"),

    M200(WARNING,"unknown release (%d): used %s"),
    M201("%s can only occur in locals"),
    M202("unused field(s) in typeref not zero"),
    M203(WARNING,"potential infinite loop - catch using label equals catch from label"),
    M204("different types for %s; was %s but now %s"),
    M205("enum class changed; was %s now %s"),
    M206("Invalid type letter '%c' (%d)"),
    M207("possible serialization method %s is not private"),
    M208("code not complete - last %s was %s"),
    M209("code not complete - last was %s"),
    M210(ENDINFO,"%s instruction ignored as not required"),
    M211("unknown variable: %s"),
    M212("attempting to load variable %d but current max is %d"),
    M213(WARNING,"label %s defined before use - locals assumed as before last unconditional op"),
    M214(WARNING,"field %s %s does not exist in this class but may exist in superclass/superinterface"),
    M215(" %s field %s accessed by %s op %s"),
    M216("frame locals %s incompatible with current locals %s"),
    M217("from label %s is not before to label %s"),
    M218(INFO,"SYSIN will be used as input"),
    M219("wrong number of parameters after options %s"),
    M220(WARNING,"label %s is an alias for label %s"),
    M221("required %s for var %d but found %s"),
    M222("either option %s is specified or file name is present but not both"),
    M223("Annotation for unknown variables [%s ]"),
    M224(WARNING,"invalid %s as only has %s"),
    M225(WARNING,"empty %s ignored"),
    M226("invalid access flags %s for component"),
    M227(WARNING,"possible serialization method %s does not match %s"),
    M228(WARNING,"indent %d found but expected %d"),
    M229("duplicate key %d; previous target = %s, current target = %s"),
    M230(WARNING,"keys must be in ascending order; key = %d; previous key = %s"),
    M231(WARNING,"Last instruction was %s: expected %s"),
    M232("Last instruction was %s: expected %s"),
    M233("Duplicate entry %s in %s: previous entry at line %d"),
    M234("invalid parameter number %d; bounds are [0 - %d)"),
    M235("%s method appears in an interface"),
    M236(STYLE,"%s (%s) starts with uppercase letter and is not all uppercase"),
    M237("error accepting class file: %s"),
    M238("error reading class file: %s"),
    M239(WARNING,"%s does not override object equals method in %s"),
    M240("%s is for internal use only"),
    M241("ambiguous hint for common supertype of %s and %s%n    %s and %s"),
    M242("either init method %s is static or op  is not %s"),
    M243("%s op defined in %s has already been defined in %s"),
    M244(WARNING,"%s could be used as entries are consecutibe"),
    M245("Unknown directive = %s"),

    M247("inner class %s must be different from %s %s"),
    M248("ELSE does not match an IF or TRY op"),
    M249("structured op(s) missing; level at end is %d"),
    M250(INFO,"the following own virtual method(s) are used but not found in class (but may be in super class or interface)%n    %s"),
    M251(WARNING,"own static method %s not found (but may be in super class)"),
    M252("own init method %s not found"),
    M253("illegal number of dimensions %d; must be in range [0,%d]"),
    M254("%s is used in a macro after a mulit-line op"),
    M255("cannot mix absolute and relative local variables"),
    M256("%s has %d entries, maximum possible is %d"),
    M257("argument count %d is not in range [0,%d]"),
    M258(WARNING,"%s is a reserved word and cannot be a Java Id"),
    M259("dynamic constant is %s but %s expected"),
    M260("return type (%s) of invoke bootstrap method is not ( a known subtype of ) CallSite or Object"),
    M261("enclosing class name(%s) is not a prefix of class name(%s)"),
    M262("%s cannot be overridden"),
    M263(ENDINFO,"%s is deprecated in version %s"),
    M264("structured op mismatch: index %d in label stack is not in  range [0,%d]"),
    M265("structured op mismatch: label stack is empty"),
    M266("Label %s not defined; used in%n%s"),
    M267("%s has %d simple ops which exceeds maximum of %d"),
    M268("enclosing instance has already been defined%n   %s"),
    M269("component %s description %s differs from field description %s"),
    M270(WARNING,"%s directive missing but assumed"),
    M271("incomplete quoted string %s"),
    M272(WARNING,"Label %s not used - defined in line:%n  %s"),
    M273("no Jynx directives in file!"),
    M274(SEVERE,"duplicate: %s has the same opcode(%d) as %s"),
    M275("descriptor '%s' for %s must start with '()'"),
    M276("empty element in  array"),
    M277("unexpected reserved word %s found"),
    M278("NaN type %#x is not in (0,%#x)"),
    M279(WARNING,"empty catch block - from label equals to label"),
    M280(BLANK,"program terminated because of severe error(s)"),
    M281("local variables [%s ] are in a frame but not written"),
    M282("element %s (%c) at top of stack is not one of %s"),
    M283("class is not a %s but %s is %s"),
    M284("label %s is not (yet?) known"),
    M285(SEVERE,"magic number is %#x; should be 0xcafebabe"),
    M286(SEVERE,"%s is not (a known) class"),
    M287(WARNING,"JVM version %s is not supported by the version of ASM used; %s substituted"),
    M288(SEVERE,"JVM version %s is not supported by the version of ASM used; maximum version is %s "),
    M289("A nest member has already been defined"),
    M290(BLANK,";%s  %s -> %s"),
    M291(BLANK,";%s  %s = %s"),
    M292(BLANK,"%s  +%s"),
    M293(LINE,"print options = %s"),
    M294(LINE,"%s = %s"),
    M295("%s is invalid: %s"),
    M296(WARNING,"method %s%s not added as contains errors"),
    M297(ENDINFO,"package %s used in %s is not in %s (OK if package not in module)"),
    M298(WARNING,"%s of %s failed"),
    M299("Unexpected %s %s removed"),
    M300("required %s is missing"),
    M302("%s is null or has different feature requirement than %s"),
    M303("Enclosing method cannot be in enclosed class"),
    M304("Nest host already defined%n  %s"),
    M305(ENDINFO,"it is preferred that name is separated from owner by '%c' not '%c'"),
    M306("nested class have different owners; class = %s token = %s"),
    M307(WARNING,"parameters [%s ] are overwritten"),

    M313("final class cannot have %s"),
    M314(SEVERE,"only one parameter translater allowed"),
    M315(SEVERE,"only one owner translater allowed"),
    M316(SEVERE,"only one label tester allowed"),
    M317("number of macro ops exceeds maximum of %d for %s"),
    M318(SEVERE,"op %s is not a valid op name"),
    M319("%s is an interface and so '%c' must be prepended to %s"),
    M320(ENDINFO,"occurences of %s have been ignored: %s"),
    M321("%d does not in [0,%d]"),
    M322("use of %s is not supported: %s"),
    //
    M327(INFO,"added: %s %s"),
    //
    M335("index (%d) is not a current try index [0,%d]"),

    M362("expected arg %s but was %s"),

    M370("Type annotations not allowed for Module"),
    M394("END OF CLASS HEADER - SHOULD NOT APPEAR!; %s"),
    
    M400(ENDINFO,"unable to find %s %s because of %s"),
    M401(ENDINFO,"%s is a contextual reserved word"),
    M402("cannot insert end_token"),
    M403(WARNING,"(redundant?) checkcast or hint needed if %s is subtype of %s"),
    M404(WARNING,"(redundant?) checkcasts or hint needed to obtain common supertype of%n    %s and %s"),
    M405("%s has different type %s from previous %s at line %d"),
    M406("method %s in %s class must be %s, not %s and have no parameters"),
    M407(ENDINFO,"%s %s is deprecated"),
    M408("expected %s but found %s"),
    M409("illegal operation on END_TOKEN"),
    M410("cannot amend quoted token"),
    M411("type %s not found"),
    M412("%s not supported"),
    
    M901("unknown ASM type %s as it starts with '%c'"),
    M902("unknown ASM stack frame type (%d)"),
    M903("unknown class %s for ASM frametype"),

    M905("unexpected StringState %s"),
    M906("unknown ASM type - %s"),
    M907("unknown directive %s for context %s"),
    M908("unexpected Op %s in this instruction"),
    M909("unknown ASM Node %s in instruction list"),
    M910("indent depth is now negative"),
    
    // for debugging
    M990(BLANK,"%s"),
    M993(INFO,"%s"),
    M995(WARNING,"%s"),
    M997("%s"),
    M999(SEVERE,"%s"),
    ;

    private final LogMsgType logtype;
    private final String format;
    private final String msg;

    private Message(LogMsgType logtype, String format) {
        this.logtype = logtype;
        this.format = logtype.prefix(name()) + format;
        this.msg = format;
    }
    
    
    private Message(String format) {
        this(ERROR,format);
    }

    public String format(Object... objs) {
        return String.format(format,objs);
    }

    public String getFormat() {
        return msg;
    }
    
    public LogMsgType getLogtype() {
        return logtype;
    }
    
}
