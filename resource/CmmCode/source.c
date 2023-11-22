import stdio;
import erron;
import strings;
import string;
import unistd;

extern int  N,M;

//全局变量
const int e = 25;
bool b = 0;
int[25] fibArr;

int global =1, gb = -2;
double gloDouble=3.13;
char gloChar = 'd';
int[10] gloArr;
static struct st gloStruct;

struct  st {
    int member1;
    char member2;
    float member3;
    bool member4;
    int* member5;
    float[2][3] member6;
    struct st2 st2Mem;
};

struct st2 {
    int mem21;
    struct data st3Mem;
};

union uni{
    char c1;
    int i1;
    float f1;
    double d1;
};

union charU{
    char c1;
    char c2;
};


struct ds{  
    int i1;
    float f1;
    char i3;
    short s1;
    int i4;
    char c1;
    char * cp1;
    int[5] arr;
    double d1;
};

const  int conInt=9;
typedef int* int_P;
typedef int[2][3] int_arr_t;

void testUnion(void){
    char ch='c';
    union uni u={'a',3,1.3};
    short st=-2;
    int* p=&u.i1;
    int i1=0;

    printf("p=%p",p);

    // u.i1=1;
    // u.f1=1.1;
    // u.c1='s';
    bool bv=p;
    printf("bv=%d",bv);

    union uni* uPtr=&u;
    printf("u.i1=%d",uPtr->i1);
    printf("u.f1=%f",uPtr->f1);
    printf("u.c1=%c",uPtr->c1);

    struct ds dVar={1,1.1,'i',255,-9,'S'};
    for(int i=0;i<3;i++){
        dVar.arr[i]=i*2;
    }
    printf("dVar.i1=%d",dVar.i1);
    printf("dVar.f1=%f",dVar.f1);
    printf("dVar.i3=%c",dVar.i3);
    printf("dVar.s1=%d",(int)dVar.s1);
    printf("dVar.c1=%c",dVar.c1);
    printf("dVar.arr[2]=%d",dVar.arr[2]);

    int a=-1;
    union charU cu={'u'};
    double[2] dArr={-0.2,0.2};
    // printf("dArr[0]=%lf",dArr[0]);
    // printf("dArr[1]=%lf",dArr[1]);

    float f4=.000000;
    int i6=-0;
    //测试对bool变量的赋值
    bool bl=p,bl2=dArr[1],bl3=(float)dArr[0],bl4=f4;
    bool bl5=0,bl6;
    printf("bl=%d,bl2=%d,bl3=%d",(int)bl,(int)bl2,(int)bl3);
    printf("bl4=%d",(int)bl4);
    printf("bl5=%d",(int)bl5);
    printf("bl6=%d",(int)(bl6=i6));

    //测试自定义类型
    int[10] I_arr={0,1,2,-3};
    for(int i=4;i<10;i++){
        I_arr[i]=i*(-1);
    }
    int_P p2=I_arr;
    printf("p2=%p,*p2=%d",p2,*p2);
    int_arr_t[3][4]* arr_ptr;
    for(int_P p3=p2;p3<p2+10;p3++ ){
        printf("p3=%p,*p3=%d",p3,*p3);
    }


    printf("N=%d",N);
    N=M+2;
    printf("M=%d,N=%d",M,N);
}



int asmMain(void)
{
    int a = -1;
    int n = 2, m = 10;
    float f1 = -0.0111111111, f2 = 2;
    bool bl;
    int c = 0;
    int i;
    int** p;
    static int staVar = 3;
    struct data* baseP;
    char ch = 'a';
    static struct data st;
    struct data localData;

    st.intField = -2;
    localData.intField = global++;
    localData.sp = &global;

    baseP = &localData;
    p = &localData.sp;

    void *gPtr;
    gPtr=&gloDouble;

    testDeref();

    return 0;
}


int loopFib(int n)
{
    if (!b) {
        fillArr();
        b = 1;
    }
    if (1 <= n && n <= e)
        return fibArr[n];
    else {
        printf("n不在合法的范围:%d!\n",n);
        return -1;
    }
}
void fillArr(void)
{
    int i;
    for (i = 1;i <= e;i++) {
        if (i == 1 || i == 2) {
            fibArr[i] = 1;
        } else {
            fibArr[i] = fibArr[i - 1] + fibArr[i - 2];
        }
    }
}

void testArr(void){
    // int c=1;
    int[2][3] a;
    int i=0;
    a[1][i]=global++;
    printf("a[1][i]=%d",a[1][i]);
}

void testStaticVar(void){
    static int a=-1;
    static int[5][6] staArr;
    static struct data staData;
    int native=0, t;
    int[3] arr;
    int* p=arr;
    int two=2;
    // arr[0]=0;arr[1]=1;arr[2]=2;
    //一个右值的++,实际上不能通过语义检查
    // (t=a++)++;
    // a++;
    arr[0]=-12;arr[1]=10;
    printf("before,p=%p,*p=%d",p,*p);
    // printf("Now (*p++=a++)=%d",*p++=a++);
    // printf("after,*(p-1)=%d",*(p-1));
    // printf("after, p=%p,*p=%d",p,*p);
    // printf("e");
    // staArr[0][0]=a++;
    // staArr[0][1]=-2;
    // staArr[0][2]=-3;
    // printf("staArr[0][a]=%d",staArr[0][a]);
    // printf("Now g is %d:",t=global++);
    // printf("(t=a++)=%d",t=a++);
    // printf("Now t=%d", t);
    // printf("native=%d",native++);

    //测试静态结构体
    staData.e=(global++)+(gloArr[arr[2]]);
    staData.bl[0]=global%2==1;
    staData.sp=&global;
    staData.send=&gloChar;
    printf("staData.e=%d",staData.e);
    contentOfStruct(&staData);
}

struct  data {
    bool[10]  bl;
    int intField;
    int* sp;
    int e;
    char[256] rec;
    char* send;
};


void contentOfStruct(struct data * strPtr){
    printf("**********************");
    printf("bl[0]=%d",strPtr->bl[0]);
    printf("intFiled=%d",strPtr->intField);
    printf("*sp=%d",*strPtr->sp);
    printf("sp=%p",strPtr->sp);
    printf("e=%d", strPtr->e);
    printf("rec[0]=%c",(strPtr->rec)[0]);
    printf("*send=%c",*strPtr->send);
    printf("**********************");
}

// int * globalIntPtr;

void testDeref(void){
    int a=5;
    int* p1,p2;
    struct data localData;
    localData.intField=1;
    localData.sp=&a;
    localData.e=*localData.sp+1;
    localData.rec="Hello world!";
    localData.send="Hello my Dummb Compiler!";


    struct data * dataPtr=&localData;
    int [2][3]*[4] ptrArray;
    int [2][3] arr1,arr2;
    arr1[0][2]=-6;

    /* 对全局变量的解引用 */
    // *globalIntPtr=*p1;
    
    /* 对成员访问结果解引用 */
    *dataPtr->sp=*&dataPtr->rec[2];
    /* 对成员访问得到的指针进行数组访问*/
    //实际上不能这么做,因为send所指向的字符串是只读的
    // dataPtr->send[8]='c';

    /* 对数组访问得到的指针赋值 */
    ptrArray[2]=&arr1;
    /* 对数组访问得到的指针进行解引用再访问得到的数组*/
    (*ptrArray[2])[0][2]=-2;
    /* 对数组访问得到的指针直接进行数组访问 */
    ptrArray[2][0][0][2]=-3;


    /*检查上述两次访问是否是同一个位置*/
    printf("( * ptrArray[1] ) [0][2] = %d", (*ptrArray[2])[0][2]);
}

extern void read(void);
extern void read1(void);


// 测试目标代码
int fun(int argc, char[10][] argv, char param1, bool param2, int param3)
{
    // int a=1,b=1,c=1,d=1,e=1,g,h;
    int a = -1, b, c, d, e, g, h;
    int[3][2] arr;
    float[10] x;
    int* p = &a, q;
    char ch = 'c';
    bool bl;
    float f1 = 1.0, f2 = 3.14, f3 = -2.3, f4, f5;
    struct data st;
    struct data* stPtr = &st;
    int i;

    //指针数组
    int* [3] ptrArray;
    int[2][4] * [3][7] matricxPointerOfMaticex;
    int [2][4]element;//三个2*4维数组
    int[5] * arrayP;
    char[10][3] promts;
    char[256] str = "Hello World!\n";
    printf(str);


    *p = 5;
    // 测试多维数组访问
    arr[2][1] = *p;

    // 测试指针数组的访问
    ptrArray[2] = p + 1;//同时还有指针运算
    // 指针元素的数组运算
    matricxPointerOfMaticex[1][2]=&element;//将不可修改的左值:[数组名]赋给指向统一尺寸数组的[指针]

    // 数组指针的参数传递
    // stFun(stPtr,arrayP);

    i = fun(a, promts, (char) b, (int) f2, (int) x[2])
        + (int) (Calfloat(2.14, f2, &f5));
    return 0;//str[1]实际为*(str+1*1);
}


float Calfloat(float a, float b, float* c)
{
    float ret = a * b + *c;
    float[5] fArr;
    *c = a - b + Calfloat(b, *&(ret) / 1.9, fArr);


    Calfloat(b, Calfloat(b, c[1], &ret + 1), &fArr[4]);
    return ret + *c;
}

// int g=1;
// // float gloable=1.0;