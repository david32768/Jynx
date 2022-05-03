package jynx;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

import static jvm.AttributeName.*;
import static jynx.Global.*;
import static jynx.Message.*;
import static jynx.State.*;

import jvm.Feature;
import jvm.JvmVersioned;
import jvm.JvmVersionRange;
import jynx2asm.JynxClass;
import jynx2asm.Line;

public enum Directive implements JvmVersioned {
    //  dir_x(after_state,before_states[,feature])

    dir_version(START_BLOCK, EnumSet.of(START)),
    dir_source(START_BLOCK, EnumSet.of(START_BLOCK, END_START), SourceFile),
    dir_macrolib(START_BLOCK, EnumSet.of(START_BLOCK, END_START)),
    
    dir_class(CLASSHDR, EnumSet.of(START_BLOCK, END_START)),
    dir_interface(CLASSHDR, EnumSet.of(START_BLOCK, END_START)),
    dir_enum(CLASSHDR, EnumSet.of(START_BLOCK, END_START)),
    dir_module(MODULEHDR, EnumSet.of(START_BLOCK, END_START), Module),
    dir_record(CLASSHDR, EnumSet.of(START_BLOCK, END_START),Record),
    dir_package(PACKAGEHDR, EnumSet.of(START_BLOCK, END_START)),
    dir_define_annotation(CLASSHDR, EnumSet.of(START_BLOCK, END_START), AnnotationDefault),

    dir_super(HEADER, EnumSet.of(CLASSHDR, PACKAGEHDR)),
    dir_implements(HEADER, EnumSet.of(CLASSHDR)),
    dir_debug(HEADER, EnumSet.of(CLASSHDR,MODULEHDR), SourceDebugExtension),
    
    dir_signature(COMMON, EnumSet.of(CLASSHDR, FIELD_BLOCK, METHOD_BLOCK, COMPONENT_BLOCK), Signature),
    
    dir_inner_class(HEADER, EnumSet.of(CLASSHDR,MODULEHDR),InnerClasses),
    dir_inner_interface(HEADER, EnumSet.of(CLASSHDR,MODULEHDR),InnerClasses),
    dir_inner_enum(HEADER, EnumSet.of(CLASSHDR,MODULEHDR),InnerClasses),
    dir_inner_record(HEADER, EnumSet.of(CLASSHDR,MODULEHDR),InnerClasses),
    dir_inner_define_annotation(HEADER, EnumSet.of(CLASSHDR,MODULEHDR),InnerClasses),

    dir_nesthost(HEADER, EnumSet.of(CLASSHDR), NestHost),
    dir_nestmember(HEADER, EnumSet.of(CLASSHDR), NestMembers),
    dir_permittedSubclass(HEADER, EnumSet.of(CLASSHDR), PermittedSubclasses),
    dir_enclosing_method(HEADER, EnumSet.of(CLASSHDR), EnclosingMethod),
    dir_enclosing_class(HEADER, EnumSet.of(CLASSHDR), EnclosingMethod),
    dir_hints(HEADER, EnumSet.of(CLASSHDR)),
    
    dir_annotation(COMMON, EnumSet.of(CLASSHDR,FIELD_BLOCK, METHOD_BLOCK, MODULEHDR,COMPONENT_BLOCK,PACKAGEHDR),
            RuntimeInvisibleAnnotations),

    dir_param_type_annotation(COMMON, EnumSet.of(CLASSHDR, METHOD_BLOCK, MODULEHDR, PACKAGEHDR),
            Feature.type_annotations),
    dir_extends_type_annotation(COMMON, EnumSet.of(CLASSHDR, MODULEHDR, PACKAGEHDR),
            Feature.type_annotations),
    dir_param_bound_type_annotation(COMMON, EnumSet.of(CLASSHDR, METHOD_BLOCK, MODULEHDR, PACKAGEHDR),
            Feature.type_annotations),
    dir_field_type_annotation(COMMON, EnumSet.of(FIELD_BLOCK, COMPONENT_BLOCK),
            Feature.type_annotations),
    dir_return_type_annotation(COMMON, EnumSet.of(METHOD_BLOCK),
            Feature.type_annotations),
    dir_receiver_type_annotation(COMMON, EnumSet.of(METHOD_BLOCK),
            Feature.type_annotations),
    dir_formal_type_annotation(COMMON, EnumSet.of(METHOD_BLOCK),
            Feature.type_annotations),
    dir_throws_type_annotation(COMMON, EnumSet.of(METHOD_BLOCK),
            Feature.type_annotations),
    dir_var_type_annotation(COMMON, EnumSet.of(CODE),
            Feature.type_annotations),
    dir_resource_type_annotation(COMMON, EnumSet.of(CODE),
            Feature.type_annotations),
    dir_instanceof_type_annotation(COMMON, EnumSet.of(CODE),
            Feature.type_annotations),
    dir_new_type_annotation(COMMON, EnumSet.of(CODE),
            Feature.type_annotations),
    dir_newref_type_annotation(COMMON, EnumSet.of(CODE),
            Feature.type_annotations),
    dir_methodref_type_annotation(COMMON, EnumSet.of(CODE),
            Feature.type_annotations),
    dir_cast_type_annotation(COMMON, EnumSet.of(CODE),
            Feature.type_annotations),
    dir_argnew_type_annotation(COMMON, EnumSet.of(CODE),
            Feature.type_annotations),
    dir_argmethod_type_annotation(COMMON, EnumSet.of(CODE),
            Feature.type_annotations),
    dir_argnewref_type_annotation(COMMON, EnumSet.of(CODE),
            Feature.type_annotations),
    dir_argmethodref_type_annotation(COMMON, EnumSet.of(CODE),
            Feature.type_annotations),
    
    dir_default_annotation(METHOD_BLOCK, EnumSet.of(METHOD_BLOCK), AnnotationDefault),
    dir_parameter_annotation(METHOD_BLOCK, EnumSet.of(METHOD_BLOCK), Feature.annotations),

    end_annotation(READ_END, EnumSet.noneOf(State.class)),
    end_annotation_array(READ_END, EnumSet.noneOf(State.class)),

    dir_component(COMPONENT,EnumSet.of(END_CLASSHDR,END_COMPONENT), Record),
    end_component(END_COMPONENT, EnumSet.of(COMPONENT_BLOCK), Record),
    
    dir_field(FIELD, EnumSet.of(END_CLASSHDR, END_FIELD,END_COMPONENT)),
    end_field(END_FIELD, EnumSet.of(FIELD_BLOCK)),
    
    dir_method(METHOD_BLOCK, EnumSet.of(END_CLASSHDR, END_FIELD, END_METHOD)),
    dir_throws(METHOD_BLOCK, EnumSet.of(METHOD_BLOCK),  Exceptions),
    
    dir_parameter(METHOD_BLOCK, EnumSet.of(METHOD_BLOCK), MethodParameters),
    dir_visible_parameter_count(METHOD_BLOCK,EnumSet.of(METHOD_BLOCK), RuntimeVisibleParameterAnnotations),
    dir_invisible_parameter_count(METHOD_BLOCK,EnumSet.of(METHOD_BLOCK), RuntimeInvisibleParameterAnnotations),
    
    dir_catch(CATCH, EnumSet.of(METHOD_BLOCK, CODE), Exceptions),
    dir_except_type_annotation(CATCH_BLOCK, EnumSet.of(CATCH, CATCH_BLOCK),
            RuntimeVisibleTypeAnnotations),
    end_catch(END_CATCH,EnumSet.of(CATCH_BLOCK)),

    dir_limit(CODE, EnumSet.of(METHOD_BLOCK, CODE)),
    dir_line(CODE, EnumSet.of(METHOD_BLOCK, CODE), LineNumberTable),
    dir_print(CODE, EnumSet.of(METHOD_BLOCK, CODE)),
    state_opcode(CODE, EnumSet.of(METHOD_BLOCK, CODE)),
    
    // ASM can calculate stack frames
    // cannot be first line in code; hence no METHOD state
    dir_stack(CODE, EnumSet.of(CODE),StackMapTable), 
    end_stack(CODE, EnumSet.of(CODE),StackMapTable),
    // cannot be first line in code as labels must be defined
    dir_var(CODE, EnumSet.of(CODE), LocalVariableTable),
    // cannot be first line in code; hence no METHOD state
    dir_if(CODE, EnumSet.of(CODE)),
    end_if(CODE, EnumSet.of(CODE)),
    
    end_method(END_METHOD, EnumSet.of(METHOD_BLOCK, CODE)),

    end_array(READ_END, EnumSet.noneOf(State.class)),

    dir_main(MODULE, EnumSet.of(MODULE,END_MODULEHDR), ModulePackages),
    dir_packages(MODULE, EnumSet.of(MODULE,END_MODULEHDR), ModulePackages),
    dir_uses(MODULE, EnumSet.of(MODULE,END_MODULEHDR), Module),
    dir_exports(MODULE, EnumSet.of(MODULE,END_MODULEHDR), Module),
    dir_opens(MODULE, EnumSet.of(MODULE,END_MODULEHDR), Module),
    dir_requires(MODULE, EnumSet.of(MODULE,END_MODULEHDR), Module),
    dir_provides(MODULE, EnumSet.of(MODULE,END_MODULEHDR), Module),

    // used internally to end class, module etc.
    end_class(END_CLASS, EnumSet.of(END_CLASSHDR, END_FIELD, END_METHOD,MODULE,END_MODULEHDR, END_PACKAGEHDR)),
    ;

    private final State after;
    private final EnumSet<State> before;
    private final JvmVersionRange range;

    private Directive(State after, EnumSet<State> before) {
        this(after,before,Feature.unlimited);
    }

    private Directive(State after, EnumSet<State> before, JvmVersioned range) {
        this.after = after;
        this.before = before;
        this.range = range.range();
    }

    @Override
    public JvmVersionRange range() {
        return range;
    }
    
    private boolean isUniqueWithin() {
        switch(this) {
            case dir_source:
            case dir_super:
            case dir_signature:
            case dir_nesthost:
            case dir_enclosing_method:
            case dir_enclosing_class:
            case dir_default_annotation:
            case dir_packages:
            case dir_main:
            case dir_visible_parameter_count:
            case dir_invisible_parameter_count:
                return true;
            default:
                return false;
        }
    }

    public void checkUnique(Map<Directive,Line> unique_directives, Line line) {
        if (isUniqueWithin()) {
            Line linex = unique_directives.putIfAbsent(this, line);
            if (linex != null) {
                throw new LogIllegalStateException(M31,this,linex); // "%s already set in line%n    %s"
            }
        }
    }
    
    public State visit(JynxClass jc, State current) {
        if (after == READ_END) {
            LOG(M136,this);  // "Extraneous directive %s"
            return current;
        }
        if (current == null) {
            current = START;
        }
        if (!before.contains(current)) {
            current = current.changeToValidState(jc, before);
        }
        CHECK_SUPPORTS(this);
        return after.changeToThisState(jc, this)?after:current;
    }

    public boolean isEndDirective() {
        return name().startsWith("end");
    }

    private final static EnumSet<Directive> QUOTED_ARGS = EnumSet.of(dir_debug,dir_signature);
    
    public boolean hasQuotedArg() {
        return QUOTED_ARGS.contains(this);
    }
    
    @Override
    public String toString() {
        String name = name();
        if (name.startsWith("dir_")) {
            return Line.DIRECTIVE_INICATOR + name.substring(4);
        }
        if (name.startsWith("end")) {
            return Line.DIRECTIVE_INICATOR + name;
        }
        return name;
    }

    private static Optional<Directive> getInstance(String dirtoken) {
        for (Directive dir : values()) {
            if (dirtoken.equals(dir.name())) {
                return Optional.of(dir);
            }
        }
        return Optional.empty();
    }

    public static Optional<Directive> getDirInstance(String token) {
        Optional<Directive> dir =  getInstance("dir_" + token);
        if (!dir.isPresent()) {
            dir = getInstance(token);
        }
        return dir;
    }

}
