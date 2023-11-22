package ASM.Constants;

public enum RegisterState {
    EMPTY,
    //储存地址
    M,
    //只储存低字节
    B,
    //令有高字节寄存器,但是该状态表示此寄存器储存低字节
    HB,
    HH,
    //储存高字节
    H,
    //储存子
    W,
    //储存双子
    D,
    //储存四字
    Q,

    //单精度
    SS,
    //双精度
    SD,
}
