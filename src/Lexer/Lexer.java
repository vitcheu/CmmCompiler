package Lexer;

import ASM.Util.LiteralProcessor;
import Parser.Entity.Location;
import Parser.Entity.Token;
import compile.Constants;

import java.io.*;
import java.util.*;

import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isWhitespace;

/**
 * 该词法分析器输入C--语言的词素流,
 * 输出语法分析所用的词法单元流tokens
 * 主要将数字,字符,字符串规约为相应类型的常量,将标识符统一规约为id
 * 其他如关键字,分隔符,运算符等,保留其词素的值
 * 唯一的例外是逗号,因为语法分析需要读入csv文件,所以特殊地,逗号被表示成值为comma的词法单元
 */
public class Lexer {
    public final static int NORMAL = 0;
    public final static int IN_BLOCK_COOMENT = 2;
    public final static int IN_STRING = 3;
    private List<Token> tokens;
//    private HashMap<String,String>
    private int lineCnt = 1;

    private int state;
    private char cur;
    private StringBuilder multiLineStr=new StringBuilder();

    private String line;
    private int pos = 0;
    private boolean errorOccurred=false;
    private PrintWriter err =null;
    private PrintWriter log=null;
    private final static List<String> keyWords;
    static {
        keyWords=new LinkedList<>();
        String terminalFile=Constants.parserResPath+Constants.terminalFileName;
        try (BufferedReader reader=new BufferedReader(new FileReader(terminalFile))){
            String line;
            while ((line=reader.readLine())!=null){
                Scanner scanner=new Scanner(line);
                while (scanner.hasNext()){
                    keyWords.add(scanner.next());
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
//            = Arrays.asList(new String[]{"int", "float", "char","bool",
//            "unsigned", "signed", "long", "double" ,
//            "if", "else", "do", "return", "for", "while","import",
//            "util", "goto", "break", "continue", "static", "const" ,
//            "void", "struct", "typedef", "union", "sizeof", "switch"});

    public Lexer(String filename) throws FileNotFoundException {
        scan(filename);
        //令pos指向当前token流的位置
        pos=0;
    }

    public boolean ErrorOccurred(){
        return errorOccurred;
    }

    public boolean hasMoreTokens(){
        return pos!=tokens.size();
    }

    public Token getCurToken(){
        return tokens.get(pos);
    }

    public Token nextToken() {
        return tokens.get(pos++);
    }

    public List<Token> scan(String filename) throws FileNotFoundException {
        tokens = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            log=new PrintWriter(Constants.parserOutputPath +"scan log.txt");
            err= new PrintWriter(Constants.parserOutputPath+"scan error.txt");
            state=NORMAL;
            while ((line = reader.readLine()) != null) {
                //("line: "+lineCnt);
                tokenize(line);
                lineCnt++;
            }
        }catch (FileNotFoundException fileNotFoundException){
            throw fileNotFoundException;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(log!=null)
                log.close();
            if(err!=null)
                err.close();
        }
        //加入结束符
        tokens.add(new Token("$",new Location(lineCnt,0)));
        return tokens;
    }

    /**
     *扫描特定行,解析得到词法词法单元并加入tokens
     */
    private void tokenize(String line) {
        log.printf("\n-------第%d行开始:%s\n",lineCnt,line);
        pos=0;
        List<Token> ori=new LinkedList<>(tokens);
        List<Token> lineToken=new LinkedList<>();
        while ((cur=getChar())!= '\n') {
            StringBuilder sb = new StringBuilder();
            Location location=new Location(lineCnt,pos);
//            log.println("********循环开始,cur="+cur);
            switch (state) {
                case NORMAL:
                    if (isWhitespace()) {
//                        if (cur == '\n')
//                            break;
//                        log.println("空白符:"+cur);
                        pos++;
                    }

                    else if (Character.isDigit(cur)) {
                        if (cur == '0') {
                            consume(sb);
                            //小数
                            if (cur == '.') {
                                consume(sb);
                                sb.append(decimal());
                                tokens.add(new Token("FLOAT",sb.toString(), location));
//                                log.println("小数:"+sb.toString());
                                continue;
                            }
                            //十六进制数
                            else if (cur == 'x' || cur == 'X') {
                                consume(sb);
                                sb.append(hex());
                            }
                            //八进制数
                            else if (cur >= '0' && cur < '8') {
                                sb.append(octal());
                            }
                            sb.append(opt_UL());
                            log.println("解析到数字:" + sb);
                            tokens.add(new Token("INTEGER", sb.toString(), location));
                        }
                        //[1-9]
                        else {
                            consume(sb);
                            sb.append(number());
                            if (cur == '.') {
                                consume(sb);
                                sb.append(decimal());
                                tokens.add(new Token("FLOAT",sb.toString(),location));
                                log.println("解析到数字:" + sb);
                            } else {
                                String exp = exp();
                                if (!exp.isEmpty()) {
                                    sb.append(exp);
                                    sb.append(opt_DF());
                                    tokens.add(new Token("FLOAT", sb.toString(), location));
                                    log.println("解析到数字:" + sb);
                                } else {
                                    sb.append(opt_UL());
                                    tokens.add(new Token("INTEGER", sb.toString(), location));
                                    log.println("解析到数字:" + sb);
                                }
                            }
                        }


                    }
                    //标识符
                    else if (Character.isJavaIdentifierStart(cur)) {
                        consume(sb);
                        sb.append(identity());
                        log.println("id="+sb);
                        if(sb.toString().equals("sizeof")){
                            tokens.add(new Token("SIZEOF",location));
                        }else if(keyWords.contains(sb.toString())){
                            tokens.add(new Token(sb.toString(),location));
                        }else{
                            tokens.add(new Token("id",sb.toString(),location));
                        }

                    } else if (cur == '.') {
                        consume(sb);
                        //表示可变参数的"..."
                        if(cur=='.'){
                            consume(sb);
                            if(cur=='.'){
                                consume(sb);
                                tokens.add(new Token("...",location));
                            }else{
                                error("错误的输入符号: .."+cur,true);
                            }
                        }
                        //省略整数部分的小数
                        else if (Character.isDigit(cur)) {
                            sb.delete(0,sb.length());
                            sb.append("0.");
                            sb.append(decimal());
                            tokens.add(new Token("FLOAT", sb.toString(), location));
                        }
                        //成员操作符
                        else if (Character.isAlphabetic(cur)||cur=='_') {
                            tokens.add(new Token(".",location));
                            continue;
                        }
                        else {
                           error("错误的点号",true);
                        }

                    }

                    else if (cur == '+' || cur == '-') {
                        char c = cur;
                        consume(sb);
                        //++,--,+=,-+,->
                        if(c=='-'&&cur=='>'||
                            cur==c||
                            cur=='='){
                            consume(sb);
                        }

                            //+int或-int
    //                        if (Character.isDigit(cur) && cur != '0') {
    //                            consume(sb);
    //                            sb.append(number());
    //                            tokens.add(new Token(sb.toString(), tokenKind.INTEGER, location));
    //                        }
                            tokens.add(new Token(sb.toString(),location));
                        }

                    else if(cur=='*'||cur=='/'||cur=='%'||cur=='!'){
                        if(cur=='/'&&lookahead()=='/'){
                            //进入行注释,丢弃行内其他所有字符
                            pos=line.length();
                        }else if(cur=='/'&&lookahead()=='*') {
                            //进入块注释
                            pos+=2;
                            cur=getChar();
                            state=IN_BLOCK_COOMENT;
                        }else {
                            consume(sb);
                            if (cur == '=') {
                                consume(sb);
                            }
                            tokens.add(new Token(sb.toString(),location));
                            log.println("****解析到运算符："+sb);
                        }
                    }

                    else if(cur=='&'||cur=='|'||cur=='='){
                        char c=cur;
                        consume(sb);
                        if(cur==c){
                            consume(sb);
                        }
                        tokens.add(new Token(sb.toString(),location));
                    }

                    else if(cur=='<'||cur=='>'){
                        char c=cur;
                        consume(sb);
                        if(cur==c){
                            consume(sb);
                        }else if(cur=='='){
                            consume(sb);
                        }
                        tokens.add(new Token(sb.toString(),location));
                    }

                    else if(cur=='~'||cur=='{'||cur=='}'||cur=='['||cur==']'||cur==';'||cur==','||cur=='?'
                            ||cur==':'||cur=='('||cur==')'){
                        if(cur==','){
                            sb.append("comma");
                            pos++;
                            cur=getChar();
                        }else {
                            consume(sb);
                        }
                        tokens.add(new Token(sb.toString(),location));
                    }

                    //字符串
                    else if(cur=='\"'){
                        pos++;//丢弃\”
                        cur=getChar();
                        str(sb,location);
                    }

                    //字符
                    else if(cur=='\''){
                        pos++;
                        cur=getChar();
                        if(cur=='\\'){
                            consume(sb);
                            //八进制数
                            if(cur>='0'&&cur<'7'){
                                for(int i=0;i<3;i++){
                                    if(cur>='0'&&cur<='7')
                                        consume(sb);
                                    else {
                                        error("\n期望八进制数,得到:,"+sb+cur,true);
                                    }
                                }
                            }else{
                                consume(sb);
                            }
                        }else if(cur!='\n'&&cur!='\''){
                            consume(sb);
                        }

                        if(cur=='\''){
                            pos++;
                            cur=getChar();
                        }else{
                            error("非法的字符:"+sb+cur,true);
                            continue;
                        }
                        tokens.add(new Token("CHARACTOR",sb.toString(),location));
                        log.println("解析到字符："+sb);
                    }else{
                        error("非法的字符\'"+cur+"'",true);
                    }
                    break;

                case IN_BLOCK_COOMENT:
                    while (!(cur=='\n'||cur=='*'&&lookahead()=='/')){
                        pos++;
                        cur=getChar();
                    }

                    if(cur!='\n'){
                        state=NORMAL;
                        pos+=2;
                        cur=getChar();
                    }
                    break;

                case IN_STRING:
                    while (cur!='\"'&&cur!='\n'){
                       if(isWhitespace())
                           getNext();
                       else{
                           error("多行字符串中出现意外的字符作为行开头:"+cur,false);
                           eatLine();
                           break;
                       }
                    }
                    if(cur=='\"'){
                        getNext();
                        str(sb,location);
                    }
                    break;
            }
        }

        for(int i=0;i<tokens.size();i++){
            Token t=tokens.get(i);
            if(!ori.contains(t)){
                lineToken.add(t);
            }
        }
        log.println("本行tokens:\n");
        for(Token tok:lineToken){
            log.println(tok.getValue()+"\t#"+tok.getImage()+"\t"+tok.getLocation());
        }
    }

    private void eatLine(){
        while (cur!='\n'){
            getNext();
        }
    }

    /**
     * 获取当前输入流的字符
     */
    public char getChar() {
        if (pos == line.length()) {
            return '\n';//表示结束
        } else {
            return line.charAt(pos);
        }
    }

    /**
     * 向前看一个字符
     */
    public char lookahead() {
        if (pos+1== line.length()) {
            return '\n';
        } else {
            return line.charAt(pos + 1);
        }
    }

    /**
     * 消耗一个字符
     */
    public void consume(StringBuilder sb) {
        sb.append(getChar());
        pos++;
        cur = getChar();
    }

    /**
     * @return 从当前位置开始扫描的数字串
     * 即[digit]*
     */
    public String number() {
        StringBuilder sb = new StringBuilder();
        while (Character.isDigit(cur)) {
            consume(sb);
        }
        log.println("#number获得："+sb);
        return sb.toString();
    }

    /**
     * 扫描指数部分
     */
    public String exp() {
        StringBuilder sb = new StringBuilder();
        //可选的指数部分
        if (cur == 'E' || cur == 'e') {
            consume(sb);
            //读入指数值
            String numStr = number();
            if (numStr.isEmpty()) {
                error("\n指数部分为空!",false);
                sb = new StringBuilder();
            } else {
                sb.append(numStr);
            }
        }
        return sb.toString();
    }

    /**
     * 读入可选的后置D或F
     */
    public String opt_DF() {
        if (cur == 'D' || cur == 'F') {
            pos++;
            return String.valueOf(cur);
        }
        return "";
    }

    public String opt_UL() {
        if (cur == 'U' || cur == 'L') {
            pos++;
            return String.valueOf(cur);
        }
        return "";
    }

    /**
     * 读入小数部分,由数字串,可选的指数部分,可选的D或F标志组成
     */
    public String decimal() {
        StringBuilder sb = new StringBuilder();
        String num1 = number(),
                exp = exp(),
                opt = opt_DF();
        if (num1.isEmpty()) {
            sb.append("0");
        }
        sb.append(num1);
        sb.append(exp);
        sb.append(opt);
        return sb.toString();
    }

    /**
     * 读入八进制数字
     * [0-7]*
     */
    public String octal() {
        StringBuilder sb = new StringBuilder();
        while (cur >= '0' &&cur <= '7') {
            consume(sb);
        }
        return sb.toString();
    }

    /**
     * 读入十六进制数字
     * [0-f]+
     */
    public String hex() {
        StringBuilder sb = new StringBuilder();
        while (Character.isDigit(cur) || cur >= 'a' &&
                cur<= 'f') {
            consume(sb);
        }
        return sb.toString();
    }

    /**
     * 读入标识符串
     */
    public String identity() {
        //进入此函数时已经判断nextChar为[1-9]
        StringBuilder sb = new StringBuilder();
        while (Character.isDigit(cur)||Character.isAlphabetic(cur)||cur=='_') {
            consume(sb);
        }
        return sb.toString();
    }

    /**
     *读取多行字符串
     */
    public void str(StringBuilder sb,Location location){
        while ((cur=getChar())!='\"'){
            if(cur!='\\'&&cur!='\n'){
               consume(sb);
            }else if(cur=='\\'){
                consume(sb);
                /*记录转义字符*/
                StringBuilder builder=new StringBuilder("\\");
                //八进制数
                if(cur>='0'&&cur<'7'){
                    for(int i=0;i<3;i++){
                        if(cur>='0'&&cur<'7'){
                            builder.append(cur);
                            consume(sb);
                        }
                        else {
                           break;
                        }
                    }
                }
                //十六进制
                else if(cur=='x'){
                    builder.append(cur);
                    consume(sb);
                    int cnt=0;
                    while (isHexNum(cur)){
                        builder.append(cur);
                        consume(sb);
                        cnt++;
                    }
                    if(cur==0){
                        error("十六进制整数文本至少具有一位数",false);
                    }
                }
                //其他字符
                else {
                    builder.append(cur);
                    consume(sb);
                }
                if (!LiteralProcessor.checkValidationOfEscapeStr(builder.toString())) {
                    error("无效的转义字符:"+builder,false);
                }
            }else{
               error("\n此格式的字符串不能跨越多行,需要在行末增加\\!",false);
//               break;
            }
        }
        //字符串结束
        if(cur=='\"'){
            //丢弃\”
            getNext();
            //过滤空白符
            while (isWhitespace()){
                getNext();;
            }
            multiLineStr.append(sb.toString());
            //多行字符串,丢弃'\',暂时保存其他字符
            if(cur=='\\'){
                if(lookahead()!='\n'){
                    error("需要用\\结束多行字符串",false);
                    eatLine();
                    return;
                }
                state=IN_STRING;
                pos++;
            }
            //多行字符串
            else if(cur=='\n') {
                state=IN_STRING;
            }else{
                state = NORMAL;
                tokens.add(new Token("STRING", multiLineStr.toString(), location));
                log.println("#str解析到字符串：" + multiLineStr.toString());
                multiLineStr=new StringBuilder();
            }
        }
    }

    private boolean isWhitespace(){
        return Character.isWhitespace(cur)&&cur!='\n';
    }

    private void error(String msg,boolean moveToNext){
        errorOccurred=true;
        err.println(String.format("在(%s,%s)处,%s",lineCnt,pos,msg));
        if(moveToNext)
            getNext();
    }

    private void getNext(){
        pos++;
        cur=getChar();
    }

    private boolean isHexNum(char c){
        return c>='0'&&c<='9'
                ||(c>='a'&&c<='f')||(c>='A'&&c<='F');
    }
}