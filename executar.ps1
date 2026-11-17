param(
    [ValidateSet('executar', 'compilar', 'testar')]
    [string]$Acao = 'executar',
    [switch]$Interface
)

$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

try {
    if ($Acao -eq 'executar') {
        & (Join-Path $PSScriptRoot 'iniciar.ps1')
        exit $LASTEXITCODE
    }
    if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
        throw 'Instale um JDK 17 ou superior e inclua a pasta bin no PATH. O comando javac precisa estar disponível.'
    }
    $driver = Join-Path $PSScriptRoot 'lib\h2-2.5.252.jar'
    if (-not (Test-Path -LiteralPath $driver)) {
        throw 'Driver H2 ausente: lib\h2-2.5.252.jar. Restaure esse arquivo junto com o projeto.'
    }
    New-Item -ItemType Directory -Force -Path 'build\classes', 'build\test-classes' | Out-Null
    $fontes = @(Get-ChildItem -LiteralPath 'src\main\java' -Filter '*.java' -Recurse | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' })
    $utf8 = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllLines((Join-Path $PSScriptRoot 'build\sources.txt'), $fontes, $utf8)
    & javac -encoding UTF-8 --release 17 -cp $driver -d 'build\classes' '@build/sources.txt'
    if ($LASTEXITCODE -ne 0) { throw 'Falha na compilação da aplicação.' }

    if ($Acao -eq 'testar') {
        $testes = @(Get-ChildItem -LiteralPath 'src\test\java' -Filter '*.java' -Recurse | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' })
        [System.IO.File]::WriteAllLines((Join-Path $PSScriptRoot 'build\tests.txt'), $testes, $utf8)
        & javac -encoding UTF-8 --release 17 -cp "build\classes;$driver" -d 'build\test-classes' '@build/tests.txt'
        if ($LASTEXITCODE -ne 0) { throw 'Falha na compilação dos testes.' }
        & java -ea -cp "build\classes;build\test-classes;$driver" br.edu.estoque.TesteIntegracao
        if ($LASTEXITCODE -ne 0) { throw 'Os testes de integração falharam.' }
        if ($Interface) {
            & java -ea -cp "build\classes;build\test-classes;$driver" br.edu.estoque.TesteInterface
            if ($LASTEXITCODE -ne 0) { throw 'O teste da interface falhou.' }
        }
    } else {
        $arquivoJar = Join-Path $PSScriptRoot 'ControleEstoque.jar'
        if (Test-Path -LiteralPath $arquivoJar) {
            try {
                $acessoJar = [System.IO.File]::Open($arquivoJar, [System.IO.FileMode]::Open, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
                $acessoJar.Dispose()
            } catch [System.IO.IOException] {
                throw 'Feche a janela do Controle de Estoque antes de recompilar com compilar.bat. Para abrir o programa, use iniciar.bat.'
            }
        }
        $manifesto = "Manifest-Version: 1.0`r`nMain-Class: br.edu.estoque.App`r`nClass-Path: lib/h2-2.5.252.jar`r`n`r`n"
        [System.IO.File]::WriteAllText((Join-Path $PSScriptRoot 'build\MANIFEST.MF'), $manifesto, $utf8)
        & jar --create --file 'build\ControleEstoque.jar' --manifest 'build\MANIFEST.MF' -C 'build\classes' .
        if ($LASTEXITCODE -ne 0) { throw 'Falha ao empacotar o JAR.' }
        Copy-Item -LiteralPath 'build\ControleEstoque.jar' -Destination $arquivoJar -Force
        Write-Host 'Compilado: ControleEstoque.jar (mantenha a pasta lib ao lado do JAR).'
    }
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
