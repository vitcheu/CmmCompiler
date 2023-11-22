package Parser;

import Parser.Entity.Nonterminal;
import Parser.Entity.Token;
import compile.Constants;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import static java.lang.Integer.parseInt;

public class AnalysisTable {
    private final Parser parser;
    private int[][] table;
    private static final String tsvSplit="\t";
    private int actionLen;

    public AnalysisTable(int rowLen, int colLen, int actionLen,Parser parser){
        this.parser=parser;
        table=new int[rowLen][colLen];
        this.actionLen=actionLen;
        load();
    }

//    public static void main(String[] args) {
//        AnalysisTable analysisTable=new AnalysisTable();
//        analysisTable.load();
//    }
    /**
     *从文件中加载语法分析表至内存
     */
    public void load(){
        try(BufferedReader br=new BufferedReader(new FileReader(Constants.parserResPath+"raw table.tsv"))){
            String line;
            String[] row;
            int r=0;
            while ((line=br.readLine())!=null){
                row=line.split("\t");
                //过滤掉每行第一个单元
                for(int i=0;i<row.length;i++){
                    table[r][i]= parseInt(row[i]);
                }
                r++;
            }

            //检查导入效果
//            for(int i=0;i<10;i++){
//                for(int j=0;j<5;j++){
//                    System.out.print(table[i][j]+tsvSplit);
//                }
//                //();
//            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     *获取语法分析表中第r行第c列的动作
     *有些动作是语义相关的,需要从parser中获得上下文信息
     * @return 执行动作后的状态
     */
    private int getAction(int r,int c){
        if(r>=table.length||c>=table[0].length){
            throw new  ArrayIndexOutOfBoundsException();
        }else{
            return table[r][c];
        }
    }

    public int getAction(int r,Token token,int tokenId){
        String value=token.getValue();
        String lxr=token.getImage();

        /*利用上下文信息解决语法冲突*/
        //在栈状态:[@135,       []中,
        // 解决    reduced typeName	->	id	 (pid=171)
        //和      reduced primary_expr	->	id (pid=139)的冲突
        if(r==158&&
                (value.equals(Token.Right_Parenthesis)//')'
            ||value.equals(Token.Left_Parenthesis)//'('
            ||value.equals(Token.Open_Bracket)//'['
            ||value.equals(Token.Asterisk))//'*'
        ){
            /*获取栈顶的id*/
            Token idTok=(Token) parser.getSymbol(0);
            //栈顶符号是一个类型名,那么应该执行reduced typeName->	id规约
            int desProductionId = parser.containTypeName(idTok.getImage())?171:139;
            return getReducedProductionActionId(desProductionId);
        }

        //在栈状态[@205,      id]中,
        //解决	shift to 216/80 和	reduced storage	->	 (pid=20)  的冲突
        else if((r==224||r==274)
                &&value.equals(Token.ID)){
            //当前输入符号是一个类型名,那么应该执行reduced storage	->	ε规约
            if(parser.containTypeName(lxr)){
                int  desProductionId=20;
                return getReducedProductionActionId(desProductionId);
            }else{
                //shift
                return r==224?263:113;
            }
        }

        return getAction(r,tokenId);
    }

    public int getAction(int r, Nonterminal nonterminal){
        return getAction(r,nonterminal.getId()+ Constants.numOfTerminal);
    }

    private int getReducedProductionActionId(int productionId){
        return productionId+ Constants.numOfState;
    }
}
