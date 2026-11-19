<#
.SYNOPSIS
    Reproduce en local los cuatro trabajos del CI de BiblioCano, todo dentro de Docker.

.DESCRIPTION
    Por defecto clona el HEAD en una carpeta temporal y verifica ESE clon: el runner de GitHub solo
    ve lo versionado, así que un archivo sin commitear que «hace funcionar» las pruebas aquí, allá
    no existe. Con -ArbolDeTrabajo verifica la carpeta actual (útil mientras se programa).

    Usa un proyecto de Compose aparte (bibliocano-verificar) y el puerto 15433, para no tocar la
    base de uso diario, y lo baja SIEMPRE al terminar, aunque algo falle.

    Trabajos, en el mismo orden del CI:
      1. compilar   mvn test-compile con -Xlint:all -Werror
      2. diagramas  PlantUML renderiza todos los .puml sin errores
      3. pruebas    mvn verify contra PostgreSQL + umbral de cobertura + integración no omitida
      4. imagen     docker build + demo por consola contra PostgreSQL

.EXAMPLE
    .\scripts\verificar.ps1
.EXAMPLE
    .\scripts\verificar.ps1 -ArbolDeTrabajo
#>
param(
    [switch]$ArbolDeTrabajo
)

# 'Continue' y no 'Stop': Docker escribe su progreso en stderr, y en Windows PowerShell 5.1 eso se
# convierte en un error que, con 'Stop', aborta el script. El éxito se decide por $LASTEXITCODE.
$ErrorActionPreference = 'Continue'
$raiz = Split-Path -Parent $PSScriptRoot
$proyecto = 'bibliocano-verificar'
$plantuml = 'plantuml/plantuml:1.2025.4'

if ($ArbolDeTrabajo) {
    $dir = $raiz
    Write-Host "Verificando el árbol de trabajo: $dir"
} else {
    $dir = Join-Path $env:TEMP ("bibliocano-ci-" + [guid]::NewGuid().ToString('N').Substring(0, 8))
    git clone --quiet $raiz $dir
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo clonar el repositorio' }
    $commit = git -C $dir log -1 --format='%h %s'
    Write-Host "Verificando un clon limpio de HEAD ($commit) en $dir"
}

$env:COMPOSE_PROJECT_NAME = $proyecto
$env:BIBLIOCANO_DB_PUERTO = '15433'
$fallos = 0

function Invoke-Trabajo([string]$nombre, [scriptblock]$accion) {
    Write-Host ""
    Write-Host "=== $nombre ===" -ForegroundColor Cyan
    & $accion
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  OK  $nombre" -ForegroundColor Green
    } else {
        Write-Host "  FALLA  $nombre (código $LASTEXITCODE)" -ForegroundColor Red
        $script:fallos++
    }
}

Push-Location $dir
try {
    Invoke-Trabajo '1. compilar' {
        docker compose run --rm --no-deps pruebas mvn -B -ntp -q -DskipTests test-compile
    }

    Invoke-Trabajo '2. diagramas' {
        $salida = Join-Path $dir 'target\verificar-png'
        New-Item -ItemType Directory -Force $salida | Out-Null
        docker run --rm -v "${dir}\docs\diagramas\fuentes:/fuentes:ro" -v "${salida}:/salida" `
            $plantuml -failfast2 -tpng -o /salida '/fuentes/*.puml'
    }

    Invoke-Trabajo '3. pruebas' {
        docker compose up -d --wait db
        if ($LASTEXITCODE -ne 0) { return }
        docker compose run --rm pruebas
        if ($LASTEXITCODE -ne 0) { return }
        $informe = 'target\surefire-reports\TEST-cl.bibliocano.persistencia.JdbcIntegracionTest.xml'
        if (-not (Select-String -Path $informe -Pattern 'skipped="0"' -Quiet)) {
            Write-Host '  JdbcIntegracionTest quedó omitida: no corrió contra PostgreSQL' -ForegroundColor Red
            $global:LASTEXITCODE = 1
        }
    }

    Invoke-Trabajo '4. imagen + demo' {
        docker compose run --rm --build demo
    }
} finally {
    Write-Host ""
    Write-Host "Bajando el entorno de verificación ($proyecto)..."
    docker compose down --remove-orphans *> $null
    # Se borra la base de prueba; el volumen de Maven se conserva para no re-descargar dependencias
    docker volume rm "${proyecto}_datos_postgres" *> $null
    Pop-Location
    Remove-Item Env:COMPOSE_PROJECT_NAME, Env:BIBLIOCANO_DB_PUERTO -ErrorAction SilentlyContinue
    if (-not $ArbolDeTrabajo) {
        Remove-Item -Recurse -Force $dir -ErrorAction SilentlyContinue
    }
}

Write-Host ""
if ($fallos -eq 0) {
    Write-Host 'Todo en verde: se puede commitear y pushear.' -ForegroundColor Green
    exit 0
} else {
    Write-Host "$fallos trabajo(s) en rojo: NO pushees todavía." -ForegroundColor Red
    exit 1
}
