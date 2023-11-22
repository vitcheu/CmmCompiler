package Parser;

import AST.AST;
import AST.DEFINE.*;
import AST.EXPR.*;
import AST.Node;
import AST.NodeList.ListNode;
import AST.STMT.*;
import AST.TYPE.*;
import CompileException.errorHandler;
import Parser.Entity.*;
import compile.Constants;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class Reducer {
    private PrintWriter pw = null;
    private errorHandler handler;
    private AnalysisStack stack;
    private final Parser parser;

    public Reducer(AnalysisStack stack, Parser parser,PrintWriter err) throws FileNotFoundException {
        pw = new PrintWriter(Constants.outputPath + "Syntax/reduce.txt");
        this.parser = parser;
        this.stack = stack;
        this.handler=new errorHandler(err);
    }


    public Node reduce(Production p) {
        switch (p.getPid()) {
            /**
             *
             *
             *
             *
             * 表达式
             *
             *
             *
             *
             */

            /**
             * 主项和常量
             */
            //primary_expr	->	id
            case 139: {
                Token tok = (Token) stack.getSymbol(0);
                ExprNode node = new VariableNode(tok.getLocation(), tok.getImage());
//                pw.println("\ntok="+tok+",node="+node);
                return node;
            }


            //primary_expr	->	(	expr	)
            case 141: {
                return stack.getNode(1);
            }

            //CONSTANT	->	INTEGER
            //CONSTANT	->	CHARACTOR
            //CONSTANT	->	STRING
            //CONSTANT	->	FLOAT
            case 142:
            case 143:
            case 144:
            case 145: {
                Token tok = (Token) stack.getSymbol(0);
                ExprNode node = null;
                if (tok.getValue().equals("INTEGER")) {
                    node = new IntegerNode(tok.getLocation(), tok.getImage());
                }
                if (tok.getValue().equals("CHARACTOR")) {
                    node = new CharNode(tok.getLocation(), tok.getImage());
                }
                if (tok.getValue().equals("STRING")) {
                    node = new StringNode(tok.getLocation(), tok.getImage());
                }
                if (tok.getValue().equals("FLOAT")) {
                    node = new FloatNode(tok.getLocation(), tok.getImage());
                }
//                pw.println("\n"+node);
                return node;
            }

            /**
             * 前置一元运算
             */
            //term	->	(type) unary_expr
            case 118: {
                ExprNode expr = (ExprNode) stack.getNode(0);
                TypeNode type=(TypeNode) stack.getNode(2);
                return new CastNode(stack.getSymbol(3).getLocation(),expr,type);
            }


            //unary_expr,++,unary_expr,
            //unary_expr,--,unary_expr,
            //unary_expr,unary_operator,term,
            case 120:
            case 121:
            case 122: {
                ExprNode unary=(ExprNode)stack.getNode(0);

                Symbol sym = stack.getSymbol(1);
                //++,--
                if (sym.getValue().equals("++") || sym.getValue().equals("--")) {
                    return new PrefixOpNode(sym.getLocation(), sym.getValue(),unary);
                }

                Op opNode=(Op)stack.getNode(1);
                String opstr=opNode.getOp();
                //其他
                if (!opstr.equals("*") && !opstr.equals("&")) {
                    return new UnaryOpNode(opNode.getLocation(), opstr, unary);
                //*
                } else if (opstr.equals("*")) {
                    return new DerefrenceNode(opNode.getLocation(),unary);
                //&
                } else {
                    return new AddressNode(opNode.getLocation(), unary);
                }
            }

            //unary_expr,SIZEOF,unary_expr,
            case 123: {
                return new sizeOfExprNode(stack.getSymbol(1).getLocation(),
                        (ExprNode) stack.getNode(0));
            }

            //unary_expr,SIZEOF,(,type,)
            case 124: {
                return new sizeOfTypeNode(stack.getSymbol(3).getLocation(),
                        (TypeNode) stack.getNode(1));
            }

            //unary_operator->+|-|!|~|*|&

            case 125:
            case 126:
            case 127:
            case 128:
            case 129:
            case 130:

                //opassign_op->+=,-=,*=,/=,%=
            case 50:
            case 146:
            case 147:
            case 148:
            case 149: {
                Token tok = (Token) stack.getSymbol(0);
                return new Op(tok.getLocation(), tok.getValue());
            }

            /**
             * 后置一元运算
             */

            //postfix_expr	->	postfix_expr	[	expr	]
            case 132: {
                ExprNode idx = (ExprNode)stack.getNode(1);
                ExprNode array = (ExprNode) stack.getNode(3);
                if (idx.equals(array)) {
                    System.err.println("idx=" + idx);
                    throw new RuntimeException();
                }
                return new ArrayNode(stack.getSymbol(2).getLocation(), array, idx);
            }

            //postfix_expr	->	postfix_expr	(	)
            case 133: {
                ExprNode postExpr=(ExprNode) stack.getNode(2);
                ArgNode argNode = new ArgNode(stack.getSymbol(1).getLocation());
                return new CallNode(postExpr.getLocation(), postExpr, argNode);
            }

            //postfix_expr	->	postfix_expr	(	args	)
            case 134: {
                ExprNode postExpr=(ExprNode) stack.getNode(3);
                 ArgNode argNode=(ArgNode) stack.getNode(1);
                return new CallNode(postExpr.getLocation(),postExpr, argNode);
            }

            //postfix_expr	->	postfix_expr	.	name
            //postfix_expr	->	postfix_expr	->	name
            case 135:
            case 136: {
                NameNode nameNode = (NameNode) stack.getNode(0);
                String name=nameNode.getName();
                ExprNode expr = (ExprNode) stack.getNode(2);
                Token tok = (Token) stack.getSymbol(1);
                return new MemberNode(tok.getLocation(), tok.getValue(), expr,
                        name);
            }

            //postfix_expr	->	postfix_expr	++	| --
            case 137:
            case 138: {
                ExprNode expr = (ExprNode) stack.getNode(1);
                Symbol sym = stack.getSymbol(0);
                return new PostfixOpNode(expr.getLocation(), sym.getValue(), expr);
            }

            //args	->	expr
            case 151: {
                ExprNode expr=(ExprNode) stack.getNode(0);
                ArgNode argNode = new ArgNode(expr.getLocation());
                argNode.addArgument(expr);
                return argNode;
            }

            //args	->	args	comma	expr
            case 152: {
                ArgNode argNode = (ArgNode) stack.getNode(2);
                argNode.addArgument((ExprNode) stack.getNode(0));
                return argNode;
            }


            /**
             *
             *
             * 形如A->B的产生式
             *
             *
             *
             */

//            top_decl,defun
//            top_decl,defvars
//            top_decl,defconst
//            top_decl,defstruct
//            top_decl,TYPEDEF
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:


            case 19:
            case 39:

            case 60:
            case 61:
            case 63:

            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:

            case 92:
            case 93:
            case 95:
            case 97:
            case 99:

            case 102:
            case 107:

            case 110:
            case 113:
            case 117:
            case 119:

            case 131:

            case 140:
                //optional_for_expr	->	expr
            case 78: {
                return stack.getNode(0);
            }


            /**
             *
             *
             *
             * 二元表达式
             *
             *
             *
             *
             */
            //expr->l,op,r
            //||
            case 96:
                //&&
            case 98:

                //==,!=
            case 100:
            case 101:

                //<><=>=
            case 103:
            case 104:
            case 105:
            case 106:

                //>><<
            case 108:
            case 109:

                //+-
            case 111:
            case 112:

                //*/%
            case 114:
            case 115:
            case 116: {
                ExprNode left=(ExprNode) stack.getNode(2);
                ExprNode right=(ExprNode) stack.getNode(0);
                Token tok = (Token) stack.getSymbol(1);
                return new BinaryOpNode(tok.getLocation(),
                        left,right, tok.getValue());
            }

            /**
             * 三元表达式
             */
            //conditional_expr	->	logical_or_expr	?	expr	:	conditional_expr
            case 94: {
                ExprNode c=(ExprNode) stack.getNode(4),
                        t=(ExprNode) stack.getNode(2),
                        e=(ExprNode) stack.getNode(0);
                return new ConditionNode(c.getLocation(), c,t,e);
            }

            /**
             * 赋值表达式
             */
            //expr	->	term	=	expr
            case 90: {
                ExprNode lhs=(ExprNode) stack.getNode(2),
                        rhs=(ExprNode) stack.getNode(0);
                return new AssignNode(lhs.getLocation(), lhs, rhs);
            }
            //expr	->	term	opassign_op 	expr
            case 91: {
                ExprNode lhs=(ExprNode) stack.getNode(2),
                        rhs=(ExprNode) stack.getNode(0);
                String op=((Op)stack.getNode(1)).getOp();
                return new OpAssignNode(lhs.getLocation(),lhs,rhs,op);
            }


            /**
             *
             *
             *
             *
             * 语句(stmt)
             *
             *
             *
             *
             */

            /**
             * if语句
             */
            //if_stmt	->	if	(	expr	)	stmt
            case 72: {
                ExprNode condition = (ExprNode) stack.getNode(2);
                StmtNode stmt = (StmtNode) stack.getNode(0);
                return new IfNode(condition.getLocation(), condition, stmt, null);
            }

            //if_stmt	->	if	(	expr	)	stmt	else	stmt
            case 73: {
                ExprNode condition = (ExprNode) stack.getNode(4);
                StmtNode thenStmt = (StmtNode) stack.getNode(2),
                        elseStmt = (StmtNode) stack.getNode(0);
                return new IfNode(stack.getSymbol(6).getLocation(), condition, thenStmt, elseStmt);
            }

            /**
             * while语句
             */
            //while_stmt	->	while	(	expr	)	stmt
            case 74: {
                ExprNode condition = (ExprNode) stack.getNode(2);
                StmtNode stmt = (StmtNode) stack.getNode(0);
                return new WhileNode(stack.getSymbol(4).getLocation(),
                        condition, stmt);
            }

            /**
             *   doWhile语句
             */
            //dowhile_stmt	->	do	stmt	util	(	expr	)	;
            case 75: {
                ExprNode condition = (ExprNode) stack.getNode(2);
                StmtNode stmt = (StmtNode) stack.getNode(5);
                return new DoWhileNode(stack.getSymbol(6).getLocation(), condition, stmt);
            }

            /**
             * for语句
             */
            //for_stmt	->	for	(	forIniitialer	optional_for_expr	;	optional_for_exprs	)	stmt
            case 76: {
                ExprNode
                        expr2 = (ExprNode) stack.getNode(4);
                StmtNode stmt = (StmtNode) stack.getNode(0);
                ListNode<ExprNode> expr3=(ListNode<ExprNode>) stack.getNode(2);
                ForInitializer forInitializer=(ForInitializer) stack.getNode(5);

                return new ForNode(forInitializer, expr2, expr3, stmt);
            }


            //forIniitialer	->	defvars
            case 163:{
                ListNode<DefinedVariable> listNode=(ListNode<DefinedVariable>) stack.getNode(0);
                return new ForInitializer(listNode.getLocation(),listNode);
            }
            //forIniitialer	->	optional_for_exprs ;
            case 164:{
                ListNode<ExprNode> listNode = (ListNode<ExprNode>) stack.getNode(1);
                return new ForInitializer(listNode.getLocation(), listNode, false);
            }


            //optional_for_exprs	->	optional_for_expr
            case 165:{
                ExprNode expr=(ExprNode) stack.getNode(0);
                ListNode<ExprNode> exprs=new ListNode<ExprNode>(expr==null?null: expr.getLocation());
                if(expr!=null)
                    exprs.addNode(expr);
                return exprs;
            }

            //optional_for_exprs	->	optional_for_exprs	comma	optional_for_expr
            case 166:{
                ListNode<ExprNode> exprs = (ListNode<ExprNode>) stack.getNode(2);
                ExprNode expr = (ExprNode) stack.getNode(0);
                exprs.addNode(expr);
                return exprs;
            }


            //optional_for_expr	->
            //stmt	->	;
            // optional_expr	->
            //storage	->
            //params	->	void
            //kwargs	->
            //type_postfix	->
            case 18:
            case 20:
            case 77:
            case 59:
            case 24:
            case 28:
            case 41: {
                return null;
            }

            /**
             * 跳转语句
             */
            //break_stmt	->	break	;
            case 79: {
                return new JumpNode(stack.getSymbol(1).getLocation(), JumpNode.BREAK);
            }
            //goto_stmt	->	goto	id	;
            case 80: {
                String id = ((Token) stack.getSymbol(1)).getImage();
                return new GotoNode((stack.getSymbol(2)).getLocation(),
                        id);
            }
            //continue_stmt	->	continue	;
            case 81: {
                return new JumpNode(stack.getSymbol(1).getLocation(), JumpNode.CONTINUE);
            }
            //return_stmt	->	return	;
            case 82: {
                return new JumpNode(stack.getSymbol(1).getLocation(), JumpNode.RETURN);
            }
            //return_stmt	->	return	expr	;
            case 83: {
                ExprNode expr= (ExprNode) stack.getNode(1);
                return new ReturnExprNode(stack.getSymbol(2).getLocation(),
                            expr);
            }

            /**
             * switch语句
             */
            //switch_stmt	->	switch	(	expr	)	{	label_stmts	}
            case 87: {
                ListNode<LabelNode> lblStmts = (ListNode<LabelNode>) stack.getNode(1);
                ExprNode expr = (ExprNode) stack.getNode(4);
                return new SwitchNode(expr.getLocation(), expr, lblStmts);
            }

            /**
             * 标签语句
             */
            //label_stmt	->	id	:	stmt
            //label_stmt	->	case	INTEGER	:	stmt
            case 84:
            case 85: {
                StmtNode stmt = (StmtNode) stack.getNode(0);
                Token tok = (Token) stack.getSymbol(2);
                String id = tok.getImage();
                if (p.getPid() == 84 && !id.equals("default")) {
                    return new LabelNode(tok.getLocation(), LabelNode.GOTO, id, stmt);
                } else {
                    return new LabelNode(stack.getSymbol(3).getLocation(),
                            LabelNode.SWITCH, id, stmt);
                }
            }

            //label_stmts	->	label_stmts	label_stmt
            case 88: {
                Nonterminal nt = (Nonterminal) stack.getSymbol(0),
                        nt1 = (Nonterminal) stack.getSymbol(1);
                LabelNode labelStmt = (LabelNode) stack.getNode(0);
                ListNode<LabelNode> lableList = (ListNode<LabelNode>) stack.getNode(1);
                lableList.addNode(labelStmt);
                return lableList;
            }

            //label_stmts	->	label_stmt
            case 89: {
                LabelNode lblStmt = (LabelNode) stack.getNode(0);
                ListNode<LabelNode> lblList = new ListNode<>(lblStmt.getLocation());
                lblList.addNode(lblStmt);
                return lblList;
            }

            /**
             * stmt
             */
            //stmt	->	expr	;
            case 62: {
                ExprNode expr = (ExprNode) stack.getNode(1);
                return new ExprStmt(expr.getLocation(), expr);
            }

            /**
             * block
             */
            //block	->	{	block_componets 	}
            case 30: {
                BlockComponents blockComponents =(BlockComponents) stack.getNode(1);
                return new BlockNode(blockComponents);
            }

            //block_componets	->
            case 31:{
                return new BlockComponents(null);
            }

            //block_componets	->	block_componets	block_componet
            case 32:{
                BlockComponents blockComponents =(BlockComponents) stack.getNode(1);
                Node node= stack.getNode(0);
                blockComponents.addNode(node);
                return blockComponents;
            }

            //block_componet	->	defvars
            //block_componet	->	stmt
            case 161:
            case 162:{
                return stack.getNode(0);
            }

            /**
             *语句列表
             */
            //stmts	->
            case 57: {
                return new ListNode<>((stack.getSymbol(0)).getLocation());
            }
            //stmts	->	stmts	stmt
            case 58: {
                StmtNode stmt = (StmtNode) stack.getNode(0);
                ListNode<StmtNode> stmts = (ListNode<StmtNode>) stack.getNode(1);
                stmts.addNode(stmt);
                return stmts;
            }

            /**
             *
             *
             *
             * 定义
             *
             *
             *
             */

            /**
             * 变量定义
             */
            //defvars	->	storage	type	name	optional_expr	initial_parts	;
            case 14: {
                Node expr = stack.getNode(2);
                NameNode nameNode = ((NameNode) stack.getNode(3));
                String name = nameNode.getName();
                TypeNode type = (TypeNode) stack.getNode(4);
                StaticNode prv = (StaticNode) stack.getNode(5);

                boolean isStatic = prv != null;
                DefinedVariable var = new DefinedVariable((isStatic ? prv.getLocation() : nameNode.getLocation()), isStatic, type, name, expr);

                ListNode<DefinedVariable> vars = (ListNode<DefinedVariable>) stack.getNode(1);
                //将继承属性storage,type传递下去
                for (DefinedVariable v : vars.getNodeList()) {
                    v.setPriv(isStatic);
                    v.setType(type);
                }
                vars.addFirst(var);
                return vars;
            }

            //optional_expr	->	=	{	expr_list	}
            case 167:{
                ListNode<ExprNode> list=(ListNode<ExprNode>) stack.getNode(1);
                return list;
            }

            //expr_list	->
            case 168:{
                return null;
            }

            //expr_list	->	expr
            case 169:{
                ExprNode expr=(ExprNode)stack.getNode(0);
                ListNode<ExprNode> list=new ListNode<>(expr.getLocation());
                list.addNode(expr);
                return list;
            }

            //expr_list	->	expr_list	comma	expr
            case 170:{
                ListNode<ExprNode> list=(ListNode<ExprNode>) stack.getNode(2);
                ExprNode expr=(ExprNode) stack.getNode(0);
                list.addNode(expr);
                return list;
            }

            //initial_parts	->
            //other_var_decls	->
            case 15, 179: {
                ListNode vars = new ListNode<>(null);
                return vars;
            }

            //initial_parts	->	initial_parts	initial_part
            //other_var_decls	->	other_var_decls	other_var_decl
            case 16,180: {
                Node var =stack.getNode(0);
                ListNode vars = (ListNode) stack.getNode(1);
                vars.addNode(var);
                return vars;
            }

            //initial_part	->	comma	name	optional_expr
            case 17: {
                ExprNode expr = (ExprNode) stack.getNode(0);
                NameNode nameNode = ((NameNode) stack.getNode(1));
                String name = nameNode.getName();
                DefinedVariable var = new DefinedVariable(nameNode.getLocation(), name, expr);
                return var;
            }


            //storage	->	static
            case 21: {
                Token tok = (Token) stack.getSymbol(0);
                return new StaticNode(tok.getLocation());
            }

            /**
             * 变量声明
             */
            //var_decl	->	storage	type	name	other_var_decls	;
            case 178:{
                NameNode nameNode = ((NameNode) stack.getNode(2));
                String name = nameNode.getName();
                TypeNode type = (TypeNode) stack.getNode(3);
                StaticNode prv = (StaticNode) stack.getNode(4);

                boolean isStatic = prv != null;
                DeclaredVariable var = new DeclaredVariable((isStatic ? prv.getLocation() : nameNode.getLocation()),
                        isStatic, type, name);

                ListNode<DeclaredVariable> vars = (ListNode<DeclaredVariable>) stack.getNode(1);
                //将继承属性storage,type传递下去
                for (DeclaredVariable v : vars.getNodeList()) {
                    v.setPriv(isStatic);
                    v.setType(type);
                }
                vars.addFirst(var);
                return vars;
            }


            //other_var_decl	->	comma	name
            case 181:{
                NameNode nameNode = ((NameNode) stack.getNode(0));
                String name = nameNode.getName();
                DeclaredVariable var = new DeclaredVariable(nameNode.getLocation(), name);
                return var;
            }

            /**
             * 变量定义列表
             */

            //defvar_list	->	defvar_list	defvars
//            case 32: {
//                ListNode<DefinedVariable> vars1 = (ListNode<DefinedVariable>) stack.getNode(0);
//                ListNode<DefinedVariable> vars2 = (ListNode<DefinedVariable>) stack.getNode(1);
//                vars2.addAllNode(vars1);
//                return vars2;
//            }

            /**
             * 函数定义
             */
            //defun	->	storage	typeref	name	(	params	)	block
            case 22: {
                StaticNode storage = (StaticNode) stack.getNode(6);
                TypeNode type = (TypeNode) stack.getNode(5);
                NameNode nameNode = (NameNode) stack.getNode(4);
                String name = nameNode.getName();
                ListNode<ParamNode> parms = (ListNode<ParamNode>) stack.getNode(2);
                BlockNode block = (BlockNode) stack.getNode(0);
                boolean priv = storage != null;
                return new DefinedFunction((storage != null) ? storage.getLocation() : nameNode.getLocation()
                        , priv, type, name, parms, block);
            }

            /**
             * 函数声明
             */
            //fun_decl	->	storage	type	name	(	param_decl	)	;
            case 175:{
                StaticNode storage = (StaticNode) stack.getNode(6);
                TypeNode type = (TypeNode) stack.getNode(5);
                NameNode nameNode = (NameNode) stack.getNode(4);
                String name = nameNode.getName();
                ListNode<ParamNode> parms = (ListNode<ParamNode>) stack.getNode(2);
                boolean priv = storage != null;
                return new DeclaredFunction((storage != null) ? storage.getLocation() : nameNode.getLocation()
                        , priv, type, name, parms);
            }

            //param_decl	->	fixed_params	kwargs
            case 176:{
                ParamNode k = (ParamNode) stack.getNode(0);
                ListNode<ParamNode> params = (ListNode<ParamNode>) stack.getNode(1);
                if (k != null) {
                    params.addNode(k);
                }
                return params;
            }

            //param_decl	->	param_typerefs
            case 177:{
                return stack.getNode(0);
            }

            //params	->	fixed_params	kwargs
            case 23: {
                ParamNode k = (ParamNode) stack.getNode(0);
                ListNode<ParamNode> params = (ListNode<ParamNode>) stack.getNode(1);
                if (k != null) {
                    params.addNode(k);
                }
                return params;
            }


            //fixed_params	->	fixed_params	comma	param
            case 25: {
                ParamNode parm = (ParamNode) stack.getNode(0);
                ListNode<ParamNode> params = (ListNode<ParamNode>) stack.getNode(2);
                params.addNode(parm);
                return params;
            }

            //fixed_params	->	param
            case 26: {
                ParamNode parm = (ParamNode) stack.getNode(0);
                ListNode<ParamNode> params = new ListNode<>(parm.getLocation());
                params.addNode(parm);
                return params;
            }

            //param	->	type	name
            case 27: {
                NameNode nameNode=(NameNode)stack.getNode(0);
                String name = nameNode.getName();
                TypeNode type = (TypeNode) stack.getNode(1);
                return new ParamNode(type.getLocation()!=null?type.getLocation():nameNode.getLocation(), type, name);
            }


            //kwargs	-> comma	...
            case 29: {
                return ParamNode.KwargsNode;
            }

            /**
             *
             * 顶层定义/声明
             *
             */
            //top_decls	->	top_decls	top_decl
            case 7: {
                Node node = stack.getNode(0);
                ListNode<DeclararedNode> decls = (ListNode<DeclararedNode>) stack.getNode(1);
                if (!(node instanceof ListNode)){
                    DeclararedNode decl = (DeclararedNode) stack.getNode(0);
                    if(decl!=null){
                        decls.addNode(decl);
                    }else{
                        throw new RuntimeException();
                    }
                }else{
                    ListNode<DeclararedNode> lst=(ListNode<DeclararedNode>) node;
                    decls.addAllNode(lst);
                }
                return decls;
            }

            //top_decls	->	s
            case 8: {
                return new ListNode<>(null);
            }

            //defconst	->	const	type	id   =   expr ;
            case 150: {
                ExprNode expr = (ExprNode) stack.getNode(1);
                TypeNode type = (TypeNode) stack.getNode(4);
                String id = ((Token) stack.getSymbol(3)).getImage();
                Token tok = (Token) stack.getSymbol(5);

                return new DefinedConst(tok.getLocation(), type, id, expr);
            }

            //top_decl	->	extern	fun_decl
            //top_decl	->	extern	var_decl
            case 173,174:{
                return stack.getNode(0);
            }

            /**
             *
             *
             *
             *
             * 类型系统
             *
             *
             *
             *
             *
             *
             */
            //defstruct	->	struct	name	member_list	;
            //defunion	->	union	name	member_list	;
            case 33:
            case 34: {
                String name = ((NameNode) stack.getNode(2)).getName();
                Token tok = (Token) stack.getSymbol(3);
                ListNode<SlotNode> members = (ListNode<SlotNode>) stack.getNode(1);
                if (tok.getValue().equals("struct"))
                    return new definedStruct(tok.getLocation(), name, members);
                else
                    return new definedUnion(tok.getLocation(), name, members);
            }

            //member_list	->	{	slots	}
            case 35: {
                return stack.getNode(1);
            }

            //slots	->
            case 36: {
                ListNode<SlotNode> slots = new ListNode<>(null);
                return slots;
            }

            //slots	->	slots	type	name	;
            case 37: {
                String name = ((NameNode) stack.getNode(1)).getName();
                TypeNode type = (TypeNode) stack.getNode(2);
                ListNode<SlotNode> slots = (ListNode<SlotNode>) stack.getNode(3);

                Location l = (type.getLocation() == null) ? stack.getSymbol(0).getLocation() : type.getLocation();
                SlotNode slot = new SlotNode(l, type, name);
                slots.addNode(slot);
                return slots;
            }

            /**
             * 类型别名
             */
            //TYPEDEF	->	typedef	type	typeName	;
            case 38: {
                Token tok=(Token)(stack.getSymbol(3));
                String typeName = ((StringNode) stack.getNode(1)).getLiteralValue();
                TypeNode type = (TypeNode) stack.getNode(2);
                parser.addTypeName(typeName);
                return new TypedefNode(tok.getLocation(), type, typeName);
            }

            //typeName	->	id
            case 171:{
                Token token=(Token)stack.getSymbol(0);
                String name=token.getImage();
                return new  StringNode(token.getLocation(),name);
            }


            /**
             * 类型构成
             */
            //type	->	primary_type	type_postfix
            case 40: {
                BaseType base = (BaseType) stack.getNode(1);
                PostfixNode postfix = (PostfixNode) stack.getNode(0);
                PostfixNode head;
                if (postfix == null) head = null;
                else {
                    head = postfix.getHead();
                }
                return new TypeNode(base.getLocation(), base, head);
            }
            /**
             * 类型后缀
             */
            //type_postfix	->	type_postfix	[	]
            //type_postfix	->	type_postfix	*
            case 42:
            case 44: {
                PostfixNode postfix = null;
                if (p.getPid() == 42)
                    postfix = (PostfixNode) stack.getNode(2);
                else
                    postfix = (PostfixNode) stack.getNode(1);
                Token t = (Token) stack.getSymbol(0);
                PostfixNode newPostfix = new PtrPofix(t.getLocation());
                setNewPostfix(postfix, newPostfix);
                return newPostfix;

            }

            //type_postfix	->	type_postfix	[  INTEGER	]
            case 43: {
                PostfixNode postfix = (PostfixNode) stack.getNode(3);
                Nonterminal nt = (Nonterminal) stack.getSymbol(3);
                String lxrValue = ((Token) stack.getSymbol(1)).getImage();
                int integer = Integer.parseInt(lxrValue);

                PostfixNode newPostfix = new PtrPofix(nt.getLocation(), integer);
                setNewPostfix(postfix, newPostfix);
                return newPostfix;
            }

            //type_postfix	->	type_postfix	(	param_typerefs	)
            case 45: {
                PostfixNode postfix = (PostfixNode) stack.getNode(3);
                Nonterminal nt = (Nonterminal) stack.getSymbol(3);
                ListNode<ParamNode> parms = (ListNode<ParamNode>) stack.getNode(1);

                PostfixNode newPostfix = new ParamPofix(nt.getLocation(), parms.getNodeList());
                setNewPostfix(postfix, newPostfix);
                return newPostfix;
            }

            //type_postfix	->	type_postfix	(	void	)
            case 46: {
                PostfixNode postfix = (PostfixNode) stack.getNode(3);
                Nonterminal nt = (Nonterminal) stack.getSymbol(3);

                PostfixNode newPostfix = new ParamPofix(nt.getLocation(), null);
                setNewPostfix(postfix, newPostfix);
                return newPostfix;
            }


            /**
             * 函数指针的参数/函数的参数声明
             */
            //param_typerefs	->	fixed_param_ref	kwargs
            case 47: {
                ListNode<ParamNode> parms = (ListNode<ParamNode>) stack.getNode(1);
                ParamNode k =(ParamNode)(stack.getNode(0));
                if(k!=null){
                    parms.addNode(k);
                }
                return parms;
            }
            //fixed_param_ref	->	fixed_param_ref	comma type
            case 48: {
                TypeNode type = (TypeNode) stack.getNode(0);
                ListNode<ParamNode> parms = (ListNode<ParamNode>) stack.getNode(2);
                ParamNode parm = new ParamNode(type.getLocation(), type);
                parms.addNode(parm);
                return parms;
            }
            //fixed_param_ref	->	type
            case 49: {
                TypeNode type = (TypeNode) stack.getNode(0);
                ListNode params = new ListNode(type.getLocation());
                ParamNode parm = new ParamNode(type.getLocation(), type);
                params.addNode(parm);
                return params;
            }


            //primary_type	->	signed int_type
            //primary_type	->	unsigned int_type
            case 51:
            case 52:{
                BaseType baseType=(BaseType) stack.getNode(0);
                baseType.setSigned(p.getPid()==51);
                return baseType;
            }


            //primary_type	->	char
            //primary_type	->	void
            //primary_type	->	bool
            //primary_type	->	float
            //primary_type	->	double
            //int_type      ->  long
            //int_type      ->  short
            //int_type      ->  int
            case 53:
            case 54:
            case 153:
            case 155:
            case 156:
            case 157:
            case 159:
            case 160: {
                String t = stack.getSymbol(0).getValue();
                BaseType baseType = BaseType.typeMap.get(t);
                if (baseType == null) {
                    throw new RuntimeException("类型错误!");
                }
                baseType.setSigned(true);
                return baseType;
            }

            //int_type      ->  long long
            case 158:{
                BaseType baseType=BaseType.longLongType;
                return baseType;
            }

            //primary_type	->	int_type
            case 154:{
                return stack.getNode(0);
            }

            //primary_type	->	type_name
            case 172:{
                StringNode node=(StringNode)(stack.getNode(0));
                String typeName =node.getLiteralValue();
                return new UserType(node.getLocation(),typeName);
            }


            //typeref_base	->	struct	id
            case 55: {
                String id = ((Token) stack.getSymbol(0)).getImage();
                return new StructType((stack.getSymbol(1)).getLocation(),
                        id);
            }
            //typeref_base	->	union	id
            case 56: {
                String id = ((Token) stack.getSymbol(0)).getImage();
                return new UnionType((stack.getSymbol(1)).getLocation(),
                        id);
            }

            /**
             * 程序整体
             */
            //programe	->	import_staments	top_defs
            case 0: {
                ListNode<ImportNode> importstmts = (ListNode<ImportNode>) stack.getNode(1);
                ListNode<DeclararedNode> top_decls = (ListNode<DeclararedNode>) stack.getNode(0);
                AST ast = new AST(importstmts.getLocation(), importstmts, top_decls,handler);

                parser.setAst(ast);

                //输出程序的抽象语法树
                ast.dump(0);
                return ast;
            }

            //import_staments	->	import_staments	import_stament
            case 1: {
                ListNode<ImportNode> importstmts = (ListNode<ImportNode>) stack.getNode(1);
                ImportNode improtstmt = (ImportNode) stack.getNode(0);
                importstmts.addNode(improtstmt);
                return importstmts;
            }

            //import_staments	->
            case 2: {
                ListNode<ImportNode> importstmts = new ListNode<>(null);
                return importstmts;
            }

            //import_stament	->	import	NAME
            case 3: {
                ListNode<NameNode> path = (ListNode<NameNode>) stack.getNode(0);
                return new ImportNode(stack.getSymbol(1).getLocation(),
                        path);
            }


            //NAME	->	name	.	NAME
            case 4: {
                ListNode<NameNode> Name = (ListNode<NameNode>) stack.getNode(0);
                NameNode name = (NameNode) stack.getNode(2);
                Name.push(name);
                return Name;
            }

            //NAME	->	name	;
            case 5: {
                ListNode<NameNode> Name = new ListNode<>(null);
                NameNode name = (NameNode) stack.getNode(1);
                Name.addNode(name);
                return Name;
            }

            //name	->	id
            case 6: {
                Token tok = (Token) stack.getSymbol(0);
                String id = tok.getImage();
                return new NameNode(tok.getLocation(), id);
            }

            default:
                return null;
        }
    }

    public void setNewPostfix(PostfixNode oldp, PostfixNode newp) {
        if (oldp == null) {
            //此后缀为头后缀
            newp.setHead(newp);
        } else {
            //设置指向下一个后缀的指针
            oldp.setNextPostfix(newp);
            //设置指向头后缀的指针
            newp.setHead(oldp.getHead());
        }
    }

    public void close() {
        pw.close();
    }
}
