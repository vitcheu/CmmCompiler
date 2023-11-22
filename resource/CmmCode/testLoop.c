import stdio;

void asmMain(void){
    int [10][10] a;
    int i=0;
    for(i=0;i<10;i++){
        int x=1;
       for(int j=0;j<i;j++){
         a[i][j]=0;
         x=a[i][j];
       }

       while (1) {
           if (a[0][i] > 10)
               break;
           x++;
           i++;
           printf("hello\n");
           printf("i=%d,x%d\n,a=%p", i, x,a);
       }
       i++;
    }

    printf("hello,out of loops\n");
    printf("a[0][i]=%d\n",a[0][i]);
}