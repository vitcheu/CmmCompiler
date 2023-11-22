Get-ChildItem -Filter "*.hb" -File | ForEach-Object {
    $newName = $_.FullName -replace "hb", "h"
    if(!(Test-Path $newName)){
        Rename-Item $_.FullName $newName
    }else{
        echo "\n$newName already exists!"
        Remove-Item $_
    }
}