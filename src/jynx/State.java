package jynx;

import java.util.EnumSet;
import java.util.function.BiConsumer;

import static jynx.Message.M165;
import jynx2asm.JynxClass;

public enum State {
    
    END_CLASS(JynxClass::endClass),
    END_CLASSHDR(JynxClass::endHeader),
    END_COMPONENT(JynxClass::endComponent),
    END_FIELD(JynxClass::endField),
    END_METHOD(JynxClass::endMethod),
    END_MODULEHDR(JynxClass::endHeader),
    END_START(JynxClass::defaultVersion),
    END_PACKAGEHDR(JynxClass::endHeader),

    COMPONENT_BLOCK(null,END_COMPONENT),
    FIELD_BLOCK(null,END_FIELD),
    METHOD_BLOCK(JynxClass::setMethod),
    START_BLOCK(JynxClass::setStart),

    START(null,END_START),
    CLASSHDR(JynxClass::setClass, END_CLASSHDR),
    COMPONENT(JynxClass::setComponent,COMPONENT_BLOCK),
    FIELD(JynxClass::setField,FIELD_BLOCK),
    CODE(JynxClass::setCode),
    MODULEHDR(JynxClass::setClass, END_MODULEHDR),
    MODULE(JynxClass::setModule),
    PACKAGEHDR(JynxClass::setClass, END_PACKAGEHDR),
    COMMON(JynxClass::setCommon), // state unchanged
    REMOVED(JynxClass::removed),
    HEADER(JynxClass::setHeader), // state unchanged
    READ_END(null),    // END directive is read by previous directive in stream
    ;

    private final BiConsumer<JynxClass,Directive> dirfn;
    private final State next;

    private State(BiConsumer<JynxClass, Directive> dirfn, State next) {
        this.dirfn = dirfn;
        this.next = next;
    }

    private State(BiConsumer<JynxClass, Directive> dirfn) {
        this(dirfn,null);
    }

    
    private State changeStateTo(JynxClass jc, Directive dir) {
        if (dirfn != null) {
            dirfn.accept(jc,dir);
        }
        return this;
    }

    public State changeToValidState(JynxClass jc, EnumSet<State> before) {
        if (next == null) {
            // "Directive in wrong place; Current state = %s%n  Expected state was one of %s"
            throw new LogIllegalStateException(M165,this,before);
        }
        // as enum always goes towards top so cannot loop
        assert next.ordinal() < this.ordinal();
        if (before.contains(next)) {
            return next.changeStateTo(jc,null);
        }
        return next.changeToValidState(jc, before);
    }
    
    public boolean changeToThisState(JynxClass jc,Directive dir) {
        changeStateTo(jc, dir);
        switch(this) {
            case HEADER:
            case COMMON:
            case REMOVED:
                return false;
            default:
                return true;
        }
    }

    public static State getState(ClassType classtype) {
        switch(classtype) {
            case MODULE:
                return MODULEHDR;
            case PACKAGE:
                return PACKAGEHDR;
            default:
                return CLASSHDR;
        }
    }
    
}
