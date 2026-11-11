param(
    [switch]$Build
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$frontDir = Join-Path $projectRoot 'front'
$distDir = Join-Path $frontDir 'dist'
$staticDir = Join-Path $PSScriptRoot 'src/main/resources/static'

if ($Build) {
    Push-Location $frontDir
    try {
        npm run build
        if ($LASTEXITCODE -ne 0) {
            throw '前端构建失败，未复制 dist。'
        }
    }
    finally {
        Pop-Location
    }
}

if (-not (Test-Path -LiteralPath (Join-Path $distDir 'index.html'))) {
    throw "未找到 $distDir/index.html，请先构建前端。"
}

New-Item -ItemType Directory -Path $staticDir -Force | Out-Null
Get-ChildItem -LiteralPath $distDir -Force | ForEach-Object {
    Copy-Item -LiteralPath $_.FullName -Destination $staticDir -Recurse -Force
}

Write-Host "前端 dist 已复制到 $staticDir"
