# Builds Tessel4J with the JDK tools only (no Maven/Gradle needed).
#
#   ./build.ps1            -> dist/tessel-laf-<v>.jar, tessel-laf-<v>-sources.jar, tessel-gallery-<v>.jar
#   ./build.ps1 -Run       -> builds, then starts the gallery
#
# Requires JDK 17 or newer on the PATH. The jars target Java 17.
param([switch]$Run)

$ErrorActionPreference = 'Stop'
$version = '1.1.0'
$root = $PSScriptRoot
$build = Join-Path $root 'build'
$dist = Join-Path $root 'dist'

# Use javac/jar/java from one JDK: JAVA_HOME if set, otherwise the home of the 'java' on the PATH.
$jdk = $env:JAVA_HOME
if (-not $jdk) {
    $props = & java -XshowSettings:properties -version 2>&1 | Out-String
    if ($props -match 'java\.home = (.+)') { $jdk = $Matches[1].Trim() }
}
if (-not $jdk -or -not (Test-Path (Join-Path $jdk 'bin/javac*'))) { throw "No JDK found. Set JAVA_HOME to a JDK 17+." }
$tools = @{
    javac = Join-Path $jdk 'bin/javac'
    jar = Join-Path $jdk 'bin/jar'
    java = Join-Path $jdk 'bin/java'
}
Write-Host "Using JDK: $jdk"

function Invoke-Tool {
    $exe = $tools[$args[0]]
    & $exe $args[1..($args.Count - 1)]
    if ($LASTEXITCODE -ne 0) { throw "$($args[0]) failed with exit code $LASTEXITCODE" }
}

function Get-Sources($dir) {
    Get-ChildItem -Recurse -Path $dir -Filter *.java | ForEach-Object { '"' + ($_.FullName -replace '\\', '/') + '"' }
}

# Only this version's jars are replaced, so the jars of earlier releases stay in dist/.
if (Test-Path $build) { Get-ChildItem $build -Force | Remove-Item -Recurse -Force }
else { New-Item -ItemType Directory $build | Out-Null }
if (-not (Test-Path $dist)) { New-Item -ItemType Directory $dist | Out-Null }
Get-ChildItem $dist -Filter "*-$version*.jar" | Remove-Item -Force
$libClasses = Join-Path $build 'lib'
$galleryClasses = Join-Path $build 'gallery'
New-Item -ItemType Directory $libClasses, $galleryClasses | Out-Null

# JNA gives TesselFrame native Windows chrome (shadow, rounded corners, Snap Layouts); see lib/README.md.
$libJars = Get-ChildItem (Join-Path $root 'lib') -Filter 'jna-*.jar' -ErrorAction SilentlyContinue
$jnaJar = $libJars | Where-Object { $_.Name -match '^jna-[0-9]' } | Select-Object -First 1
$jnaPlatformJar = $libJars | Where-Object { $_.Name -match '^jna-platform-' } | Select-Object -First 1
if (-not $jnaJar -or -not $jnaPlatformJar) { throw "lib/jna-*.jar and lib/jna-platform-*.jar are required (see README.md)." }
$jnaCp = "$($jnaJar.FullName);$($jnaPlatformJar.FullName)"

# ---------------------------------------------------------------- library
Write-Host "Compiling tessel-laf..."
$libSrc = Join-Path $root 'tessel-laf/src/main/java'
Get-Sources $libSrc | Set-Content (Join-Path $build 'lib-sources.txt') -Encoding ascii
Invoke-Tool javac --release 17 -encoding UTF-8 '-Xlint:all,-serial,-this-escape' -cp $jnaCp -d $libClasses "@$(Join-Path $build 'lib-sources.txt')"
# Bundle JNA's classes (and its native dispatch library) so consumers still only need this one jar.
Push-Location $libClasses
try {
    Invoke-Tool jar -xf $jnaJar.FullName
    Invoke-Tool jar -xf $jnaPlatformJar.FullName
} finally {
    Pop-Location
}
Remove-Item (Join-Path $libClasses 'META-INF/MANIFEST.MF') -Force -ErrorAction SilentlyContinue

$libManifest = Join-Path $build 'lib-manifest.txt'
@"
Automatic-Module-Name: net.tessel.laf
Implementation-Title: Tessel Look and Feel
Implementation-Version: $version
Implementation-Vendor: Tessel
"@ | Set-Content $libManifest -Encoding ascii

$libJar = Join-Path $dist "tessel-laf-$version.jar"
Invoke-Tool jar --create --file $libJar --manifest $libManifest -C $libClasses .
Invoke-Tool jar --create --file (Join-Path $dist "tessel-laf-$version-sources.jar") -C $libSrc .

# ---------------------------------------------------------------- gallery
Write-Host "Compiling tessel-gallery..."
$gallerySrc = Join-Path $root 'tessel-gallery/src/main/java'
Get-Sources $gallerySrc | Set-Content (Join-Path $build 'gallery-sources.txt') -Encoding ascii
Invoke-Tool javac --release 17 -encoding UTF-8 '-Xlint:all,-serial,-this-escape' -cp $libJar -d $galleryClasses "@$(Join-Path $build 'gallery-sources.txt')"
$resources = Join-Path $root 'tessel-gallery/src/main/resources'
Get-ChildItem -Recurse -File $resources | ForEach-Object {
    $target = Join-Path $galleryClasses $_.FullName.Substring($resources.Length)
    New-Item -ItemType Directory -Force (Split-Path $target) | Out-Null
    Copy-Item $_.FullName $target
}

$galleryManifest = Join-Path $build 'gallery-manifest.txt'
@"
Class-Path: tessel-laf-$version.jar
Implementation-Title: Tessel Gallery
Implementation-Version: $version
"@ | Set-Content $galleryManifest -Encoding ascii
$galleryJar = Join-Path $dist "tessel-gallery-$version.jar"
Invoke-Tool jar --create --file $galleryJar --manifest $galleryManifest --main-class net.tessel.gallery.TesselGallery -C $galleryClasses .

Write-Host ""
Get-ChildItem $dist | ForEach-Object { Write-Host ("  {0,-36} {1,8:N0} bytes" -f $_.Name, $_.Length) }

if ($Run) { & $tools.java -jar $galleryJar }
