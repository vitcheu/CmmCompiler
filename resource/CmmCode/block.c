import stdio;

int asmMain(void){
    int a,b,c,d,e;
    // a=b+c;//a=5
    // b=a-d;//b=1
    // c=b+c;//c=4
    // d=a-d;//d=1
    // printf("a=%d,b=%d,c=%d,d=%d\n",
    //     a, b, c, d);


    int x,y=27,z;
    int i=1,j=2;
    int[3] arr = { 10,20,30 };
    a=arr[i];
    x=arr[i];
    arr[j]=y+x;
    z=arr[i];
    printf("x=%d,arr[j]=%d,z=%d\n",x,arr[j],z);

    int* brr=0+arr,p1,p2;
    z=brr[i]+a;//z=20
    // char* str = "h//elloaabc//";////th//is is"Hello"string;
    // printf(str);
    brr[i]=-y+z;//brr[2]=-7
    arr[j]=-y+z;

    z=-y+z+brr[i];//-7+1=-6
    p1=brr+1;
    y=p1[0]+z+arr[i];//20-6=14

    p1[1]=3;//p1[1]=brr[2]=arr[2]
    printf("p1[0]=%d,p1[1]=%d\n",p1[0],p1[1]);
    printf("p1[1]=%d\n",p1[1]);
    printf("p1=%p,brr=%p,arr=%p\n\n\n",p1,brr,arr);

    z=y+p1[0];//z=14+20=34
    x=brr[i];//x=20
    z=brr[i]+z-2;//z=20+34-2=52
    // y=z*11;
    // y=(a+++10-1)*y;//y=29
    // z=30*30;

    printf("x=%d,brr[j]=%d,z=%d,y=%d\n\n",x++,brr[j],++z,y=z+1);

    // x = *brr + 1;
    // p1 = &y;
    // p2 = &x;
    // y=*p1+x+*p2;
    
    // //write
    // *p1=3;
    // x=*p1+y+*p2;
    // // *p2=*p1;

    // // p1=brr;
    // printf("x=%d\t &x=%p \t p1=%p \t p2=%p \t *p1=%d \t *p2=%d\n",
    //          x,&x,p1,p2,*p1 ,*p2);
    // printf("&arr[1]=%p,*&arr[1]=%d\n\n",&arr[1],*&arr[1]);

    struct st st = { 3,2.7,"Hello Compiler"};
    // x=st.a;
    // printf("x=%d,st.a=%d,&st.a=%p,*&st.a=%d\n\n"
    //             ,x,st.a,&st.a,*&st.a);
    // printf("st.b=%f,st.msg=%s\n",st.b,st.msg);
    st.p=arr;
    st.a=(st.p)[0];
    a=st.a;
    b=(int)st.b;
    c=(int)(st.b+st.a);
    // (st.p)[0]=1;
    printf("st.p=%p,st.p[0]=%d\n",st.p,(st.p)[0]);
    char* str1="hello";
    char[20] str2="world";
   
   return 0;
}

struct st{
    int a;
    float b;
    char[27] msg;
    int * p;
};