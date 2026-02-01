param(
    [switch]$Build,
    [switch]$NoDocker,
    [string]$Profiles = "dev,local",
    [int]$XmsMb = 128,
    [int]$XmxMb = 512
)

$services = @(
    "discovery",
    "gateway",
    "video",
    "profile",
    "storage",
    "interactions",
    "admin",
    "search",
    "transcoding"
)

if ($Build) {
    & .\gradlew bootJar
}

if (-not $NoDocker) {
    & docker compose up -d postgres redis kafka minio ollama ollama-pull
}

$env:SPRING_PROFILES_ACTIVE = $Profiles
$env:JAVA_TOOL_OPTIONS = "-Xms${XmsMb}m -Xmx${XmxMb}m"

foreach ($svc in $services) {
    $jar = Get-ChildItem "$svc\build\libs\*.jar" |
        Where-Object { $_.Name -notmatch "plain" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if (-not $jar) {
        Write-Warning "Jar not found for $svc. Run with -Build first."
        continue
    }

    Write-Host "Starting $svc -> $($jar.Name)"
    Start-Process java -ArgumentList "-jar", $jar.FullName -WorkingDirectory $PWD
}
