package Parser;

import AST.AST;
import AST.DEFINE.DeclararedNode;
import AST.STMT.ImportNode;
import CompileException.CompileException;
import CompileException.CompileWarning;
import compile.Constants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.*;

import static compile.Constants.parserOutputPath;

/**
 *
 */
public class LibraryLoader extends Parser{
    private static final String loadErrPath=parserOutputPath+"Loading Error.txt";
    private static final String loadingLogPath =parserOutputPath+"Loading Log.txt";
    private static final List<String> Candidate_Paths=new ArrayList<>();
    private static final  PrintWriter loadingLog;
    private static final  PrintWriter errWriter;
    static {
//        String systemPaths=System.getenv("PATH");
//        String[] sysPaths=systemPaths.split(";");
//        Candidate_Paths.addAll(Arrays.stream(sysPaths).filter(
//                s -> !s.isEmpty() && !s.isBlank()).toList());
        String pwd= Paths.get("").toAbsolutePath().toString();
        Candidate_Paths.add(pwd);
        Candidate_Paths.add(Constants.LibraryPath);
        try {
            errWriter=new PrintWriter(loadErrPath);
            loadingLog =new PrintWriter(loadingLogPath);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    private static HashMap<String,List<DeclararedNode>> loadedLibs=new LinkedHashMap<>();
    private static LinkedList<String> loadingLibs=new LinkedList<>();

    public LibraryLoader(String sourceFilePath) throws FileNotFoundException {
        super(sourceFilePath);
        verbose=false;
    }

    private static List<DeclararedNode> loadFile(String filePath) throws CompileException, FileNotFoundException {
            if(loadingLibs.contains(filePath)){
                String msg=filePath+"被重复导入";
                loadingLog.println(msg);
                throw new CompileWarning(msg);
            }
            /*返回缓冲区中的内容*/
            else if(loadedLibs.containsKey(filePath)){
                loadingLog.println("缓冲区已存在"+filePath+"的内容");
                return loadedLibs.get(filePath);
            }

            loadingLog.println(String.format("\n--------------<Loading:%s>--------------------", filePath));
            loadingLibs.addLast(filePath);

            AST ast1=null;
            String errMsg="\n<!> failed to load "+filePath+" . Reason:";
            try {
                LibraryLoader loader = new LibraryLoader(filePath);
                loader.analysis(errWriter, loadingLog);
                ast1 = loader.getAst();
            }catch (Exception e){
                String reason=e.getMessage();
                loadingLog.println(String.format("%s%s",errMsg,reason));
                throw e;
            }

            loadingLog.println("#Added Items:");
            List<DeclararedNode> decls = ast1.getDeclarations();
            List<DeclararedNode> outputLst=new ArrayList<>(decls);
//            outputLst.sort(Comparator.comparing(DeclararedNode::toString));
            outputLst.forEach(decl -> loadingLog.println(decl));
            loadingLog.println(String.format("--------------<Loaded:%s!>--------------------", filePath));

            loadingLibs.removeLast();
            loadedLibs.put(filePath,decls);
            return decls;
    }

    public static List<DeclararedNode> resolveImport(ImportNode impt) throws CompileException, FileNotFoundException {
        String lib=impt.getPath();
        List<DeclararedNode> nodes= LibraryLoader.loadFile(searchLib(lib));
        return nodes;
    }

    /**
     * 搜索导入文件
     * @return 导入文件的路径
     */
    private static String searchLib(String lib) throws FileNotFoundException {
        for(String path:Candidate_Paths){
            File file=new File(path
                    +(path.endsWith(File.separator)?"":File.separator)
                    +lib);
            if(file.exists()){
                return file.getPath();
            }
        }
        throw new  FileNotFoundException("找不到"+lib+"的路径");
    }

    public static void close() {
        loadingLibs.clear();
        loadedLibs.clear();
        errWriter.close();
        loadingLog.close();
    }
}
