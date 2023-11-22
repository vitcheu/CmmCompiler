$path=".\resource\CmmCode";

$children=Get-ChildItem -Path $path;
Write-Output "children=$children";
$children.forEach({
    $name=$_.name
    Write-Output  "Processing $name ..."
    if($name -ne "q.c"){
        $name=$name.replace(".c","");
        $result=./javaBuild.ps1 $name
        Write-Output $result

    }

    # Write-Output 'Done...' 
})