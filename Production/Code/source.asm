
		.const
__str0	byte	'p=%p', 0
__str1	byte	'bv=%d', 0
__str2	byte	'u.i1=%d', 0
__str3	byte	'u.f1=%f', 0
__str4	byte	'u.c1=%c', 0
__str5	byte	'dVar.i1=%d', 0
__str6	byte	'dVar.f1=%f', 0
__str7	byte	'dVar.i3=%c', 0
__str8	byte	'dVar.s1=%d', 0
__str9	byte	'dVar.c1=%c', 0
__str10	byte	'dVar.arr[2]=%d', 0
__str11	byte	'bl=%d,bl2=%d,bl3=%d', 0
__str12	byte	'bl4=%d', 0
__str13	byte	'bl5=%d', 0
__str14	byte	'bl6=%d', 0
__str15	byte	'p2=%p,*p2=%d', 0
__str16	byte	'p3=%p,*p3=%d', 0
__str17	byte	'N=%d', 0
__str18	byte	'M=%d,N=%d', 0
__str19	byte	'n不在合法的范围:%d!', 10, 0
__str20	byte	'a[1][i]=%d', 0
__str21	byte	'before,p=%p,*p=%d', 0
__str22	byte	'staData.e=%d', 0
__str31	byte	'**********************', 0
__str24	byte	'bl[0]=%d', 0
__str25	byte	'intFiled=%d', 0
__str26	byte	'*sp=%d', 0
__str27	byte	'sp=%p', 0
__str28	byte	'e=%d', 0
__str29	byte	'rec[0]=%c', 0
__str30	byte	'*send=%c', 0
__str32	byte	'Hello world!', 0
__str33	byte	'Hello my Dummb Compiler!', 0
__str34	byte	'( * ptrArray[1] ) [0][2] = %d', 0
__str35	byte	'Hello World!', 10, 0
__f0	real4	3.13
__f01	real8	3.13
__f1	real4	1.3
__f10	real4	2.14
__f11	real4	1.9
__f2	real4	1.1
__f3	real4	-0.2
__f4	real4	0.2
__f41	real8	0.2
__f5	real4	0.0
__f6	real4	-0.011111111
__f7	real4	1.0
__f8	real4	3.14
__f9	real4	-2.3
e	dword	25
conInt	dword	9
NULL	qword	0
real8@ZERO	real8	0.0

		.data
b	byte	0
fibArr	dword	25 dup (0)
global	dword	1
gb	dword	-2
gloDouble	real8	3.13
gloChar	byte	'd'
gloArr	dword	10 dup (0)
gloStruct_$_	byte	392 dup (0)
staVar$_7	dword	3
st$_7	byte	312 dup (0)
a$_16	dword	-1
staArr$_16	dword	30 dup (0)
staData$_16	byte	312 dup (0)

		.code
		externdef	printf:proc
		externdef	N	:dword
		externdef	M	:dword
		public		testUnion
		public		asmMain
		public		loopFib
		public		fillArr
		public		testArr
		public		testStaticVar
		public		contentOfStruct
		public		testDeref
		public		fun
		public		Calfloat

_TEXT	SEGMENT
p3$1@2 = 45
i$1@1 = 49
i$1 = 49
bl6$ = 53
bl5$ = 54
bl4$ = 55
bl3$ = 56
bl2$ = 57
bl$ = 58
cu$ = 59
bv$ = 60
ch$ = 61
st$ = 62
i6$ = 64
f4$ = 68
a$ = 72
i1$ = 76
arr_ptr$ = 80
p2$ = 88
uPtr$ = 96
p$ = 104
u$ = 112
dArr$ = 120
I_arr$ = 136
dVar$ = 176
testUnion PROC 
;      function prologue   
;      Line 64  :void testUnion(void){                   
       sub      rsp    , 248      
;      function body       
;      Line 66  :union uni u={'a',3,1.3};                
       movss    xmm0    , __f1     
       movss    real4 ptr u$[rsp]    , xmm0     
;      Line 68  :int* p=&u.i1;                           
       lea      rax    , dword ptr u$[rsp] 
       mov      qword ptr p$[rsp]    , rax      
;      passing the 2th argument
;      Line 71  :printf("p=%p",p);                       
       mov      rdx    , rax      
;      passing the 1th argument
       lea      rcx    , __str0   
       call     printf            

;      Line 76  :bool bv=p;                              
       mov      rax    , qword ptr p$[rsp] 
       xor      rcx    , rcx      
       cmp      rax    , 0        
       setnz    cl                
;      passing the 2th argument
;      Line 77  :printf("bv=%d",bv);                     
       mov      dl    ,  cl       
;      passing the 1th argument
       lea      rcx    , __str1   
       call     printf            

;      Line 79  :union uni* uPtr=&u;                     
       lea      rax    , qword ptr u$[rsp] 
       mov      qword ptr uPtr$[rsp]    , rax      
;      passing the 2th argument
;      Line 80  :printf("u.i1=%d",uPtr->i1);             
       mov      ecx    , dword ptr[rax] 
       mov      edx    , ecx      
;      passing the 1th argument
       lea      rcx    , __str2   
       call     printf            

;      passing the 2th argument
;      Line 81  :printf("u.f1=%f",uPtr->f1);             
       mov      rax    , qword ptr uPtr$[rsp] 
       movss    xmm0    , real4 ptr[rax] 
       cvtss2sd xmm0    , xmm0     
       movaps   xmm1    , xmm0     
       movq     rdx    , xmm1     
;      passing the 1th argument
       lea      rcx    , __str3   
       call     printf            

;      passing the 2th argument
;      Line 82  :printf("u.c1=%c",uPtr->c1);             
       mov      rax    , qword ptr uPtr$[rsp] 
       mov      cl    ,  byte ptr[rax] 
       mov      dl    ,  cl       
;      passing the 1th argument
       lea      rcx    , __str4   
       call     printf            

;      Line 84  :struct ds dVar={1,1.1,'i',255,-9,'S'};  
       mov      dword ptr dVar$[rsp]    , 1        
       movss    xmm0    , __f2     
       movss    real4 ptr dVar$[rsp+4]    , xmm0     
       mov      byte ptr dVar$[rsp+8]    , 'i'      
       mov      word ptr dVar$[rsp+10]    , 255      
       mov      dword ptr dVar$[rsp+12]    , -9       
       mov      byte ptr dVar$[rsp+16]    , 'S'      
;      Line 85  :for(int i=0;i<3;i++){                   
       xor      eax    , eax      
       mov      dword ptr i$1[rsp]    , eax      

       mov      ecx    , eax      
       lea      rax    , qword ptr dVar$[rsp] 
       jmp      L4                
;      Line 86  :dVar.arr[i]=i*2;                        
L3:    mov      edx    , ecx      
       imul     edx    , 2        
       mov      r8    ,  qword ptr[rax+40] 
       mov      dword ptr[r8+rcx*4]    , edx      
       inc      ecx               
L4:    cmp      ecx    , 3        
       jl       L3                

       mov      dword ptr i$1[rsp]    , ecx      
;      passing the 2th argument
;      Line 88  :printf("dVar.i1=%d",dVar.i1);           
       mov      ecx    , dword ptr[rax] 
       mov      edx    , ecx      
;      passing the 1th argument
       lea      rcx    , __str5   
       call     printf            

;      passing the 2th argument
;      Line 89  :printf("dVar.f1=%f",dVar.f1);           
       movss    xmm0    , real4 ptr dVar$[rsp+4] 
       cvtss2sd xmm0    , xmm0     
       movaps   xmm1    , xmm0     
       movq     rdx    , xmm1     
;      passing the 1th argument
       lea      rcx    , __str6   
       call     printf            

;      passing the 2th argument
;      Line 90  :printf("dVar.i3=%c",dVar.i3);           
       mov      al    ,  byte ptr dVar$[rsp+8] 
       mov      dl    ,  al       
;      passing the 1th argument
       lea      rcx    , __str7   
       call     printf            

;      passing the 2th argument
;      Line 91  :printf("dVar.s1=%d",(int)dVar.s1);      
       mov      ax    ,  word ptr dVar$[rsp+10] 
       movsx    eax    , ax       
       mov      edx    , eax      
;      passing the 1th argument
       lea      rcx    , __str8   
       call     printf            

;      passing the 2th argument
;      Line 92  :printf("dVar.c1=%c",dVar.c1);           
       mov      al    ,  byte ptr dVar$[rsp+16] 
       mov      dl    ,  al       
;      passing the 1th argument
       lea      rcx    , __str9   
       call     printf            

;      passing the 2th argument
;      Line 93  :printf("dVar.arr[2]=%d",dVar.arr[2]);   
       mov      eax    , dword ptr dVar$[rsp+48] 
       mov      edx    , eax      
;      passing the 1th argument
       lea      rcx    , __str10  
       call     printf            

;      Line 96  :union charU cu={'u'};                   
       mov      byte ptr cu$[rsp]    , 'u'      
;      Line 97  :double[2] dArr={-0.2,0.2};              
       cvtss2sd xmm0    , __f3     
       movsd    real8 ptr dArr$[rsp]    , xmm0     
       movsd    xmm1    , __f41    
       movsd    real8 ptr dArr$[rsp+8]    , xmm1     
;      Line 102 :int i6=-0;                              
       xor      eax    , eax      
       mov      dword ptr i6$[rsp]    , eax      
;      Line 104 :bool bl=p,bl2=dArr[1],bl3=(float)dArr[0],bl4=f4;
       mov      rcx    , qword ptr p$[rsp] 
       xor      rdx    , rdx      
       cmp      rcx    , 0        
       setnz    dl                
       mov      cl    ,  1        
       cvtsd2ss xmm0    , xmm0     
       xor      ch    ,  ch       
       comiss   xmm0    , __f5     
       setnz    ch                
       xor      dh    ,  dh       
       mov      byte ptr bl4$[rsp]    , dh       
;      Line 105 :bool bl5=0,bl6;                         
       xor      r8b    , r8b      
       mov      byte ptr bl5$[rsp]    , r8b      
;      passing the 4th argument
;      Line 106 :printf("bl=%d,bl2=%d,bl3=%d",(int)bl,(int)bl2,(int)bl3);
       movsx    eax    , ch       
       mov      r9d    , eax      
;      passing the 3th argument
       movsx    ecx    , cl       
       mov      r8d    , ecx      
;      passing the 2th argument
;      passing the 1th argument
       lea      rcx    , __str11  
       call     printf            

;      passing the 2th argument
;      Line 107 :printf("bl4=%d",(int)bl4);              
       mov      al    ,  byte ptr bl4$[rsp] 
       movsx    eax    , al       
       mov      edx    , eax      
;      passing the 1th argument
       lea      rcx    , __str12  
       call     printf            

;      passing the 2th argument
;      Line 108 :printf("bl5=%d",(int)bl5);              
       mov      al    ,  byte ptr bl5$[rsp] 
       mov      edx    , eax      
;      passing the 1th argument
       lea      rcx    , __str13  
       call     printf            

;      passing the 2th argument
;      Line 109 :printf("bl6=%d",(int)(bl6=i6));         
       mov      eax    , dword ptr i6$[rsp] 
       xor      rcx    , rcx      
       cmp      eax    , 0        
       setnz    cl                
       mov      edx    , ecx      
;      passing the 1th argument
       lea      rcx    , __str14  
       call     printf            

;      Line 112 :int[10] I_arr={0,1,2,-3};               
       mov      dword ptr I_arr$[rsp]    , 0        
       mov      dword ptr I_arr$[rsp+4]    , 1        
       mov      dword ptr I_arr$[rsp+8]    , 2        
       mov      dword ptr I_arr$[rsp+12]    , -3       
;      Line 113 :for(int i=4;i<10;i++){                  
       mov      eax    , 4        
       mov      dword ptr i$1@1[rsp]    , eax      

       movsxd   rcx    , eax      
       lea      rax    , qword ptr I_arr$[rsp] 
       jmp      L9                
;      Line 114 :I_arr[i]=i*(-1);                        
L8:    mov      edx    , ecx      
       imul     edx    , -1       
       mov      dword ptr[rax+rcx*4]    , edx      
       inc      ecx               
L9:    cmp      ecx    , 10       
       jl       L8                

;      Line 116 :int_P p2=I_arr;                         
       mov      qword ptr p2$[rsp]    , rax      
;      passing the 3th argument
;      Line 117 :printf("p2=%p,*p2=%d",p2,*p2);          
       mov      ecx    , dword ptr[rax] 
       mov      r8d    , ecx      
;      passing the 2th argument
       mov      rdx    , rax      
;      passing the 1th argument
       lea      rcx    , __str15  
       call     printf            

;      Line 119 :for(int_P p3=p2;p3<p2+10;p3++ ){        
       mov      rax    , qword ptr p2$[rsp] 
       mov      qword ptr p3$1@2[rsp]    , rax      

       mov      rcx    , rax      
       jmp      L14               
;      Line 120 :printf("p3=%p,*p3=%d",p3,*p3);          
L13:   mov      qword ptr p2$[rsp]    , rax      
;      passing the 3th argument
       mov      eax    , dword ptr[rcx] 
       mov      r8d    , eax      
;      passing the 2th argument
       mov      rdx    , rcx      
;      passing the 1th argument
       lea      rcx    , __str16  
       call     printf            

       mov      rax    , qword ptr p3$1@2[rsp] 
       add      rax    , 4        
       mov      qword ptr p3$1@2[rsp]    , rax      
       mov      rcx    , rax      
       mov      rax    , qword ptr p2$[rsp] 
L14:   mov      rdx    , rax      
       add      rdx    , 40       
       cmp      rcx    , rdx      
       jl       L13               

;      passing the 2th argument
;      Line 124 :printf("N=%d",N);                       
       mov      edx    , N        
;      passing the 1th argument
       lea      rcx    , __str17  
       call     printf            

;      Line 125 :N=M+2;                                  
       mov      eax    , M        
       mov      ecx    , eax      
       add      ecx    , 2        
       mov      N    ,   ecx      
;      passing the 3th argument
;      Line 126 :printf("M=%d,N=%d",M,N);                
       mov      r8d    , ecx      
;      passing the 2th argument
       mov      edx    , eax      
;      passing the 1th argument
       lea      rcx    , __str18  
       call     printf            

;      function epilogue   
       add      rsp    , 248      
       ret                        
testUnion ENDP 
_TEXT	ENDS


_TEXT	SEGMENT
ch$ = 42
bl$ = 43
i$ = 44
c$ = 48
f2$ = 52
f1$ = 56
m$ = 60
n$ = 64
a$ = 68
gPtr$ = 72
baseP$ = 80
p$ = 88
localData$ = 96
asmMain PROC 
;      function prologue   
;      Line 131 :int asmMain(void)                       
       sub      rsp    , 408      
;      function body       
;      Line 146 :st.intField = -2;                       
       mov      dword ptr [st$_7+12]    , -2       
;      Line 147 :localData.intField = global++;          
       mov      eax    , global   
       mov      ecx    , eax      
       inc      ecx               
       mov      global    , ecx      
       mov      dword ptr localData$[rsp+12]    , eax      
;      Line 148 :localData.sp = &global;                 
       lea      rax    , qword ptr global 
       mov      qword ptr localData$[rsp+16]    , rax      
;      Line 156 :testDeref();                            
       call     testDeref          

;      Line 158 :return 0;                               
       xor      eax    , eax      
;      function epilogue   
       add      rsp    , 408      
       ret                        
asmMain ENDP 
_TEXT	ENDS


_TEXT	SEGMENT
n$ = 48
loopFib PROC 
;      Line 162 :int loopFib(int n)                      
       sub      rsp    , 40       
;      store ecx to Shadow Space
       mov      dword ptr n$[rsp]    , ecx      
;      Line 164 :if (!b) {                               
       cmp      b    ,   0        
       jnz      L18               
;      Line 165 :fillArr();                              
       call     fillArr           

;      Line 166 :b = 1;                                  
       mov      b    ,   1        
;      Line 168 :if (1 <= n && n <= e)                   
L18:   cmp      dword ptr n$[rsp]    , 1        
       jl       L20               
       mov      eax    , e        
       cmp      dword ptr n$[rsp]    , eax      
       jg       L20               
;      Line 169 :return fibArr[n];                       
       mov      eax    , dword ptr n$[rsp] 
       mov      eax    , eax      
       lea      rcx    , fibArr   
       add      rcx    , rax      
       mov      edx    , dword ptr[rcx] 
       mov      eax    , edx      
       jmp      L36               
;      passing the 2th argument
;      Line 171 :printf("n不在合法的范围:%d!\n",n);             
L20:   mov      edx    , dword ptr n$[rsp] 
;      passing the 1th argument
       lea      rcx    , __str19  
       call     printf            

;      Line 172 :return -1;                              
       mov      eax    , -1       
L36:   add      rsp    , 40       
       ret                        
loopFib ENDP 
_TEXT	ENDS


_TEXT	SEGMENT
i$ = 4
fillArr PROC 
;      function prologue   
;      Line 175 :void fillArr(void)                      
       sub      rsp    , 8        
;      function body       
;      Line 178 :for (i = 1;i <= e;i++) {                
       mov      eax    , 1        
       mov      dword ptr i$[rsp]    , eax      

       movsxd   rcx    , eax      
       jmp      L25               
;      Line 179 :if (i == 1 || i == 2) {                 
L24:   cmp      ecx    , 1        
       jz       L29               
       cmp      ecx    , 2        
       jnz      L28               
;      Line 180 :fibArr[i] = 1;                          
L29:   lea      r8    ,  fibArr   
       add      r8    ,  rcx      
       mov      dword ptr[r8]    , 1        
       jmp      L27               
;      Line 182 :fibArr[i] = fibArr[i - 1] + fibArr[i - 2];
L28:   mov      r8d    , ecx      
       dec      r8d               
       mov      r8d    , r8d      
       lea      r9    ,  fibArr   
       add      r9    ,  r8       
       mov      r10d    , dword ptr[r9] 
       mov      r8d    , ecx      
       sub      r8d    , 2        
       lea      r9    ,  fibArr   
       add      r9    ,  r8       
       mov      r11d    , dword ptr[r9] 
       add      r10d    , r11d     
       lea      r8    ,  fibArr   
       add      r8    ,  rcx      
       mov      dword ptr[r8]    , r10d     
L27:   inc      ecx               
L25:   cmp      ecx    , edx      
       jle      L24               

;      function epilogue   
       add      rsp    , 8        
       ret                        
fillArr ENDP 
_TEXT	ENDS


_TEXT	SEGMENT
i$ = 44
a$ = 48
testArr PROC 
;      function prologue   
;      Line 187 :void testArr(void){                     
       sub      rsp    , 72       
;      function body       
;      Line 191 :a[1][i]=global++;                       
       mov      eax    , global   
       mov      ecx    , eax      
       inc      ecx               
       mov      global    , ecx      
       mov      dword ptr a$[rsp+12]    , eax      
;      passing the 2th argument
;      Line 192 :printf("a[1][i]=%d",a[1][i]);           
       mov      edx    , eax      
;      passing the 1th argument
       lea      rcx    , __str20  
       call     printf            

;      function epilogue   
       add      rsp    , 72       
       ret                        
testArr ENDP 
_TEXT	ENDS


_TEXT	SEGMENT
two$ = 36
t$ = 40
native$ = 44
p$ = 48
arr$ = 60
testStaticVar PROC 
;      function prologue   
;      Line 195 :void testStaticVar(void){               
       sub      rsp    , 72       
;      function body       
;      Line 207 :arr[0]=-12;arr[1]=10;                   
       mov      dword ptr arr$[rsp]    , -12      
       mov      dword ptr arr$[rsp+4]    , 10       
;      passing the 3th argument
;      Line 208 :printf("before,p=%p,*p=%d",p,*p);       
       lea      rax    , qword ptr arr$[rsp] 
       mov      ecx    , dword ptr[rax] 
       mov      r8d    , ecx      
;      passing the 2th argument
       mov      rdx    , rax      
;      passing the 1th argument
       lea      rcx    , __str21  
       call     printf            

;      Line 223 :staData.e=(global++)+(gloArr[arr[2]]);  
       mov      eax    , global   
       mov      ecx    , eax      
       inc      ecx               
       mov      global    , ecx      
       mov      edx    , dword ptr arr$[rsp+8] 
       mov      edx    , edx      
       lea      r8    ,  gloArr   
       add      r8    ,  rdx      
       mov      r9d    , dword ptr[r8] 
       add      eax    , r9d      
       mov      dword ptr [staData$_16+24]    , eax      
;      Line 224 :staData.bl[0]=global%2==1;              
       xor      edx    , edx      
       mov      r8d    , eax      
       mov      eax    , ecx      
       mov      r9d    , 2        
       div      r9d               
       xor      rax    , rax      
       cmp      edx    , 1        
       setz     al                
       xor      ah    ,  ah       
       cmp      al    ,  0        
       setnz    ah                
       mov      byte ptr [staData$_16]    , ah       
;      Line 225 :staData.sp=&global;                     
       lea      rax    , qword ptr global 
       mov      qword ptr [staData$_16+16]    , rax      
;      Line 226 :staData.send=&gloChar;                  
       lea      rax    , qword ptr gloChar 
       mov      qword ptr [staData$_16+296]    , rax      
;      passing the 2th argument
;      Line 227 :printf("staData.e=%d",staData.e);       
       mov      edx    , r8d      
;      passing the 1th argument
       lea      rcx    , __str22  
       call     printf            

;      passing the 1th argument
;      Line 228 :contentOfStruct(&staData);              
       lea      rcx    , qword ptr staData$_16 
       call     contentOfStruct          

;      function epilogue   
       add      rsp    , 72       
       ret                        
testStaticVar ENDP 
_TEXT	ENDS


_TEXT	SEGMENT
strPtr$ = 48
contentOfStruct PROC 
;      Line 241 :void contentOfStruct(struct data * strPtr){
       sub      rsp    , 40       
;      store rcx to Shadow Space
       mov      qword ptr strPtr$[rsp]    , rcx      
;      passing the 1th argument
;      Line 242 :printf("**********************");       
       lea      rcx    , __str31  
       call     printf            

;      passing the 2th argument
;      Line 243 :printf("bl[0]=%d",strPtr->bl[0]);       
       mov      rax    , qword ptr strPtr$[rsp] 
       mov      cl    ,  byte ptr[rax] 
       mov      dl    ,  cl       
;      passing the 1th argument
       lea      rcx    , __str24  
       call     printf            

;      passing the 2th argument
;      Line 244 :printf("intFiled=%d",strPtr->intField); 
       mov      rax    , qword ptr strPtr$[rsp] 
       mov      ecx    , dword ptr[rax+12] 
       mov      edx    , ecx      
;      passing the 1th argument
       lea      rcx    , __str25  
       call     printf            

;      passing the 2th argument
;      Line 245 :printf("*sp=%d",*strPtr->sp);           
       mov      rax    , qword ptr strPtr$[rsp] 
       mov      rcx    , qword ptr[rax+16] 
       mov      edx    , dword ptr[rcx] 
;      passing the 1th argument
       lea      rcx    , __str26  
       call     printf            

;      passing the 2th argument
;      Line 246 :printf("sp=%p",strPtr->sp);             
       mov      rax    , qword ptr strPtr$[rsp] 
       mov      rcx    , qword ptr[rax+16] 
       mov      rdx    , rcx      
;      passing the 1th argument
       lea      rcx    , __str27  
       call     printf            

;      passing the 2th argument
;      Line 247 :printf("e=%d", strPtr->e);              
       mov      rax    , qword ptr strPtr$[rsp] 
       mov      ecx    , dword ptr[rax+24] 
       mov      edx    , ecx      
;      passing the 1th argument
       lea      rcx    , __str28  
       call     printf            

;      passing the 2th argument
;      Line 248 :printf("rec[0]=%c",(strPtr->rec)[0]);   
       mov      rax    , qword ptr strPtr$[rsp] 
       mov      cl    ,  byte ptr[rax+40] 
       mov      dl    ,  cl       
;      passing the 1th argument
       lea      rcx    , __str29  
       call     printf            

;      passing the 2th argument
;      Line 249 :printf("*send=%c",*strPtr->send);       
       mov      rax    , qword ptr strPtr$[rsp] 
       mov      rcx    , qword ptr[rax+296] 
       mov      al    ,  byte ptr[rcx] 
       mov      dl    ,  al       
;      passing the 1th argument
       lea      rcx    , __str30  
       call     printf            

;      passing the 1th argument
;      Line 250 :printf("**********************");       
       lea      rcx    , __str31  
       call     printf            

       add      rsp    , 40       
       ret                        
contentOfStruct ENDP 
_TEXT	ENDS


_TEXT	SEGMENT
a$ = 52
dataPtr$ = 56
p2$ = 64
p1$ = 72
arr2$ = 80
arr1$ = 104
ptrArray$ = 128
localData$ = 160
testDeref PROC 
;      function prologue   
;      Line 255 :void testDeref(void){                   
       sub      rsp    , 472      
       mov      qword ptr[rsp+36]    , rsi      
       mov      qword ptr[rsp+44]    , rdi      
;      function body       
;      Line 256 :int a=5;                                
       mov      eax    , 5        
       mov      dword ptr a$[rsp]    , eax      
;      Line 259 :localData.intField=1;                   
       mov      dword ptr localData$[rsp+12]    , 1        
;      Line 260 :localData.sp=&a;                        
       lea      rax    , dword ptr a$[rsp] 
       mov      qword ptr localData$[rsp+16]    , rax      
;      Line 261 :localData.e=*localData.sp+1;            
       mov      dword ptr localData$[rsp+24]    , 6        
;      Line 262 :localData.rec="Hello world!";           
       lea      rsi    , __str32  
       lea      rdi    , qword ptr localData$[rsp+40] 
       mov      rcx    , 13       
       rep movsb                   
;      Line 263 :localData.send="Hello my Dummb Compiler!";
       lea      rax    , __str33  
       mov      qword ptr localData$[rsp+296]    , rax      
;      Line 269 :arr1[0][2]=-6;                          
       mov      dword ptr arr1$[rsp+8]    , -6       
;      Line 275 :*dataPtr->sp=*&dataPtr->rec[2];         
       lea      rax    , qword ptr localData$[rsp] 
       lea      rax    , byte ptr[rax+42] 
       mov      cl    ,  byte ptr[rax] 
       movsx    ecx    , cl       
       lea      rax    , qword ptr localData$[rsp] 
       mov      rdx    , qword ptr[rax+16] 
       mov      dword ptr[rdx]    , ecx      
;      Line 281 :ptrArray[2]=&arr1;                      
       lea      rax    , qword ptr arr1$[rsp] 
       mov      qword ptr ptrArray$[rsp+16]    , rax      
;      Line 283 :(*ptrArray[2])[0][2]=-2;                
       mov      dword ptr[rax+8]    , -2       
;      Line 285 :ptrArray[2][0][0][2]=-3;                
       mov      rax    , qword ptr ptrArray$[rsp+16] 
       mov      dword ptr[rax+8]    , -3       
;      passing the 2th argument
;      Line 289 :printf("( * ptrArray[1] ) [0][2] = %d", (*ptrArray[2])[0][2]);
       mov      ecx    , dword ptr[rax+8] 
       mov      edx    , ecx      
;      passing the 1th argument
       lea      rcx    , __str34  
       call     printf            

;      function epilogue   
       mov      rdi    , qword ptr[rsp+44] 
       mov      rsi    , qword ptr[rsp+36] 
       add      rsp    , 472      
       ret                        
testDeref ENDP 
_TEXT	ENDS


_TEXT	SEGMENT
param2$ = 264
param1$ = 256
argv$ = 248
argc$ = 240
bl$ = 48
ch$ = 49
i$ = 52
f5$ = 56
f4$ = 60
f3$ = 64
f2$ = 68
f1$ = 72
h$ = 76
g$ = 80
e$ = 84
d$ = 88
c$ = 92
b$ = 96
a$ = 100
arrayP$ = 112
stPtr$ = 120
q$ = 128
p$ = 136
ptrArray$ = 152
arr$ = 176
promts$ = 202
element$ = 216
x$ = 233
matricxPointerOfMaticex$ = 233
str$ = 233
st$ = 233
fun PROC 
;      function prologue   
;      Line 297 :int fun(int argc, char[10][] argv, char param1, bool param2, int param3)
       sub      rsp    , 232      
;      store r9b to Shadow Space
       mov      byte ptr param2$[rsp]    , r9b      
;      store r8b to Shadow Space
       mov      byte ptr param1$[rsp]    , r8b      
;      store rdx to Shadow Space
       mov      qword ptr argv$[rsp]    , rdx      
;      store ecx to Shadow Space
       mov      dword ptr argc$[rsp]    , ecx      
;      function body       
;      Line 300 :int a = -1, b, c, d, e, g, h;           
       mov      eax    , -1       
       mov      dword ptr a$[rsp]    , eax      
;      Line 303 :int* p = &a, q;                         
       lea      rcx    , dword ptr a$[rsp] 
       mov      qword ptr p$[rsp]    , rcx      
;      Line 306 :float f1 = 1.0, f2 = 3.14, f3 = -2.3, f4, f5;
       movss    xmm0    , __f8     
       movss    real4 ptr f2$[rsp]    , xmm0     
;      passing the 1th argument
;      Line 318 :printf(str);                            
       lea      rcx    , __str35  
       call     printf            

;      Line 321 :*p = 5;                                 
       mov      rax    , qword ptr p$[rsp] 
       mov      dword ptr[rax]    , 5        
;      Line 323 :arr[2][1] = *p;                         
       mov      dword ptr arr$[rsp+20]    , 5        
;      Line 326 :ptrArray[2] = p + 1;                    
       add      rax    , 4        
       mov      qword ptr ptrArray$[rsp+16]    , rax      
;      Line 328 :matricxPointerOfMaticex[1][2]=&element; 
       lea      rax    , qword ptr element$[rsp] 
       mov      qword ptr matricxPointerOfMaticex$[rsp+72]    , rax      
;      passing the 5th argument
;      Line 333 :i = fun(a, promts, (char) b, (int) f2, (int) x[2])
       movss    xmm0    , real4 ptr x$[rsp+8] 
       cvttss2si eax    , xmm0     
       mov      dword ptr[rsp+32]    , eax      
;      passing the 4th argument
       movss    xmm0    , real4 ptr f2$[rsp] 
       cvttss2si eax    , xmm0     
       mov      r9d    , eax      
;      passing the 3th argument
       mov      eax    , dword ptr b$[rsp] 
       mov      r8b    , al       
;      passing the 2th argument
       lea      rdx    , qword ptr promts$[rsp] 
;      passing the 1th argument
       mov      ecx    , dword ptr a$[rsp] 
       call     fun               

;      passing the 3th argument
;      Line 334 :+ (int) (Calfloat(2.14, f2, &f5));      
       lea      r8    ,  real4 ptr f5$[rsp] 
;      passing the 2th argument
       movss    xmm1    , real4 ptr f2$[rsp] 
;      passing the 1th argument
       movss    xmm0    , __f10    
       call     Calfloat          

;      Line 335 :return 0;                               
       xor      eax    , eax      
;      function epilogue   
       add      rsp    , 232      
       ret                        
fun ENDP 
_TEXT	ENDS


_TEXT	SEGMENT
c$ = 80
b$ = 72
a$ = 64
t259$ = 44
ret$ = 48
fArr$ = 57
Calfloat PROC 
;      function prologue   
;      Line 339 :float Calfloat(float a, float b, float* c)
       sub      rsp    , 56       
;      store xmm1 to Shadow Space
       movss    real4 ptr b$[rsp]    , xmm1     
;      store xmm0 to Shadow Space
       movss    real4 ptr a$[rsp]    , xmm0     
;      store r8 to Shadow Space
       mov      qword ptr c$[rsp]    , r8       
;      function body       
;      Line 341 :float ret = a * b + *c;                 
       movaps   xmm2    , xmm0     
       mulss    xmm2    , xmm1     
       movss    xmm3    , real4 ptr[r8] 
       addss    xmm2    , xmm3     
       movss    real4 ptr ret$[rsp]    , xmm2     
;      Line 343 :*c = a - b + Calfloat(b, *&(ret) / 1.9, fArr);
       subss    xmm0    , xmm1     
       movss    real4 ptr t259$[rsp]    , xmm0     
;      passing the 3th argument
       lea      r8    ,  qword ptr fArr$[rsp] 
;      passing the 2th argument
       divss    xmm2    , __f11    
       movaps   xmm0    , xmm1     
       movaps   xmm1    , xmm2     
;      passing the 1th argument
       call     Calfloat          

       movss    xmm1    , real4 ptr t259$[rsp] 
       addss    xmm1    , xmm0     
       mov      rax    , qword ptr c$[rsp] 
       movss    real4 ptr[rax]    , xmm1     
;      passing the 3th argument
;      Line 346 :Calfloat(b, Calfloat(b, c[1], &ret + 1), &fArr[4]);
       lea      rax    , real4 ptr ret$[rsp] 
       add      rax    , 4        
       mov      r8    ,  rax      
;      passing the 2th argument
       mov      rax    , qword ptr c$[rsp] 
       movss    xmm0    , real4 ptr[rax+4] 
       movaps   xmm1    , xmm0     
;      passing the 1th argument
       movss    xmm0    , real4 ptr b$[rsp] 
       call     Calfloat          

;      passing the 3th argument
       lea      r8    ,  real4 ptr fArr$[rsp+16] 
;      passing the 2th argument
       movaps   xmm1    , xmm0     
;      passing the 1th argument
       movss    xmm0    , real4 ptr b$[rsp] 
       call     Calfloat          

;      Line 347 :return ret + *c;                        
       mov      rax    , qword ptr c$[rsp] 
       movss    xmm0    , real4 ptr[rax] 
       movss    xmm1    , real4 ptr ret$[rsp] 
       addss    xmm1    , xmm0     
       movaps   xmm0    , xmm1     
;      function epilogue   
       add      rsp    , 56       
       ret                        
Calfloat ENDP 
_TEXT	ENDS

END
