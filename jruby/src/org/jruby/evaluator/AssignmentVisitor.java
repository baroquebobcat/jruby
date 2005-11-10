/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2003-2004 Thomas E Enebo <enebo@acm.org>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.evaluator;

import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.ast.CallNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.Node;
import org.jruby.ast.visitor.AbstractVisitor;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 */
public class AssignmentVisitor extends AbstractVisitor {
    private IRuby runtime;
    private ThreadContext threadContext;
    private IRubyObject self;

    private IRubyObject value;
    private boolean check;
    private IRubyObject result;

    public AssignmentVisitor(IRuby runtime, IRubyObject self) {
        this.runtime = runtime;
        this.self = self;
        this.threadContext = runtime.getCurrentContext();
    }

    public IRubyObject assign(Node node, IRubyObject aValue, boolean aCheck) {
        this.value = aValue;
        this.check = aCheck;

        acceptNode(node);

        return result;
    }

	/**
	 * @see AbstractVisitor#visitNode(Node)
	 */
	protected Instruction visitNode(Node iVisited) {
		assert false;
		return null;
	}

	/**
	 * @see AbstractVisitor#visitCallNode(CallNode)
	 */
	public Instruction visitCallNode(CallNode iVisited) {
		EvaluateVisitor evaluator = EvaluateVisitor.createVisitor();

		IRubyObject receiver = evaluator.eval(self.getRuntime(), self, iVisited
				.getReceiverNode());

        if (iVisited.getArgsNode() == null) { // attribute set.
            receiver.callMethod(iVisited.getName(), new IRubyObject[] {value}, CallType.NORMAL);
        } else { // element set
            RubyArray args = (RubyArray) evaluator.eval(self.getRuntime(),
                    self, iVisited.getArgsNode());
            args.append(value);
            receiver.callMethod(iVisited.getName(), args.toJavaArray(), CallType.NORMAL);
        }
		return null;
	}

	/**
	 * @see AbstractVisitor#visitClassVarAsgnNode(ClassVarAsgnNode)
	 */
	public Instruction visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
		threadContext.getRubyClass().setClassVar(iVisited.getName(), value);
		return null;
	}

	/**
	 * @see AbstractVisitor#visitClassVarDeclNode(ClassVarDeclNode)
	 */
	public Instruction visitClassVarDeclNode(ClassVarDeclNode iVisited) {
		if (runtime.getVerbose().isTrue()
				&& threadContext.getRubyClass().isSingleton()) {
			runtime.getWarnings().warn(iVisited.getPosition(),
					"Declaring singleton class variable.");
		}
		threadContext.getRubyClass().setClassVar(iVisited.getName(), value);
		return null;
	}

	/**
	 * @see AbstractVisitor#visitConstDeclNode(ConstDeclNode)
	 */
	public Instruction visitConstDeclNode(ConstDeclNode iVisited) {
		threadContext.getRubyClass().defineConstant(iVisited.getName(), value);
		return null;
	}

	/**
	 * @see AbstractVisitor#visitDAsgnNode(DAsgnNode)
	 */
	public Instruction visitDAsgnNode(DAsgnNode iVisited) {
		threadContext.getCurrentDynamicVars().set(iVisited.getName(), value);
		return null;
	}

	/**
	 * @see AbstractVisitor#visitGlobalAsgnNode(GlobalAsgnNode)
	 */
	public Instruction visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
		runtime.getGlobalVariables().set(iVisited.getName(), value);
		return null;
	}

	/**
	 * @see AbstractVisitor#visitInstAsgnNode(InstAsgnNode)
	 */
	public Instruction visitInstAsgnNode(InstAsgnNode iVisited) {
		self.setInstanceVariable(iVisited.getName(), value);
		return null;
	}

	/**
	 * @see AbstractVisitor#visitLocalAsgnNode(LocalAsgnNode)
	 */
	public Instruction visitLocalAsgnNode(LocalAsgnNode iVisited) {
		runtime.getCurrentContext().getCurrentScope().setValue(iVisited.getCount(), value);
		return null;
	}

	/**
	 * @see AbstractVisitor#visitMultipleAsgnNode(MultipleAsgnNode)
	 */
	public Instruction visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
		if (!(value instanceof RubyArray)) {
			value = RubyArray.newArray(runtime, value);
		}
		result = threadContext
				.mAssign(self, iVisited, (RubyArray) value, check);
		return null;
	}
}