import stdio;
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

// int fib(int n)
// {
//     // printf("n=%d",n);
//     if (n == 2 || n == 1) {
//         //     int j=1,p;
//         //     int u;
//         //     j++;
//         //    while(j>1&&p<2){
//         //        int z=19,z2,z3;
//         //        bool bl=j&&u>2;
//         //        j+=10;
//         //        if(j>100)
//         //         break;
//         //    }

//         return 1;
//     } else
//         return fib(n - 1) + fib(n - 2);

// }

// void testLiteral(void)
// {
//     int a = 0xffff;
//     int b = 07777, c = '\033';
//     char* strPtr = "Hello,I'm a string pointer!";
//     char[32] str = "hello,world!num=\xfffff8,num2=\1011";
//     printf("str=%s\n", str);
//     printf("*strPtr=%s\n", strPtr);
//     printf("a=%d,b=%d\n", a, b);

//     // char[256] buf;
//     // gets(buf);
//     // printf("buf=%s",buf);
// }

// static int LongFunc(void)
// {
//     int a, c;
//     char ch;
//     int x, y, i;
//     int sum = 0;
//     int* p1, p2;
//     int[2][3][2] b;
//     struct st stVar;
//     struct st* ptr1, ptr2;
//     struct st2 st2Var;
//     struct data st3Var;
    
//     a= x>>y;
//     a=(int)(1.2*5+(x>>y))*((12-2)+a%3+(int)1.2);
//     // // b=&a;
//     // // a=*b;
//     b[a][0][2]=0;
//     a=a+a*a+1;
//     if(!(x||x>200&&x!=y)&&1)
//         x = (y > 1) && (x < 1);
//     if(a<-1)
//         x=(y>1)&&(x<1);
//     else {
//         x=3;
//         a=1;
//     }
//     printf("%x=%d" 
//             ",y=%d\n",x,y);

//     // 测试while
//     while (x || x < 10) {
//         a += !(a + 1 && y) + 1;
//         *p1-- = 3;
//         *--p2 = 5;
//         if (a = 2) {
//             break;
//         }
//     }

//     unsigned long long ul;
//     signed short shVar = 1;
//     // x++;

//     // do {
//     //     x = x + 1;
//     // }while(x > 10);

//     // for (int ii = 0, j = 1;ii < 10;ii = ii + 1, j++)
//     //     if (sum % 2 == 0)
//     //         sum = sum + ii;
//     // // // 指针加整数
//     // p2 = p1 + 2;
//     // p2 = 2 + p1;
//     // int* p3 = p2;
//     // // 指针相加
//     // // p1=p1+p2;
//     // // 指针相减
//     // x = (int) (p2 - p1);

//     // // 测试三元表达式
//     // //三元表达式同样需要以上处理,避免基本快碎片化.
//     // x = (a % 2 == 0) ? ((y > 10) ? y + 1 : y - 1) : 0;
//     // // b[1][2]=1;

//     // // 测试成员访问
//     // y = (int) (ptr2->member6)[1][2];
//     // // x = (int) ptr1->member3;

//     // stVar.member1=3;
//     // // 测试链式成员访问
//     // (stVar.st2Mem.st3Mem.bl)[2] = sizeof(a)+x+stVar.member5[2]+y;
//     // stVar.st2Mem.mem21 = sizeof(char);

//     return 0;
// }

union uni {
    char c1;
    int i1;
    float f1;
    double d1;
};

// void testLea(void){
//     char ch = 'c';
//     union uni u = { 'a',3,1.3 };
//     short st = -2;
//     int* p = &u.i1;
//     int i1 = 0;
//     // u.i1=1;
//     // u.f1=1.1;
//     // u.c1='s';

//     union uni* uPtr = &u;
//     printf("u.i1=%d", uPtr->i1);
//     printf("u.f1=%f", uPtr->f1);
//     printf("u.c1=%c", uPtr->c1);


//     float f4 = .000000;
//     int i6 = -0;

// }


// int test(void){
//     int a;
//     // int *p=&a;
//     int *p;
//     p[3]=5;
//     return p[2];
// }

// void testShift(void)
// {
//     int i, n = 10;;
//     for (i = -n * 100;i <= n * 100;i += 100) {
//         int r = i / 64;
//         printf("i=%d,(%x)\n", i, i);
//         printf("i/2=%d,(%x)\n", r, r);
//         printf("------------\n");
//         printf("     \n");
//     }
// }

int global=8;
double gloDouble=1.2;
void testLiteral(void){
    ;
}

struct  data {
    bool[10]  bl;
    int intField;
    int* sp;
    int e;
    char[256] rec;
    char* send;
};


void asmMain(void){
    // testShift();
    // testFuncPtr();
    int a;
    char* str;
    gets(str);
    printf("str=%s",str);
    // scanf("%d",&a);
    // printf("a=%d",a);
}


void testArr(void)
{
    // int c=1;
    int[2][3] a;
    int i = 0;
    a[1][i] = global++;
    printf("a[1][i]=%d", a[1][i]);
}

void testStaticVar(void)
{
    static int a = -1;
    static int[5][6] staArr;
    static struct data staData;
    int native = 0, t;
    int[3] arr;
    int* p = arr;
    int two = 2;
    // arr[0]=0;arr[1]=1;arr[2]=2;
    //一个右值的++,实际上不能通过语义检查
    // (t=a++)++;
    // a++;
    // arr[0] = -12;arr[1] = 10;
    printf("before,p=%p,*p=%d", p, *p);
    printf("Now (*p++=a++)=%d",*p++=a++);
    printf("after,*(p-1)=%d",*(p-1));
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

}

// void testFuncPtr(void){
//     float (int)* fPtr=&f1;
    
//     int a=5;
//     float ret=(*fPtr)(a);
// }

// /**
//  * 测试函数指针的返回和赋值
// */
// float f1 (int a){
//     printf("a=%d",a);
//     return (float)(a);
// }

// float f2(int a){
//     return a+2;
// }

// float f3(double a){
//     return a*a;
// }