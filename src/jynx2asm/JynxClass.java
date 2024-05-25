package jynx2asm;

import java.io.File;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static jynx.ClassType.MODULE_CLASS;
import static jynx.ClassType.PACKAGE;
import static jynx.Directive.dir_comment;
import static jynx.Directive.dir_module;
import static jynx.Directive.end_comment;
import static jynx.Global.*;
import static jynx.Message.*;
import static jynx2asm.NameDesc.CLASS_NAME;

import asm.ContextDependent;
import asm.JynxClassHdr;
import asm.JynxClassNode;
import asm.JynxCodeHdr;
import asm.JynxComponentNode;
import asm.JynxFieldNode;
import asm.JynxMethodNode;
import asm.JynxModule;
import jvm.AccessFlag;
import jvm.Constants;
import jvm.Feature;
import jvm.JvmVersion;
import jynx.Access;
import jynx.ClassType;
import jynx.Directive;
import jynx.Global;
import jynx.GlobalOption;
import jynx.LogAssertionError;
import jynx.MainOption;
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

    private JynxClassNode jclassnode;
    private JynxClassHdr jclasshdr;
    private JynxComponentNode jcompnode;
    private JynxFieldNode jfieldnode;
    private JynxMethodNode jmethodnode;
    private JynxCodeHdr jcodehdr;
    private JynxModule jmodule;
    
    private ContextDependent sd;
    
    private final Map<Directive,Line> unique_directives;
    private JynxOps opmap;
    

    private JynxClass(String file_source, String default_source, JynxScanner js) {
        this.js = js;
        this.file_source = file_source;
        this.defaultSource = default_source;
        this.source = null;
        this.unique_directives = new HashMap<>();
        this.sd = this;
    }

    public static byte[] getBytes(String file_source, JynxScanner lines) {
        int index = file_source.lastIndexOf(File.separatorChar);
        String default_source = file_source.substring(index + 1);
        return getBytes(file_source, default_source, lines);
    }
    
    public static byte[] getBytes(String source, String default_source, JynxScanner lines) {
        try {
            JynxClass jclass =  new JynxClass(source, default_source, lines);
            boolean ok = jclass.assemble();
            if (!ok) {
                return null;
            }
            return jclass.toByteArray();
        } catch (RuntimeException rtex) {
            if (OPTION(GlobalOption.__PRINT_STACK_TRACES)) {
                rtex.printStackTrace();;
            }
            LOG(M123, source, rtex); // "compilation of %s failed because of %s"
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
        boolean success = END_MESSAGES(jclassnode.getClassName());
        return success;
    }

    private void visitJvmVersion(JvmVersion jvmversion) {
        if (jvmVersion != null) {
            throw new IllegalStateException();
        }
        this.jvmVersion = jvmversion;
        Global.setJvmVersion(jvmversion);
        if (!OPTIONS().isEmpty()) {
            LOG(M88, OPTIONS());  // "options = %s"
        }
        this.opmap = JynxOps.getInstance(jvmVersion);
    }
    
    private void setOptions(Line line) {
        while (true) {
            Token token = line.nextToken();
            if (token.isEndToken()) {
                break;
            }
            Optional<GlobalOption> option = GlobalOption.optInstance(token.asString());
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

    private Access getAccess(Line line, ClassType classtype, JvmVersion jvmversion) {
        String cname;
        EnumSet<AccessFlag> flags;
        switch (classtype) {
            case MODULE_CLASS:
                flags = EnumSet.noneOf(AccessFlag.class); // read in JynxModule
                cname = Constants.MODULE_CLASS_NAME.stringValue();
                break;
            case PACKAGE:
                flags = line.getAccFlags();
                cname = line.nextToken().asName();
                CLASS_NAME.validate(cname);
                cname += "/" + Constants.PACKAGE_INFO_NAME.stringValue();
                jvmversion.checkSupports(Feature.package_info);
                break;
            default:
                flags = line.getAccFlags();
                cname = line.nextToken().asName();
                CLASS_NAME.validate(cname);
                break;
        }
        line.noMoreTokens();
        flags.addAll(classtype.getMustHave4Class(jvmversion)); 
        Access accessname = Access.getInstance(flags, jvmversion, cname, classtype);
        accessname.check4Class();
        return accessname;
    }
    
    public void setClass(Directive dir) {
        ClassType classtype = ClassType.of(dir);
        LOG(M89, file_source,jvmVersion); // "file = %s version = %s"
        Access accessname = getAccess(js.getLine(), classtype, jvmVersion);
        jclassnode = JynxClassNode.getInstance(accessname);
        jclasshdr = jclassnode.getJynxClassHdr(source, defaultSource);
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
        jclassnode.acceptClassHdr(jclasshdr);
        jclasshdr = null;
        sd = null;
    }

    public void setComponent(Directive dir) {
        assert dir == Directive.dir_component;
        jcompnode = jclassnode.getJynxComponentNode(js.getLine());
        sd = jcompnode;
        LOGGER().pushContext();
    }
    
    public void endComponent(Directive dir) {
        if (jcompnode == null) {
            throw new IllegalStateException();
        }
        jclassnode.acceptComponent(jcompnode, dir);
        jcompnode = null;
        sd = null;
        LOGGER().popContext();
    }
    
    public void setField(Directive dir) {
        assert dir == Directive.dir_field;
        jfieldnode = jclassnode.getJynxFieldNode(js.getLine());
        sd = jfieldnode;
        LOGGER().pushContext();
    }

    public void endField(Directive dir) {
        if (jfieldnode == null) {
            throw new IllegalStateException();
        }
        jclassnode.acceptField(jfieldnode,dir);
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
                jmethodnode = jclassnode.getJynxMethodNode(line);
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
            jclassnode.acceptMethod(jmethodnode);
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
        jclassnode.acceptModule(jmodule);
    }
    
    public void endClass(Directive dir) {
        if (js.getLine() != null) {
            LOG(M240,Directive.end_class); // "%s is for internal use only"
        }
        int errct = LOGGER().numErrors();
        if (errct != 0) {
            return;
        }
        jclassnode.visitEnd();
    }
    
    public byte[] toByteArray() {
        if (LOGGER().numErrors() != 0) {
            return null;
        }
        return jclassnode.toByteArray();
    }

}
