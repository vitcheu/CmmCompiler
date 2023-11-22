package compile;

public class Constants {
    public static final int numOfState=321;
    //表中的数量+1
    public static final int numOfTerminal=75;
    //表中的数量+1
    public  static final int numOfNonTerminal=73;
    public static final int errNo=-1;
    public static final int acc=-2;
    public static final int startState=0;

    public static final String projectDir ="./";
    public static final String outputPath =projectDir + "/Production/";
    public static final String outputCodePath=outputPath+"Code/";
    public static final String parserOutputPath=outputPath+"Syntax/";
    public static final String optimizePath=outputPath+"Optimize/";


    public static final String resourcePath= projectDir +"resource/";
    public static final String LibraryPath= resourcePath+ "import/";
    public static final String parserResPath=resourcePath+"Parser/";
    public static final String sourceCodesPath=resourcePath+"CmmCode/";


    public static final String HEADER_FILE_POSTFIX=".h";
    public static final String SOURCE_FILE_POSTFIX=".c";
    public static final String ASM_FILE_POSTFIX=".asm";

    public static final String terminalFileName="terminal.tsv";
    public static final String sourceFileName="test";
    public static final String optimizeLog=optimizePath+"Optimizer Log.txt";

}
