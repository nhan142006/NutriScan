param([string]$Alias = 'nutriscan-release')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$signingDirectory = Join-Path $projectRoot '.signing'
$keyFile = Join-Path $signingDirectory 'nutriscan-release.jks'
$configurationFile = Join-Path $signingDirectory 'release.properties'
if ((Test-Path -LiteralPath $keyFile) -or (Test-Path -LiteralPath $configurationFile)) {
    throw 'Signing material already exists. Reuse it; this script never overwrites an existing key.'
}
$keytoolPath = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/keytool.exe' } else { 'keytool' }
New-Item -ItemType Directory -Path $signingDirectory -Force | Out-Null
$random = New-Object byte[] 32
$generator = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $generator.GetBytes($random) } finally { $generator.Dispose() }
$privatePassword = [Convert]::ToBase64String($random)
$env:NUTRISCAN_GENERATED_KEY_PASSWORD = $privatePassword
try {
    & $keytoolPath -genkeypair -keystore $keyFile -storetype JKS -alias $Alias -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -validity 10000 -dname 'CN=NutriScan Group 04, OU=Mobile Application Programming, O=Group 04, C=VN' -storepass:env NUTRISCAN_GENERATED_KEY_PASSWORD -keypass:env NUTRISCAN_GENERATED_KEY_PASSWORD
    if ($LASTEXITCODE -ne 0) { throw 'Keystore generation failed.' }
    $properties = @(
        'NUTRISCAN_RELEASE_STORE_FILE=.signing/nutriscan-release.jks'
        "NUTRISCAN_RELEASE_STORE_PASSWORD=$privatePassword"
        "NUTRISCAN_RELEASE_KEY_ALIAS=$Alias"
        "NUTRISCAN_RELEASE_KEY_PASSWORD=$privatePassword"
    )
    [IO.File]::WriteAllLines($configurationFile, $properties, [Text.UTF8Encoding]::new($false))
    & $keytoolPath -exportcert -rfc -keystore $keyFile -alias $Alias -storepass:env NUTRISCAN_GENERATED_KEY_PASSWORD -file (Join-Path $signingDirectory 'release-certificate.pem')
    if ($LASTEXITCODE -ne 0) { throw 'Certificate export failed.' }
} finally {
    Remove-Item Env:NUTRISCAN_GENERATED_KEY_PASSWORD -ErrorAction SilentlyContinue
    $privatePassword = $null
    [Array]::Clear($random, 0, $random.Length)
}
Write-Output "Created private signing material in $signingDirectory. Back up this folder securely; do not commit or submit it."
