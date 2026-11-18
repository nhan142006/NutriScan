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
    $bundleSignature = & (Join-Path $env:JAVA_HOME 'bin/jarsigner.exe') -verify -certs $bundle 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'AAB signature verification failed.' }
    $bundleCertificate = & (Join-Path $env:JAVA_HOME 'bin/keytool.exe') -printcert -jarfile $bundle 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'AAB signing certificate is missing.' }
    $apkFingerprint = [regex]::Match(($signature -join "`n"), 'certificate SHA-256 digest: (\w+)').Groups[1].Value
    $bundleFingerprint = [regex]::Match(($bundleCertificate -join "`n"), 'SHA256: ([A-Fa-f0-9:]+)').Groups[1].Value.Replace(':', '')
    if (!$apkFingerprint -or $apkFingerprint -ine $bundleFingerprint) { throw 'APK and AAB use different signing certificates.' }
    Copy-Item -LiteralPath $apk -Destination (Join-Path $outputDirectory 'Group04_NutriScan_Milestone05_Release.apk')
    Copy-Item -LiteralPath $bundle -Destination (Join-Path $outputDirectory 'Group04_NutriScan_Milestone05_Release.aab')
    Copy-Item -LiteralPath 'AI_USAGE.md' -Destination (Join-Path $outputDirectory 'AI_USAGE.md')
    $publicCertificate = Join-Path $projectRoot '.signing/release-certificate.pem'
    if (Test-Path $publicCertificate) { Copy-Item -LiteralPath $publicCertificate -Destination $outputDirectory }
    [IO.File]::WriteAllLines((Join-Path $outputDirectory 'APK_SIGNATURE.txt'), $signature, [Text.UTF8Encoding]::new($false))
    [IO.File]::WriteAllLines((Join-Path $outputDirectory 'AAB_SIGNATURE.txt'), @($bundleSignature; $bundleCertificate), [Text.UTF8Encoding]::new($false))
    Write-Output "Signed artifacts are in $outputDirectory. Private keystore/passwords are excluded."
} finally { Pop-Location }
