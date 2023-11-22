package Parser.Entity;

public class Token extends Symbol {
    public final static String Right_Parenthesis="(";
    public final static String  Left_Parenthesis=")";
    public final static String  ID="id";
    public final static String Open_Bracket="[";
    public final static String Asterisk="*";
    private String image ="0";
    private static int tid=-1;
    private Location location;


    /**
     * 词法分析器使用
     *保留字,标点符号,运算符等无词素值
     */
    public Token(String value, Location location) {
        super(value);
        this.location=location;
//        setId(tid++);
    }


    /**
     *
     * @param value 语法分析所用的词法单元值,如id,INTEGER
     * @param image 词法单元的词素值,如a,18
     * @param location 在源文件的位置,行号从1开始,行偏移量从0开始
     */
    public Token(String value, String image, Location location) {
        super(value);
        this.image = image;
        this.location=location;
//        setId(tid++);
    }

    /**
     * 语法分析器读入终结符列表使用
     * 给终结符编号
     */
    public Token(String value){
        super(value);
        setId(tid++);
    }

    @Override
    public Location getLocation() {
        return location;
    }

    public String getImage() {
        return image;
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

    //    public static final Token AT = new Token("@");//tid=-3
//    public static final Token SHARP = new Token("#");//-2

    //表示空产生式体
    public static final Token EPSILON = new Token("ε");//-1
    //表示输入流结束
    public static final Token EOF = new Token("$");//0

}
