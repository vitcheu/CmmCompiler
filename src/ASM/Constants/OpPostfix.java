package ASM.Constants;

public enum OpPostfix {

    //整数
    I,
    //BCD数
    B,
    //弹出栈
    P,

    //扩展宽度传输
    SXD,

    //标量单精度浮点
    SS,
    //标量双精度浮点
    SD,

    /**
     * 比较结果后缀
     */
    l,
    b,
    le,
    be,
    g,
    a,
    ge,
    ae,
    z,
    nz,



    /**
     *   标量浮点转换指令后缀
     */
    //整数转换为浮点数
    SI2SS,
    SI2SD,
    //浮点数转换为整数
    SS2SI,
    SD2SI,
    //截断转换
    TSS2SI,
    TSD2SI,
    //浮点数精度转换
    SS2SD,
    SD2SS,

    /**
     * 64位通用寄存器/内存操作数/sse寄存器数据传输指令后缀
     */
    Q,
    D,


    /**
     * sse矢量传输指令后缀
     */
    APS,
    APD,
    UPS,
    UPD,

}
