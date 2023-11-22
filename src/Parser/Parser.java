package Parser;

import AST.Node;
import AST.AST;
import CompileException.CompileError;
import Lexer.Lexer;
import Parser.Entity.*;

import java.io.*;
import java.util.*;

import static compile.Constants.*;
import static compile.Constants.numOfNonTerminal;
import static compile.Constants.numOfTerminal;

public class Parser {
    private static final String errPath=parserOutputPath+ "Syntax Error.txt";
    private static final String logPath=parserOutputPath+"Parsing Log";
    private static boolean resource_loaded=false;
    private static Map<String, Symbol> symbolMap = new HashMap<>();
    private static List<Token> tokenList = new ArrayList<>();
    private static List<Nonterminal> NonterminalList = new ArrayList<>();
    private static List<Production> productionList = new ArrayList<>();
    //定义的类型名字
    private HashSet<String> defTypeNames=new HashSet<>();
    private final String parsingFilePath;
    private AnalysisTable analysisTable;
    private PrintWriter err =null;
    private PrintWriter logger =null;
    protected boolean verbose=true;
    private Reducer reducer;
    private Lexer lexer;
    private AnalysisStack analysisStack=null;

    private AST ast;

    public AST getAst() {
        return ast;
    }

    public void setAst(AST ast) {
        this.ast = ast;
    }

    Nonterminal start;
//    private Map<String,>

    public Parser(String filePath) throws FileNotFoundException {
        this.parsingFilePath =filePath;
         analysisTable = new AnalysisTable(numOfState,
                numOfTerminal + numOfNonTerminal, numOfTerminal,this);
    }

    private void readToken() {
        if(resource_loaded)
            return;
        String grammarFile = parserResPath + "grammar.csv";
        String csvSplit = ",";
        String line;

        String[] row;
        Symbol symbol;

        try (BufferedReader br = new BufferedReader(new FileReader(grammarFile))) {
            List<List<String>> bodies = new LinkedList<>();
            List<Nonterminal> heads = new LinkedList<>();
            int cnt1 = 6;
            int cnt2 = 6;
            //加载非终结符,保存产生式体
            while ((line = br.readLine()) != null) {
                if (line.equals("") || line.trim().startsWith("//")) continue;
//                writer.write("-----------------------<"+);
                List<String> rightSide = new ArrayList<>();
                row = line.split(csvSplit);
                //保存非终结符
//                if(!Nonterminals.contains(row[0].trim())
//                    ){
//                    Nonterminals.add(row[0].trim());
//                }
                symbol = symbolMap.get(row[0].trim());
                if (symbol == null) {
                    symbol = new Nonterminal(row[0].trim());
                    addSymbol(symbol);
                }
                heads.add((Nonterminal) symbol);

                //获取产生式体
                for (int i = 1; i < row.length; i++) {
                    rightSide.add(row[i].trim());
                }
                bodies.add(rightSide);
            }
            //获取开始符号
            start = NonterminalList.get(0);

            //加载终结符和产生式
            addSymbol(Token.EOF);
            for (int i = 0; i < bodies.size(); i++) {
                List<Symbol> rightSide = new LinkedList<>();
                List<String> body = bodies.get(i);
                Nonterminal head = heads.get(i);
                for (String str : body) {
                    if (str.trim().equals("ε") || str.equals("")) {
                        continue;
                    }
                    symbol = symbolMap.get(str.trim());
                    if (symbol == null) {
                        symbol = new Token(str.trim());
//                        log.println("加入终结符"+symbol);
                        addSymbol(symbol);
                    }
                    rightSide.add(symbol);
                }

                Production newPro = new Production(head, rightSide);
                productionList.add(newPro);
//                log.println("**加入产生式:\t"+newPro);
            }
            addSymbol(Token.EPSILON);

            logger.println("\n-----------终结符表:");
            for (int i = 0; i < tokenList.size(); i++) {
                logger.print("#" + tokenList.get(i).getId() + tokenList.get(i) + "\t");
                if (++cnt1 % 6 == 0)
                    logger.println();
            }
            logger.println("\n-----------非终结符表:");
            for (int i = 0; i < NonterminalList.size(); i++) {
                logger.print("#" + NonterminalList.get(i).getId() + NonterminalList.get(i) + "\t");
                if (++cnt2 % 6 == 0)
                    logger.println();
            }

            resource_loaded=true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 构造原文法的增广文法
     */
    private void expand() {
        Nonterminal oriStart = start;
        Nonterminal newStart = new Nonterminal(start + "'");
        addSymbol(newStart);
        List<Symbol> r = new LinkedList<>();
        r.add(oriStart);
        Production accProduction = new Production(newStart, r);
        productionList.add(accProduction);
//        accProduction = new Production(newStart, r);
//        newStart.addProduction(accProduction);
        start = newStart;
    }

    public String translateValue(int value) {
        if (value < numOfState) {
            return "shift to @" + value;
        } else {
            return "reduced " + productionList.get(value - numOfState);
        }
    }

    public void analysis()throws CompileError {
        try (PrintWriter errWriter=new PrintWriter(errPath);
             PrintWriter logger=new PrintWriter(logPath)){
             analysis(errWriter,logger);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }finally {
            LibraryLoader.close();
        }
    }

    protected void analysis(PrintWriter errWriter,PrintWriter logger) throws FileNotFoundException, CompileError {
        /*启动词法分析器*/
        lexer = new Lexer(parsingFilePath);
        if (lexer.ErrorOccurred()) {
            throw new CompileError("读入错误", null);
        }
        this.logger=logger;
        this.err=errWriter;
        start_Parsing();
    }

    private void start_Parsing() throws FileNotFoundException {
        /*预处理*/
        readToken();
        expand();
        /* 初始化语法分析栈和规约器 */
        analysisStack = new AnalysisStack(analysisTable, err);
        reducer = new Reducer(analysisStack, this,err);
        analysisStack.setReducer(reducer);
        //加入开始状态
        analysisStack.push(startState, Token.EOF);
        //记录分析过程所选择的产生式
        List<Production> selectedProduction = new LinkedList<>();

        boolean finished = false;
        int cnt = 1;
        int lineCnt;
        /*开始分析过程*/
        verbose_println("*********语法分析开始*********");
        verbose(String.format("%-4s %-3s %-96s %-10s %-15s %-18s \n",
                "行号", "回合", "栈的内容", "当前状态", "输入流符号", "动作"));

        Token a = lexer.nextToken();
        lineCnt = a.getLocation().getLine();
        int tokId = getTokenId(a);

        while ((!finished)) {
            int curState = analysisStack.peek();
            verbose(String.format("%-4d #%-3d %-100s %-10d %-15s ",
                    lineCnt, cnt++, analysisStack, curState, a));

            int value = analysisTable.getAction(curState, a, tokId);
//            if (analysisStack.size() > 2) {
//                Symbol symbol = analysisStack.getSymbol(2);
//                if ((symbol instanceof Nonterminal)) {
//                    Nonterminal nt=(Nonterminal) symbol;
//                    if (nt.getNode() != null && nt.getNode() instanceof ExprNode)
//                        verbose("\n---------------------------");
//                        verbose(nt.getNode());
//                }
//            }
            //语法错误
            if (value == errNo) {
                String s = "\n语法错误,符号:" + a + "#" + a.getImage() + ",位置:" + a.getLocation();
                err.println(s);
                throw new RuntimeException(s);
            }
            //动作为接受
            if (value == acc) {
                finished = true;
                analysisStack.pop();
                verbose(String.format ("%-18s \n", "接受"));
                break;
            }
            //动作为移进
            if (value < numOfState) {
                verbose(String.format ("%-18s \n", "移入" + a));
                //转到新状态
                analysisStack.push(value, a);
                //获取下一个符号
                if (!lexer.hasMoreTokens())
                    break;
                a = lexer.nextToken();
                lineCnt = a.getLocation().getLine();
                tokId = getTokenId(a);
            }
            //动作为规约
            else {
                Production pro = productionList.get(value - numOfState);
                verbose(String.format("%-18s \n", "根据" + pro.toNotapString() + "规约"));
                selectedProduction.add(pro);
                analysisStack.reduced(pro);
            }
        }

        if (finished) {
            //还有多余的程序,这里假设后面的输入流不全部为空行
            if (lexer.hasMoreTokens()) {
                err.println("多余的内容");
            }
            verbose_println("\n*****语法分析过程结束******");
            verbose_println("规约了" + selectedProduction.size() + "次");
//            verbose("选择的产生式依次为:");
//            for (Production p : selectedProduction) {
//                verbose(p);
//            }
//                    showSyntaxTree(selectedProduction);
        }

        reducer.close();
        Node.closeDumping();
    }

    /**
     * 获取输入流符号的token id,以查询语法分析表
     */
    public int getTokenId(Token a) {
        Token tok = (Token) symbolMap.get(a.getValue());
        if (tok == null) {
            err.println("错误的输入符号:" + a);
            err.close();
            throw new RuntimeException();
        }
        return tok.getId();
    }

    /**
     * 委托analysis_stack查询栈中特定位置的符号
     */
    public Symbol getSymbol(int offset){
        return analysisStack.getSymbol(offset);
    }

    public Token getToken(int id) {
        return tokenList.get(id);
    }

    /**
     * 添加文法符号t
     */
    public void addSymbol(Symbol t) {
        symbolMap.put(t.getValue(), t);
        if (t.isTerminal()) {
            tokenList.add((Token) t);
        } else {
            NonterminalList.add((Nonterminal) t);
        }
    }

    public Reducer getReducer() {
        return reducer;
    }

    public void addTypeName(String typeName){
        defTypeNames.add(typeName);
    }

    public boolean containTypeName(String typeName){
        return defTypeNames.contains(typeName);
    }

    private void verbose_println(String msg){
        verbose(msg+"\n");
    }

    private void verbose(String msg){
        if(verbose){
            logger.print(msg);
        }
    }
}