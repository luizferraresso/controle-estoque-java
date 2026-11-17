$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

try {
    $arquivoJar = Join-Path $PSScriptRoot 'ControleEstoque.jar'
    $arquivoPid = Join-Path $PSScriptRoot 'build\app.pid'
    $driver = Join-Path $PSScriptRoot 'lib\h2-2.5.252.jar'
    if (-not (Test-Path -LiteralPath $driver)) {
        throw 'Driver H2 ausente. Mantenha a pasta lib junto ao executavel.'
    }

    if (Test-Path -LiteralPath $arquivoPid) {
        $idAplicacao = 0
        if ([int]::TryParse((Get-Content -LiteralPath $arquivoPid -Raw).Trim(), [ref]$idAplicacao)) {
            $processoExistente = Get-Process -Id $idAplicacao -ErrorAction SilentlyContinue
            if ($processoExistente -and $processoExistente.ProcessName -in @('java', 'javaw') -and
                $processoExistente.MainWindowTitle -eq 'Controle de Estoque | Trabalho de POO') {
                Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public static class JanelaEstoque {
    [DllImport("user32.dll")]
    public static extern bool ShowWindowAsync(IntPtr janela, int comando);
    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(IntPtr janela);
}
'@
                [void][JanelaEstoque]::ShowWindowAsync($processoExistente.MainWindowHandle, 9)
                [void][JanelaEstoque]::SetForegroundWindow($processoExistente.MainWindowHandle)
                Write-Host 'O Controle de Estoque ja esta aberto. Sua janela foi restaurada.'
                exit 0
            }
        }
    }

    if (-not (Test-Path -LiteralPath $arquivoJar)) {
        & (Join-Path $PSScriptRoot 'executar.ps1') -Acao compilar
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $arquivoJar)) {
            throw 'Nao foi possivel gerar ControleEstoque.jar.'
        }
    }
    $java = Get-Command java.exe -ErrorAction SilentlyContinue
    if (-not $java) { throw 'Instale Java 17 ou superior e inclua java no PATH.' }
    $javaGrafico = Join-Path (Split-Path $java.Source -Parent) 'javaw.exe'
    if (-not (Test-Path -LiteralPath $javaGrafico)) { $javaGrafico = $java.Source }
    New-Item -ItemType Directory -Force -Path 'build' | Out-Null
    $processoNovo = Start-Process -FilePath $javaGrafico -ArgumentList '-jar', ('"' + $arquivoJar + '"') -WorkingDirectory $PSScriptRoot -WindowStyle Hidden -PassThru
    $processoNovo.Id | Set-Content -LiteralPath $arquivoPid
    Write-Host 'Controle de Estoque iniciado. Voce pode fechar este terminal.'
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
