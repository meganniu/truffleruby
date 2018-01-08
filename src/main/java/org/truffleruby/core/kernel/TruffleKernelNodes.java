/*
 * Copyright (c) 2013, 2018 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.truffleruby.core.kernel;

import java.io.IOException;

import org.jcodings.specific.UTF8Encoding;
import org.truffleruby.Layouts;
import org.truffleruby.builtins.CoreClass;
import org.truffleruby.builtins.CoreMethod;
import org.truffleruby.builtins.CoreMethodArrayArgumentsNode;
import org.truffleruby.builtins.CoreMethodNode;
import org.truffleruby.core.cast.BooleanCastWithDefaultNodeGen;
import org.truffleruby.core.string.StringOperations;
import org.truffleruby.language.RubyNode;
import org.truffleruby.language.RubyRootNode;
import org.truffleruby.language.control.RaiseException;
import org.truffleruby.language.loader.CodeLoader;
import org.truffleruby.language.methods.DeclarationContext;
import org.truffleruby.language.threadlocal.FindThreadAndFrameLocalStorageNode;
import org.truffleruby.language.threadlocal.FindThreadAndFrameLocalStorageNodeGen;
import org.truffleruby.parser.ParserContext;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;

@CoreClass("Truffle::KernelOperations")
public abstract class TruffleKernelNodes {

    @CoreMethod(names = "at_exit", isModuleFunction = true, needsBlock = true, required = 1)
    public abstract static class AtExitSystemNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public Object atExit(boolean always, DynamicObject block) {
            getContext().getAtExitManager().add(block, always);
            return nil();
        }
    }

    @NodeChildren({
            @NodeChild(value = "file", type = RubyNode.class),
            @NodeChild(value = "wrap", type = RubyNode.class)
    })
    @CoreMethod(names = "load", isModuleFunction = true, required = 1, optional = 1)
    public abstract static class LoadNode extends CoreMethodNode {

        @CreateCast("wrap")
        public RubyNode coerceToBoolean(RubyNode inherit) {
            return BooleanCastWithDefaultNodeGen.create(false, inherit);
        }

        @Specialization(guards = "isRubyString(file)")
        public boolean load(VirtualFrame frame, DynamicObject file, boolean wrap,
                @Cached("create()") IndirectCallNode callNode,
                @Cached("create()") BranchProfile errorProfile) {
            if (wrap) {
                throw new UnsupportedOperationException();
            }

            try {
                final RubyRootNode rootNode = getContext().getCodeLoader().parse(
                        getContext().getSourceLoader().load(StringOperations.getString(file)),
                        UTF8Encoding.INSTANCE, ParserContext.TOP_LEVEL,
                        null, true, this);
                final CodeLoader.DeferredCall deferredCall = getContext().getCodeLoader().prepareExecute(
                        ParserContext.TOP_LEVEL, DeclarationContext.topLevel(getContext()), rootNode, null,
                        getContext().getCoreLibrary().getMainObject());
                deferredCall.call(callNode);
            } catch (IOException e) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().loadErrorCannotLoad(file.toString(), this));
            }

            return true;
        }

    }

    @CoreMethod(names = "define_hooked_variable_with_is_defined", isModuleFunction = true, required = 4)
    public abstract static class DefineHookedVariableInnerNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = { "isRubySymbol(name)", "isRubyProc(getter)", "isRubyProc(setter)" })
        public DynamicObject defineHookedVariableInnerNode(DynamicObject name, DynamicObject getter, DynamicObject setter, DynamicObject isDefined) {
            getContext().getCoreLibrary().getGlobalVariables().put(Layouts.SYMBOL.getString(name), getter, setter, isDefined);
            return nil();
        }

    }

    @CoreMethod(names = "frame_local_variable_get", isModuleFunction = true, required = 2)
    public abstract static class GetFrameAndThreadLocalVariable extends CoreMethodArrayArgumentsNode {

        @Child FindThreadAndFrameLocalStorageNode threadLocalNode = FindThreadAndFrameLocalStorageNodeGen.create();

        @Specialization(guards = { "isRubySymbol(name)", "isRubyBinding(binding)" })
        public Object executeGetValue(DynamicObject name, DynamicObject binding) {
            return threadLocalNode.execute(name, Layouts.BINDING.getFrame(binding)).get();
        }

    }

    @CoreMethod(names = "frame_local_variable_set", isModuleFunction = true, required = 3)
    public abstract static class SetFrameAndThreadLocalVariable extends CoreMethodArrayArgumentsNode {

        @Child FindThreadAndFrameLocalStorageNode threadLocalNode = FindThreadAndFrameLocalStorageNodeGen.create();

        @Specialization(guards = { "isRubySymbol(name)", "isRubyBinding(binding)" })
        public Object executeGetValue(DynamicObject name, DynamicObject binding, Object value) {
            threadLocalNode.execute(name, Layouts.BINDING.getFrame(binding)).set(value);
            return value;
        }

    }
}
