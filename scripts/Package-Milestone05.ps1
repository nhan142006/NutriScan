param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    $outputDirectory = Join-Path $projectRoot 'submission'
    New-Item -ItemType Directory -Force $outputDirectory | Out-Null
    if (!$SkipBuild) {
        & .\gradlew.bat :app:assembleRelease :app:bundleRelease --max-workers=2 --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'Release build failed.' }
        & .\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:lintRelease --max-workers=2 --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'Verification failed.' }
    }
    $apk = Join-Path $projectRoot 'app/build/outputs/apk/release/app-release.apk'
    $bundle = Join-Path $projectRoot 'app/build/outputs/bundle/release/app-release.aab'
    if (!(Test-Path $apk) -or !(Test-Path $bundle)) { throw 'Signed APK/AAB output is missing.' }
    $sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
    $tools = Get-ChildItem (Join-Path $sdk 'build-tools') -Directory |
        Where-Object { $_.Name -match '^\d+\.\d+\.\d+$' } |
        Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1
    $signature = & (Join-Path $tools.FullName 'apksigner.bat') verify --verbose --print-certs $apk
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
    Copy-Item -LiteralPath $apk -Destination (Join-Path $outputDirectory 'Group04_NutriScan_Milestone05_Release.apk')
    Copy-Item -LiteralPath $bundle -Destination (Join-Path $outputDirectory 'Group04_NutriScan_Milestone05_Release.aab')
    Copy-Item -LiteralPath 'AI_USAGE.md' -Destination (Join-Path $outputDirectory 'AI_USAGE.md')
    $publicCertificate = Join-Path $projectRoot '.signing/release-certificate.pem'
    if (Test-Path $publicCertificate) { Copy-Item -LiteralPath $publicCertificate -Destination $outputDirectory }
    [IO.File]::WriteAllLines((Join-Path $outputDirectory 'APK_SIGNATURE.txt'), $signature, [Text.UTF8Encoding]::new($false))
    Write-Output "Signed artifacts are in $outputDirectory. Private keystore/passwords are excluded."
} finally { Pop-Location }
