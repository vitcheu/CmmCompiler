package ASM.Constants;
public enum OpBase {

    //算术指令
    ADD,
    SUB,
    MUL,
    IMUL,
    DIV,
    IDIV,
    INC,
    DEC,
    NEG,
    XOR,

    //符号扩展
    CDQ,
    CBW,
    CWD,
    CQO,
    CWDE,
    CDQE,



    //移位指令
    SHL,
    SHR,
    ROL,
    ROR,
    RCL,
    RCR,
    SAL,
    SAR,

    //位运算
    AND,
    OR,
    NOT,
    TEST,
    CMP,

    //跳转指令
    JZ,
    JNZ,
    JC,
    JNC,
    JA,
    JAE,
    JB,
    JBE,

    JG,
    JGE,
    JL,
    JLE,
    //根据条件码设置值
    set,

    LOOPZ,
    LOOPNZ,

    RET,
    JMP,
    CALL,

    //数据传送指令
    MOV,
    LEA,
    MOVZX,
    MOVSX,
    CMOV,//条件传输指令
    MOVSXD,
    LAHF,
    SAHF,
    XCHG,


    //对齐指令
    ALIGN,

    //栈指令
    POP,
    PUSH,


    /**
     *
     * 浮点指令
     *
     */
    FLD,
    //1
    FLD1,
    //log2(10)
    FLDL2T,
    //log2(e)
    FLDL2E,
    //Π
    FLDPI,
    //log10(2)
    FLDLG2,
    //loge(2)
    FLDLN2,
    //0
    FLDZ,

    FST,
    FCHS,
    FADD,
    FSUB,
    FSUBR,
    FMUL,
    FDIV,
    FDIVR,

    //标量浮点转换指令
    CVT,
    //sse标量浮点比较
    COMI,

    /**
     * 字符串指令
     */
    MOVS,
}
