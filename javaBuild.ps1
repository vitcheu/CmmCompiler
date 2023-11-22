param([String]$source)
java -classpath out/production/CmmCompiler compile.Compiler $source

# if($?){
#     ./Production/Code/build.ps1 $source
# }