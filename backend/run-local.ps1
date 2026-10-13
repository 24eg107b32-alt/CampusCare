$ErrorActionPreference = 'Stop'

$dbUsername = Read-Host 'MySQL username [root]'
if ([string]::IsNullOrWhiteSpace($dbUsername)) {
    $dbUsername = 'root'
}
$dbPassword = Read-Host 'MySQL password' -AsSecureString
$passwordHandle = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($dbPassword)

try {
    $env:DB_USERNAME = $dbUsername
    $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordHandle)
    Push-Location $PSScriptRoot
    try {
        mvn spring-boot:run
        $mavenExitCode = $LASTEXITCODE
    }
    finally {
        Pop-Location
        Remove-Item Env:DB_USERNAME -ErrorAction SilentlyContinue
        Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
    }
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordHandle)
}

if ($null -ne $mavenExitCode) {
    exit $mavenExitCode
}
