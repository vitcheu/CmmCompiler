package compile;

/**
 * 2023年11月4日 编译器优化选项
 */
public class CompilerOption {
    /**
     * 是否进行机器无关优化
     */
    public static boolean IROptimized=true;

    public static boolean Debug =false;
    /**
     * 是否输出源文件行号信息
     */
    public static boolean OutputSourceFileLine=true;

    /**
     * CPU是否处于64位长模式
     */
    public static boolean  LARGE_ADDR=false;
}
