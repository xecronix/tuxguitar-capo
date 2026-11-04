param(
    [string]$Upstream = (Join-Path $PSScriptRoot '..\work\upstream\tuxguitar-533efa74e6a56bdae28bb776358305607c79cbff'),
    [string]$Repository = (Join-Path $PSScriptRoot '..\.tools\m2'),
    [switch]$RebuildHost
)
$ErrorActionPreference = 'Stop'
$tuxToolScript = Join-Path $PSScriptRoot '..\.tools\Use-Toolchain.ps1'
if (Test-Path -LiteralPath $tuxToolScript) { . $tuxToolScript }
Get-Command mvn -ErrorAction Stop | Out-Null
$Repository = [System.IO.Path]::GetFullPath($Repository)
$tuxRepoArg = "-Dmaven.repo.local=$Repository"
$tuxHostArtifact = Join-Path $Repository 'app\tuxguitar\tuxguitar\9.99-SNAPSHOT\tuxguitar-9.99-SNAPSHOT.jar'
if ($RebuildHost -or !(Test-Path -LiteralPath $tuxHostArtifact)) {
    if (!(Test-Path -LiteralPath (Join-Path $Upstream 'desktop\pom.xml'))) { throw 'Provide -Upstream pointing to the TuxGuitar 2.0.1 source.' }
    & mvn $tuxRepoArg -B -ntp -f (Join-Path $Upstream 'desktop\pom.xml') install
    if ($LASTEXITCODE -ne 0) { throw 'TuxGuitar parent build failed.' }
    & mvn $tuxRepoArg -B -ntp -f (Join-Path $Upstream 'desktop\build-scripts\tuxguitar-windows-swt-x86_64\pom.xml') -pl 'app.tuxguitar:tuxguitar' -am install
    if ($LASTEXITCODE -ne 0) { throw 'TuxGuitar API build failed.' }
}
& mvn $tuxRepoArg -B -ntp -f (Join-Path $PSScriptRoot 'pom.xml') verify
if ($LASTEXITCODE -ne 0) { throw 'Capo plugin build or tests failed.' }
