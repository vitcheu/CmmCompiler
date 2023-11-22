param([string]$source)

function cl64 {
    param (
       [Parameter()]
       [string]$source="source"
    )

    $a= ml64 /nologo /c /Zi /Cp "$source.asm"

    $b= cl /nologo /O2 /Zi /utf-8 /EHa  /Fe"$source.exe" driver.cpp "$source.obj"

    outputStringArray($a)
    outputStringArray($b)
}

function outputStringArray([String[]] $strings){
    $strings.ForEach({
        Write-Output "$_"
    })
}

$path="D:/Code/CmmCompiler/Production/Code/";
$curPath=Get-Location
if($curPath -ne $path ){
    Set-Location $path
}

cl64 -source $source