param([string[]]$Suites = @('StarterTest', 'InventoryTest', 'MiddlewareTest', 'BundleTest', 'RmiIntegrationTest', 'RmiFailureTest', 'RmiConcurrencyTest', 'ProcessRmiTest'))
$ErrorActionPreference = 'Stop'
$pa1Root = Split-Path $PSScriptRoot -Parent
Push-Location $pa1Root
try {
    New-Item -ItemType Directory -Force build/tests | Out-Null
    $pa1Sources = Get-ChildItem Template,tests/Tests -Recurse -Filter *.java | ForEach-Object { $_.FullName }
    & javac -encoding UTF-8 -d build/tests $pa1Sources
    if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
    foreach ($pa1Suite in $Suites) {
        & java -cp build/tests "Tests.$pa1Suite"
        if ($LASTEXITCODE -ne 0) { throw "$pa1Suite failed" }
    }
} finally { Pop-Location }
