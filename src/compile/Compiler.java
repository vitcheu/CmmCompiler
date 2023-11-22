package compile;

import ASM.CodeGenerator;
import AST.AST;
import CompileException.CompileError;
import CompileException.errorHandler;
import IR.Block;
import IR.Optimise.IROptimizer;
import Parser.Parser;
import Semantic_Analysis.*;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

import static compile.Constants.*;

public class Compiler {
    public static String sourceFile=sourceFileName;
    public static void main(String[] args) throws FileNotFoundException {
        if(args.length>0)
            sourceFile=args[0];
        Parser parser = new Parser(sourceCodesPath+sourceFile+SOURCE_FILE_POSTFIX);
        System.out.printf("\033[34mCompiling: %s\n\033[0m",sourceFile);

        try {
            parser.analysis();
        }catch (CompileError e){
            throw new RuntimeException("词法分析或语法分析失败,请查看scan error 或 syntax error.");
        }
        try(PrintWriter pw=new PrintWriter(outputPath +"Semantic/Semantic Error.txt");
            PrintWriter pw2=new PrintWriter(outputPath +"Semantic/Resolve Log.txt");
            PrintWriter pw3=new PrintWriter(outputPath +"Semantic/中间代码.txt");
            PrintWriter codesWriter=new PrintWriter(outputPath +"Code/目标代码.txt")) {
            errorHandler e=new errorHandler(pw);
            AST ast=parser.getAst();

            //解决变量的引用问题
            LocalResolver resolver=new LocalResolver(e,pw2);
            resolver.resolve(ast);

            //解决类型的引用问题
            TypeResolver typeResolver=new TypeResolver(e,pw2);
            typeResolver.resolve(ast);

            //类型定义检查
            DefinedTypeChecker checker=new DefinedTypeChecker(e,pw2,typeResolver.getTypeTable());
            checker.checkDefinedType(ast);

            //类型检查
            TypeChecker typeChecker =new TypeChecker(e,pw2,typeResolver.getTypeTable(),
                    resolver.getGlobalTable());
            typeChecker.checkAst(ast);
            e.flush();
            if(e.errorOccurred()){
                throw new RuntimeException("编译失败,请查看日志.");
            }

            //中间代码生成
            IRGenerator.typeTable=typeResolver.getTypeTable();
            IRGenerator irGenerator=new IRGenerator(e,pw3);
            irGenerator.generate(ast);

            final List<Block> blocks=irGenerator.getBlocks();
            IROptimizer optimizer=null;
            if(CompilerOption.IROptimized){
                //机器无关优化
                optimizer=new IROptimizer();
                optimizer.optimize(blocks);
            }
            Block.verbose=true;
            for(Block block:blocks){
                block.setActives(pw3);
            }

            //目标代码生成
            CodeGenerator codeGenerator=new CodeGenerator(codesWriter,
//                   optimizer==null?blocks:optimizer.getBlock(),
                    blocks,
                    resolver.getGlobalTable());
            codeGenerator.generate();

            System.out.println("\nDone!");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
