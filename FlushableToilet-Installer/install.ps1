# Flushable Toilet installer
# Installs Fabric Loader for Minecraft 26.3, Fabric API, and the Flushable Toilet mod.
# Everything needed is in the "files" folder next to this script; no Java or internet required
# (the Minecraft Launcher downloads Minecraft and the Fabric libraries on first launch).
#
# Run:  powershell -ExecutionPolicy Bypass -File .\install.ps1
#  or:  double-click install.bat

param(
	[string]$MinecraftDir = (Join-Path $env:APPDATA '.minecraft')
)

$ErrorActionPreference = 'Stop'

$LoaderVersionId = 'fabric-loader-0.19.5-26.3'
$ProfileKey      = 'fabric-loader-26.3'
$ProfileName     = 'Fabric 26.3 (Flushable Toilet)'
$FilesDir        = Join-Path $PSScriptRoot 'files'

function Write-Step($msg) { Write-Host "==> $msg" -ForegroundColor Cyan }

try {
	Write-Host ''
	Write-Host '  Flushable Toilet installer' -ForegroundColor Green
	Write-Host "  Minecraft folder: $MinecraftDir"
	Write-Host ''

	if (-not (Test-Path (Join-Path $FilesDir 'mods'))) {
		throw "Can't find the 'files' folder next to this script. Extract the whole zip first, then run the script from the extracted folder."
	}

	if (Get-Process -Name 'MinecraftLauncher', 'Minecraft' -ErrorAction SilentlyContinue) {
		Write-Host 'The Minecraft Launcher is open. Close it completely, then press Enter to continue.' -ForegroundColor Yellow
		Read-Host | Out-Null
	}

	if (-not (Test-Path $MinecraftDir)) {
		New-Item -ItemType Directory -Force -Path $MinecraftDir | Out-Null
	}

	# 1. Fabric Loader version profile (same thing the official Fabric installer creates)
	Write-Step 'Installing Fabric Loader for Minecraft 26.3'
	$versionDir = Join-Path $MinecraftDir "versions\$LoaderVersionId"
	New-Item -ItemType Directory -Force -Path $versionDir | Out-Null
	Copy-Item (Join-Path $FilesDir "$LoaderVersionId.json") (Join-Path $versionDir "$LoaderVersionId.json") -Force
	$dummyJar = Join-Path $versionDir "$LoaderVersionId.jar"
	if (-not (Test-Path $dummyJar)) { New-Item -ItemType File -Path $dummyJar | Out-Null }

	# 2. Launcher profile so "Fabric 26.3" shows up in the Play menu
	Write-Step 'Adding a launcher profile'
	$profilesPath = Join-Path $MinecraftDir 'launcher_profiles.json'
	if (Test-Path $profilesPath) {
		Copy-Item $profilesPath "$profilesPath.bak" -Force
		$launcherProfiles = Get-Content $profilesPath -Raw -Encoding UTF8 | ConvertFrom-Json
	} else {
		$launcherProfiles = [pscustomobject]@{ profiles = [pscustomobject]@{} }
	}
	if (-not $launcherProfiles.profiles) {
		$launcherProfiles | Add-Member -NotePropertyName profiles -NotePropertyValue ([pscustomobject]@{}) -Force
	}
	$now = (Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ss.fffZ')
	$fabricProfile = [pscustomobject]@{
		name          = $ProfileName
		type          = 'custom'
		created       = $now
		lastUsed      = $now
		icon          = 'Furnace'
		lastVersionId = $LoaderVersionId
	}
	$launcherProfiles.profiles | Add-Member -NotePropertyName $ProfileKey -NotePropertyValue $fabricProfile -Force
	$json = $launcherProfiles | ConvertTo-Json -Depth 20
	[System.IO.File]::WriteAllText($profilesPath, $json, (New-Object System.Text.UTF8Encoding $false))

	# 3. Mods. Older copies of these two mods are moved aside so there are no duplicates.
	Write-Step 'Installing mods'
	$modsDir = Join-Path $MinecraftDir 'mods'
	New-Item -ItemType Directory -Force -Path $modsDir | Out-Null
	$old = Get-ChildItem $modsDir -File | Where-Object { $_.Name -like 'fabric-api-*.jar' -or $_.Name -like 'flushabletoilet-*.jar' }
	if ($old) {
		$backupDir = Join-Path $MinecraftDir ('mods-backup-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
		New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
		$old | Move-Item -Destination $backupDir
		Write-Host "    Moved old versions to $backupDir"
	}
	Get-ChildItem (Join-Path $FilesDir 'mods') -Filter '*.jar' | ForEach-Object {
		Copy-Item $_.FullName $modsDir -Force
		Write-Host "    $($_.Name)"
	}

	Write-Host ''
	Write-Host 'Done!' -ForegroundColor Green
	Write-Host "Open the Minecraft Launcher, pick '$ProfileName' next to the Play button, and press Play."
	Write-Host 'The first launch downloads Minecraft 26.3 and Fabric, so it takes a minute.'
} catch {
	Write-Host ''
	Write-Host "Install failed: $($_.Exception.Message)" -ForegroundColor Red
	exit 1
}
