package jynx2asm;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import jvm.AccessFlag;
import jvm.JvmVersion;
import jynx.ClassType;
import jynx.Directive;
import jynx.Global;
import jynx.GlobalOption;
import jynx.LogAssertionError;
import jynx.SevereError;
import jynx.State;

public class JynxClass {

    private final JynxScanner js;
    private final String file_source;
    private final String default_source;
    
    private JvmVersion jvmVersion;
    private String source;

    private State state;

    private JynxClassHdr jclasshdr;
    private JynxComponentNode jcompnode;
    private JynxFieldNode jfieldnode;
    private JynxMethodNode jmethodnode;
    private JynxCodeHdr jcodehdr;
    private JynxModule jmodule;
    
    private ContextDependent sd;
    
    private final Map<Directive,Line> unique_directives;

    private JynxClass(String file_source, JynxScanner js) {
        this.js = js;
        this.file_source = file_source;
        int index = file_source.lastIndexOf(File.separatorChar);
        this.default_source = file_source.substring(index + 1);
        this.source = null;
        this.unique_directives = new HashMap<>();
    }

    public static JynxClass getInstance(String default_source, List<String> lines) {
        Global.resolveAmbiguity(SIMPLE_VERIFIER, BASIC_VERIFIER);
        JynxClass jclass =  new JynxClass(default_source, new JynxScanner(lines));
        jclass.assemble();
        return jclass;
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
            } catch (SevereError lex) {
                return false;
            } catch (IllegalStateException ex) {
                LOG(ex);
                return false;
            } catch (RuntimeException ex) {
                LOG(ex);
                throw ex;
            }
        }
        LOG(M111, instct, labct, dirct - 1); // "instct = %d labct = %d dirct = %d"
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
        Global.setJvmVersion(jvmversion);
        if (!OPTIONS().isEmpty()) {
            LOG(M88, OPTIONS());  // "  options = %s"
        }
    }
    
    private void setOptions(Line line) {
        while (true) {
            Token token = line.nextToken();
            if (token == Token.END_TOKEN) {
                break;
            }
            Optional<GlobalOption> option = GlobalOption.optInstance(token.toString());
            if (option.isPresent()) {
                boolean added = ADD_OPTION(option.get());
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

    private void setSource(Line line) {
        if (jclasshdr != null) {
            throw new IllegalStateException();
        }
        this.source = line.lastToken().asString();
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
            default:
                // "unknown directive %s for context %s"
                throw new LogAssertionError(M907,dir,State.START_BLOCK);
        }
    }
    
    public void setClass(Directive dir) {
        ClassType classtype = ClassType.of(dir);
        boolean usestack = OPTION(GlobalOption.USE_STACK_MAP);
        if (source == null && !usestack) {
            LOG(M143,Directive.dir_source,default_source); // "%s %s assumed"
            source = default_source;
        }
        LOG(M89, file_source,jvmVersion); // "file = %s version = %s"
        jclasshdr = JynxClassHdr.getInstance(jvmVersion, source, js.getLine(), classtype);
        Global.setClassName(jclasshdr.getClassName());
        state = State.getState(classtype);
        sd = jclasshdr;
        if (classtype == ClassType.MODULE) {
            jmodule = JynxModule.getInstance(js,jvmVersion);
        }
    }

    public void setCommon(Directive dir) {
        sd.visitDirective(dir, js);
    }
    
    public void removed(Directive dir) {
        Line line = js.getLine();
        switch(dir) {
            case dir_deprecated:
                // "%s directive is deprecated and removed! Use %s pseudo-access flag"
                LOG(M192,dir,AccessFlag.acc_deprecated);
                break;
            case dir_bytecode:
                // "this %s directive has been replaced by %s"
                LOG(M193,Directive.dir_bytecode,Directive.dir_version);
                line.nextToken();
                break;
            case end:
                String endof = line.nextToken().asString();
                String dirstr = dir.toString().substring(1) + "_" +endof;
                Optional<Directive> newdir = Directive.getDirInstance(dirstr);
                if (newdir.isPresent()) {
                    // "this %s directive has been replaced by %s"
                    LOG(M193,dir,newdir.get());
                } else {
                    LOG(M204,dir,dir);    // "%s has been replaced by %s_xxxxxxx"
                }
                break;
            case dir_annotation:
                endof = line.nextToken().asString();
                dirstr = endof + "_" + dir.toString().substring(1);
                newdir = Directive.getDirInstance(dirstr);
                // "this %s directive has been replaced by %s"
                if (newdir.isPresent()) {
                    // "this %s directive has been replaced by %s"
                    LOG(M193,dir,newdir.get());
                } else {
                    LOG(M189,dir,dir.toString().substring(1));    // "%s has been replaced by .xxxxxxx_%s"
                }
                line.skipTokens();
                break;
            default:
                // "unknown directive %s for context %s"
                throw new LogAssertionError(M907,dir,State.REMOVED);
        }
    }
    
    public void setHeader(Directive dir) {
        jclasshdr.visitDirective(dir, js);
    }
    
    public void endHeader(Directive dir) {
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
        jcompnode.visitEnd();
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
        jfieldnode.visitEnd();
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
            jcodehdr = jmethodnode.getJynxCodeHdr(js);
            if (jcodehdr == null) {
                js.skipTokens();
                return;
            }
            jcodehdr.visitCode();
            sd = jcodehdr;
        }
        jcodehdr.visitDirective(dir, js);
    }
    
    public void endMethod(Directive dir) {
        assert dir == Directive.end_method;
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
        jmodule.visitDirective(dir, js);
    }
    
    public void endClass(Directive dir) {
        if (js.getLine() != null) {
            LOG(M240,Directive.end_class); // "%s is for internal use only"
        }
        int errct = LOGGER().numErrors();
        if (errct != 0) {
            return;
        }
        if (jmodule != null) {
            jmodule.visitEnd();
            jclasshdr.acceptModule(jmodule);
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
