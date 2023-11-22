
		.const
__str0	byte	'str=%s', 0
__str1	byte	'a[1][i]=%d', 0
__str2	byte	'before,p=%p,*p=%d', 0
__str3	byte	'Now (*p++=a++)=%d', 0
__str4	byte	'after,*(p-1)=%d', 0
__f0	real4	1.2
__f01	real8	1.2
NULL	qword	0

		.data
global	dword	8
gloDouble	real8	1.2
a$_3	dword	-1
staArr$_3	dword	30 dup (0)
staData$_3	byte	312 dup (0)

		.code
		externdef	gets:proc
		externdef	printf:proc
		public		testLiteral
		public		asmMain
		public		testArr
		public		testStaticVar
testLiteral PROC 
       ret                        
testLiteral ENDP 

_TEXT	SEGMENT
a$ = 44
str$ = 48
asmMain PROC 
;      function prologue   
;      Line 195 :void asmMain(void){                     
       sub      rsp    , 56       
;      function body       
;      passing the 1th argument
;      Line 200 :gets(str);                              
       mov      rcx    , qword ptr str$[rsp] 
       call     gets              

;      passing the 2th argument
;      Line 201 :printf("str=%s",str);                   
       mov      rdx    , qword ptr str$[rsp] 
;      passing the 1th argument
       lea      rcx    , __str0   
       call     printf            

;      function epilogue   
       add      rsp    , 56       
       ret                        
asmMain ENDP 
_TEXT	ENDS


_TEXT	SEGMENT
i$ = 44
a$ = 48
testArr PROC 
;      function prologue   
;      Line 207 :void testArr(void)                      
       sub      rsp    , 72       
;      function body       
;      Line 212 :a[1][i] = global++;                     
       mov      eax    , global   
       mov      ecx    , eax      
       inc      ecx               
       mov      global    , ecx      
       mov      dword ptr a$[rsp+12]    , eax      
;      passing the 2th argument
;      Line 213 :printf("a[1][i]=%d", a[1][i]);          
       mov      edx    , eax      
;      passing the 1th argument
       lea      rcx    , __str1   
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
;      Line 216 :void testStaticVar(void)                
       sub      rsp    , 72       
;      function body       
;      Line 223 :int* p = arr;                           
       lea      rax    , qword ptr arr$[rsp] 
       mov      qword ptr p$[rsp]    , rax      
;      passing the 3th argument
;      Line 230 :printf("before,p=%p,*p=%d", p, *p);     
       mov      ecx    , dword ptr[rax] 
       mov      r8d    , ecx      
;      passing the 2th argument
       mov      rdx    , rax      
;      passing the 1th argument
       lea      rcx    , __str2   
       call     printf            

;      passing the 2th argument
;      Line 231 :printf("Now (*p++=a++)=%d",*p++=a++);   
       mov      eax    , a$_3     
       mov      ecx    , eax      
       inc      ecx               
       mov      a$_3    , ecx      
       mov      rcx    , qword ptr p$[rsp] 
       mov      rdx    , rcx      
       add      rdx    , 4        
       mov      qword ptr p$[rsp]    , rdx      
       mov      dword ptr[rcx]    , eax      
       mov      rcx    , rdx      
       mov      edx    , eax      
;      passing the 1th argument
       mov      rax    , rcx      
       lea      rcx    , __str3   
       call     printf            

;      passing the 2th argument
;      Line 232 :printf("after,*(p-1)=%d",*(p-1));       
       mov      rax    , qword ptr p$[rsp] 
       sub      rax    , 4        
       mov      ecx    , dword ptr[rax] 
       mov      edx    , ecx      
;      passing the 1th argument
       lea      rcx    , __str4   
       call     printf            

;      function epilogue   
       add      rsp    , 72       
       ret                        
testStaticVar ENDP 
_TEXT	ENDS

END
