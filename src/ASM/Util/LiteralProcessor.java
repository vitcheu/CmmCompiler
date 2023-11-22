package ASM.Util;

import IR.Constants.Type;
import IR.Literal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 转换八进制或十六进制的字面量到masm格式
 */
public class LiteralProcessor {
    private static final String HEX_PREFIX="0x";
    private static final String OCTAL_PREFIX="0";
    private static final String  OCTAL_POSTFIX="Q";
    private static final String ESCAPE_PATTERN = "\\\\[abftvnr\\\'\\\"\\?\\\\]" + "|\\\\([0-7]{1,3}|x[0-9a-fA-F]+)";
    private static final String LINE_COMMENT= "^((\".*?\")|([^\"]*?))*(//.*)$";

    static class SpiltResult{
        int len;
        StringBuilder builder;

        public SpiltResult(int len, StringBuilder builder) {
            this.len = len;
            this.builder = builder;
        }
    }
    public static Literal transformLiteral(Literal literal){
        Type type=literal.getIRType();
        Literal result;
        String lxr=literal.getLxrValue();
        if(type.isFloatType()){
            ;
        }else if(type==Type.StringLiteral){
            ;
        }else if(type==Type.charType){
            lxr= transformCharLiteral(lxr);
        }else if(type.isIntType()){
            lxr=transformIntLiteral(lxr);
        }
        return new Literal(lxr,type);
    }

    /**
     * 检查转义字符串的有效性
     */
    public static boolean checkValidationOfEscapeStr(String str){
        return str.matches(ESCAPE_PATTERN);
    }

    private static String transformIntLiteral(String literal){
        /*十六进制*/
        if(literal.startsWith(HEX_PREFIX)){
            String replacement;
            if(Character.isAlphabetic(literal.charAt(2))){
                replacement="0";
            }else
                replacement="";
            literal= literal.replace(HEX_PREFIX,replacement);
            literal=literal+"H";
        }
        /*八进制*/
        else if(literal.startsWith(OCTAL_PREFIX)&&!literal.equals("0")){
            literal=literal.replace(OCTAL_PREFIX,"");
            literal=literal+"Q";
        }

        return literal;
    }

    private static String transformCharLiteral(String literal){
        //八进制形式
        if(literal.startsWith("\\")){
            //转换为八进制整数表示
            String pattern="^\\\\(\\d{3})$";
            literal=literal.replaceAll(pattern,"$1Q");
        }else{
            literal="\'"+literal+"\'";
        }

        return literal;
    }

    /**
     *分割带有转义字符的字符串
     */
    public static String  spilitString(String literal){
        return split(literal).builder.toString();
    }

    /**
     *统计字符串的字节数
     */
    public static int calculateLen(String str){
        return split(str).len;
    }

    private static SpiltResult split(String literal){
        String escape_pattern=ESCAPE_PATTERN;
        Pattern pattern=Pattern.compile(escape_pattern);
        Matcher m=pattern.matcher(literal);

        StringBuilder builder=new StringBuilder();
        int preEnd=0;
        int cnt=0;
        while (m.find()){
            String s=m.group();
            int start=m.start();
            String subStr=literal.substring(preEnd,start);
            if(!subStr.isEmpty()){
                cnt+=subStr.length();
                //替换字符串内部的'符号
                subStr= subStr.replace("\'","\'\'");
                builder.append(getPrefix(builder)+ getQuotedStr(subStr));
                preEnd=m.start();
            }
            //解码转义字符
            String tranStr=escapeToHex(s);
            builder.append(getPrefix(builder) + tranStr);
            cnt++;

            preEnd=m.end();
//            //(String.format("start=%d,end=%d,subString=%s",m.start(),m.end(),
//                   s));
        }

        if(preEnd!=literal.length()){
            String subStr=literal.substring(preEnd,literal.length());
            if(!subStr.isEmpty()){
                cnt+=subStr.length();
                subStr= subStr.replace("\'","\'\'");
                builder.append(getPrefix(builder)+ getQuotedStr(subStr));
            }
        }
        /*加上结束符号*/
        builder.append(getPrefix(builder)+" 0");
        cnt++;

        return new SpiltResult(cnt,builder);
    }

    private static String getPrefix(StringBuilder builder){
        return builder.isEmpty()? "" : ",";
    }

    private static String getQuotedStr(String str){
        String q="\'";
        return q+str+q;
    }

    /**
     * 转义字符->十进制ASCII码
     */
    private static String escapeToHex(String s){
        char c=s.charAt(1);
        int n;
        switch (c) {
            case 'a':
                n = 7;
                break;
            case 'b':
                n = 8;
                break;
            case 'f':
                n = 12;
                break;
            case 't':
                n = 9;
                break;
            case 'v':
                n = 11;
                break;
            case 'n':
                n = 10;
                break;
            case 'r':
                n = 13;
                break;
            case '\'':
                n = 39;
                break;
            case '\"':
                n = 32;
                break;
            case '\\':
                n = 92;
                break;
            case '?':
                n = 63;
                break;
            /*十六进制*/
            case 'x':{
                String sub = s.substring(2);
                //只取一个字节
                if (sub.length() > 2) {
                    sub = sub.substring(sub.length() - 2, sub.length());
                }
                char firstChar = sub.charAt(0);
                if (firstChar >= 'a' && firstChar <= 'f' || firstChar >= 'A' && firstChar <= 'F') {
                    sub = "0" + sub;
                }
                return " " + sub + "H";
            }
            default:{
                /*八进制*/
                if(c>='0'&&c<='7') {
                    String sub=s.substring(1);
                    return " "+sub+OCTAL_POSTFIX;
                }else{
                    n=0;
                }
            }
        }
        return " "+n;
    }

    /**
     * 去除行尾注释部分
     */
    public static String removeLineComment(String line){
        if(line==null)
            throw new RuntimeException();
        Pattern pattern=Pattern.compile(LINE_COMMENT);
////        //(line);
        Matcher matcher=pattern.matcher(line);
        String comment;
        if(matcher.find()){
           int cnt=matcher.groupCount();
           for(int i=0;i<=cnt;i++){
               String s=matcher.group(i);
//               //(String.format("Group<%d>:   %s",i,s));
           }
           comment=matcher.group(4);
           int idx=line.indexOf(comment);
           line=line.substring(0,idx);
        }
        return line;
    }

}
