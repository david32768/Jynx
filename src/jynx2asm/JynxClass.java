package jynx2asm;

import java.io.File;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static jynx.Directive.dir_comment;
import static jynx.Directive.dir_module;
import static jynx.Directive.end_comment;
import static jynx.Global.*;
import static jynx.GlobalOption.BASIC_VERIFIER;
import static jynx.GlobalOption.SIMPLE_VERIFIER;
import static jynx.Message.*;

import asm.ContextDependent;
import asm.JynxClassHdr;
import asm.JynxCodeHdr;
import asm.JynxComponentNode;
import asm.JynxFieldNode;
import asm.JynxMethodNode;
import asm.JynxModule;
import com.github.david32768.jynx.Main.MainOption;
import jvm.JvmVersion;
import jynx.ClassType;
import jynx.Directive;
import jynx.Global;
import jynx.GlobalOption;
import jynx.LogAssertionError;
import jynx.SevereError;
import jynx.State;
import jynx2asm.ops.JynxOps;

public class JynxClass implements ContextDependent {

    private final JynxScanner js;
    private final String file_source;
    private final String defaultSource;
    
    private JvmVersion jvmVersion;
    private ObjectLine<String> source;

    private State state;

    private JynxClassHdr jclasshdr;
    private JynxComponentNode jcompnode;
    private JynxFieldNode jfieldnode;
    private JynxMethodNode jmethodnode;
    private JynxCodeHdr jcodehdr;
    private JynxModule jmodule;
    
    private ContextDependent sd;
    
    private final Map<Directive,Line> unique_directives;
    private JynxOps opmap;
    

    private JynxClass(String file_source, JynxScanner js) {
        this.js = js;
        this.file_source = file_source;
        int index = file_source.lastIndexOf(File.separatorChar);
        this.defaultSource = file_source.substring(index + 1);
        this.source = null;
        this.unique_directives = new HashMap<>();
        this.sd = this;
    }

    public static byte[] getBytes(String default_source, JynxScanner lines) {
        try {
            JynxClass jclass =  new JynxClass(default_source, lines);
            boolean ok = jclass.assemble();
            if (!ok) {
                return null;
            }
            return jclass.toByteArray();
        } catch (RuntimeException rtex) {
            if (OPTION(GlobalOption.__PRINT_STACK_TRACES)) {
                rtex.printStackTrace();;
            }
            LOG(M123,default_source,rtex); // "compilation of %s failed because of %s"
            return null;
        }
    }
    
    private boolean assemble() {
        int instct = 0;
        int labct = 0;
        int dirct = 0;
        while (js.hasNext()) {
            try {
                Line line = js.next();
                Directive dir;
                if (line.isDirective()) {
                    Token token = line.firstToken();
                    dir = token.asDirective();
                    dirct++;
                } else {
                    dir = Directive.state_opcode;
                    if (line.isLabel()) {
                        ++labct;
                    } else {
                        instct++;
                    }
                }
                state = dir.visit(this,state);
            } catch (IllegalArgumentException ex) {
                LOG(ex);
                js.skipTokens();    // use js as may not be original line
            } catch (SevereError | IllegalStateException ex) {
                LOG(ex);
                return false;
            } catch (RuntimeException ex) {
                LOG(ex);
                throw ex;
            }
        }
        // "instructions = %d labels = %d directives = %d pre_comments = %d"
        LOG(M111, instct, labct, dirct - 1,js.getPreCommentsCount());
        // dirct - 1 as .end class is internal
        assert state == State.END_CLASS;
        boolean success = END_MESSAGES(getClassName());
        return success;
    }

    public String getClassName() {
        return jclasshdr.getClassName();
    }

    private void visitJvmVersion(JvmVersion jvmversion) {
        if (jvmVersion != null) {
            throw new IllegalStateException();
        }
        this.jvmVersion = jvmversion;
        Global.resolveAmbiguity(SIMPLE_VERIFIER, BASIC_VERIFIER);
        Global.setJvmVersion(jvmversion);
        if (!OPTIONS().isEmpty()) {
            LOG(M88, OPTIONS());  // "options = %s"
        }
        this.opmap = JynxOps.getInstance(!OPTION(GlobalOption.JVM_OPS_ONLY),jvmVersion);
    }
    
    private void setOptions(Line line) {
        while (true) {
            Token token = line.nextToken();
            if (token == Token.END_TOKEN) {
                break;
            }
            Optional<GlobalOption> option = GlobalOption.optInstance(token.toString());
            if (option.isPresent() && option.get().isRelevent(MainOption.ASSEMBLY)) {
                boolean added = ADD_OPTION(option.get());
                if (added && option.get() == GlobalOption.DEBUG) {
                    ADD_OPTION(GlobalOption.__EXIT_IF_ERROR);
                    ADD_OPTION(GlobalOption.__PRINT_STACK_TRACES);
                }
            } else {
                LOG(M105,token); // "unknown option %s - ignored"
            }
        }
    }
    
    private void setJvmVersion(Line line) {
        String verstr = line.nextToken().asString();
        setOptions(line);
        visitJvmVersion(JvmVersion.getVersionInstance(verstr));
    }

    public void defaultVersion(Directive dir) {
        // "%s %s assumed"
        LOG(M143,Directive.dir_version,JvmVersion.DEFAULT_VERSION);
        visitJvmVersion(JvmVersion.DEFAULT_VERSION);
    }

    @Override
    public void setSource(Line line) {
        if (jclasshdr != null) {
            throw new IllegalStateException();
        }
        this.source = new ObjectLine<>(line.lastToken().asString(),line);
    }

    private void setMacroLib(Line line) {
        String libname = line.nextToken().asString();
        line.noMoreTokens();
        opmap.addMacroLib(libname);
    }
    
    public void setStart(Directive dir) {
        Line line = js.getLine();
        dir.checkUnique(unique_directives, line);
        switch(dir) {
            case dir_version:
                setJvmVersion(line);
                break;
            case dir_source:
                setSource(line);
                break;
            case dir_macrolib:
                setMacroLib(line);
                break;
            default:
                // "unknown directive %s for context %s"
                throw new LogAssertionError(M907,dir,State.START_BLOCK);
        }
    }
    
    public void setClass(Directive dir) {
        ClassType classtype = ClassType.of(dir);
        LOG(M89, file_source,jvmVersion); // "file = %s version = %s"
        jclasshdr = JynxClassHdr.getInstance(jvmVersion, source, defaultSource, js.getLine(), classtype);
        Global.setClassName(jclasshdr.getClassName());
        state = State.getState(classtype);
        sd = jclasshdr;
    }

    @Override
    public void visitDirective(Directive dir, JynxScanner js) {
        Line line = js.getLine();
        dir.checkUnique(unique_directives, line);
        visitCommonDirective(dir, line, js);
    }

    public void setCommon(Directive dir) {
        if (dir == dir_comment) {
            LOGGER().pushContext();
            js.skipNested(dir_comment, end_comment,EnumSet.noneOf(Directive.class));
            LOGGER().popContext();
        } else {
            sd.visitDirective(dir, js);
        }
    }
    
    public void setHeader(Directive dir) {
        jclasshdr.visitDirective(dir, js);
    }
    
    public void endHeader(Directive dir) {
        assert dir == null;
        jclasshdr.endHeader();
        sd = null;
    }

    public void setComponent(Directive dir) {
        assert dir == Directive.dir_component;
        jcompnode = jclasshdr.getJynxComponentNode(js.getLine());
        sd = jcompnode;
        LOGGER().pushContext();
    }
    
    public void endComponent(Directive dir) {
        if (jcompnode == null) {
            throw new IllegalStateException();
        }
        jcompnode.visitEnd(jclasshdr,dir);
        jcompnode = null;
        sd = null;
        LOGGER().popContext();
    }
    
    public void setField(Directive dir) {
        assert dir == Directive.dir_field;
        jfieldnode = jclasshdr.getJynxFieldNode(js.getLine());
        sd = jfieldnode;
        LOGGER().pushContext();
    }

    public void endField(Directive dir) {
        if (jfieldnode == null) {
            throw new IllegalStateException();
        }
        jfieldnode.visitEnd(dir);
        jfieldnode = null;
        sd = null;
        LOGGER().popContext();
    }


    public void setMethod(Directive dir) {
        switch(dir) {
            case dir_method:
                if (jmethodnode != null) {
                    throw new IllegalStateException();
                }
                Line line = js.getLine();
                jmethodnode = jclasshdr.getJynxMethodNode(line);
                sd = jmethodnode;
                LOGGER().pushContext();
                break;
            default:
                jmethodnode.visitDirective(dir, js);
        }
    }

    public void setCode(Directive dir) {
        if (jcodehdr == null) {
            jcodehdr = jmethodnode.getJynxCodeHdr(js,opmap);
            if (jcodehdr == null) {
                js.skipTokens();
                return;
            }
            jcodehdr.visitCode();
            sd = jcodehdr;
        }
        if (dir == null) {
            return;
        }
        jcodehdr.visitDirective(dir, js);
    }
    
    public void endMethod(Directive dir) {
        if (dir == null) {
            LOG(M270, Directive.end_method); // "%s directive missing but assumed"
        }
        boolean ok;
        if (jmethodnode.isAbstractOrNative()) {
            ok = true;
        } else {
            if (jcodehdr == null) {
                LOG(M46,jmethodnode.getName()); // "method %s has no body"
                ok = false;
            } else {
                ok = jcodehdr.visitEnd();
            }
        }
        if (ok) {
            jclasshdr.acceptMethod(jmethodnode);
        }
        jmethodnode = null;
        jcodehdr = null;
        sd = null;
        LOGGER().popContext();
    }

    public void setModule(Directive dir) {
        Line line = js.getLine();
        if (dir == dir_module) {
            jmodule = JynxModule.getInstance(line,jvmVersion);
        } else {
            jmodule.visitDirective(dir, line);
        }
    }
    
    public void endModule(Directive dir) {
        assert dir == Directive.end_module;
        assert jmodule != null;
        jmodule.visitEnd();
        jclasshdr.acceptModule(jmodule);
    }
    
    public void endClass(Directive dir) {
        if (js.getLine() != null) {
            LOG(M240,Directive.end_class); // "%s is for internal use only"
        }
        int errct = LOGGER().numErrors();
        if (errct != 0) {
            return;
        }
        jclasshdr.visitEnd();
    }
    
    public byte[] toByteArray() {
        if (LOGGER().numErrors() != 0) {
            return null;
        }
        return jclasshdr.toByteArray();
    }

}
