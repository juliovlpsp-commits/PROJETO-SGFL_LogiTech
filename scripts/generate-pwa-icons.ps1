# Gera os icones PNG do PWA a partir do escudo do favicon.svg.
# Uso: powershell -ExecutionPolicy Bypass -File scripts\generate-pwa-icons.ps1
#
# Mantem as mesmas proporcoes do favicon.svg (viewBox 48x56) para a
# identidade visual bater em qualquer tamanho.

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$saida = Join-Path $PSScriptRoot '..\sgfl-frontend\public'

function Novo-Icone {
    param(
        [int]$Tamanho,
        [string]$Arquivo,
        [double]$EscalaEmblema    # 1.0 = escudo ocupa toda a altura util
    )

    $bmp = New-Object System.Drawing.Bitmap($Tamanho, $Tamanho)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

    # Fundo da marca
    $g.Clear([System.Drawing.Color]::FromArgb(255, 0x10, 0x0C, 0x0B))

    # viewBox 48x56 -> tela quadrada
    $escala = ($Tamanho / 56.0) * $EscalaEmblema
    $offX = ($Tamanho - 48.0 * $escala) / 2.0
    $offY = ($Tamanho - 56.0 * $escala) / 2.0

    $tr = New-Object System.Drawing.Drawing2D.Matrix
    $tr.Translate($offX, $offY)
    $tr.Scale($escala, $escala)
    $g.Transform = $tr

    # Escudo: M24 2.5 43.5 8.5 V28 C43.5 41.5 34.5 50.5 24 54
    #          C13.5 50.5 4.5 41.5 4.5 28 V8.5 L24 2.5 Z
    $escudo = New-Object System.Drawing.Drawing2D.GraphicsPath
    $escudo.StartFigure()
    $escudo.AddLine(24.0, 2.5, 43.5, 8.5)
    $escudo.AddLine(43.5, 8.5, 43.5, 28.0)
    $escudo.AddBezier(43.5, 28.0, 43.5, 41.5, 34.5, 50.5, 24.0, 54.0)
    $escudo.AddBezier(24.0, 54.0, 13.5, 50.5, 4.5, 41.5, 4.5, 28.0)
    $escudo.AddLine(4.5, 28.0, 4.5, 8.5)
    $escudo.CloseFigure()

    $grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        (New-Object System.Drawing.PointF(0.0, 0.0)),
        (New-Object System.Drawing.PointF(48.0, 56.0)),
        [System.Drawing.Color]::White,
        [System.Drawing.Color]::Black
    )
    $cores = New-Object System.Drawing.Drawing2D.ColorBlend(3)
    $cores.Colors = @(
        [System.Drawing.Color]::FromArgb(255, 184, 77, 94),
        [System.Drawing.Color]::FromArgb(255, 138, 37, 54),
        [System.Drawing.Color]::FromArgb(255, 94, 26, 39)
    )
    $cores.Positions = @(0.0, 0.55, 1.0)
    $grad.InterpolationColors = $cores

    $borda = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 244, 217, 223), 2.4)
    $borda.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round

    $g.FillPath($grad, $escudo)
    $g.DrawPath($borda, $escudo)

    # Cubo: M24 16 35 22 V34 L24 40 13 34 V22 Z  +  arestas internas
    $cubo = New-Object System.Drawing.Drawing2D.GraphicsPath
    $cubo.StartFigure()
    $cubo.AddLine(24.0, 16.0, 35.0, 22.0)
    $cubo.AddLine(35.0, 22.0, 35.0, 34.0)
    $cubo.AddLine(35.0, 34.0, 24.0, 40.0)
    $cubo.AddLine(24.0, 40.0, 13.0, 34.0)
    $cubo.AddLine(13.0, 34.0, 13.0, 22.0)
    $cubo.CloseFigure()
    $cubo.AddLine(13.0, 22.0, 24.0, 28.0)
    $cubo.AddLine(24.0, 28.0, 35.0, 22.0)
    $cubo.StartFigure()
    $cubo.AddLine(24.0, 28.0, 24.0, 40.0)

    $branco = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 251, 239, 242), 3.0)
    $branco.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
    $branco.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $branco.EndCap = [System.Drawing.Drawing2D.LineCap]::Round

    $g.DrawPath($branco, $cubo)

    $g.Dispose()

    $caminho = Join-Path $saida $Arquivo
    $bmp.Save($caminho, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()

    Write-Output "gerado: $Arquivo ($Tamanho x $Tamanho)"
}

Novo-Icone -Tamanho 192 -Arquivo 'icon-192.png' -EscalaEmblema 0.92
Novo-Icone -Tamanho 512 -Arquivo 'icon-512.png' -EscalaEmblema 0.92
Novo-Icone -Tamanho 512 -Arquivo 'icon-maskable-512.png' -EscalaEmblema 0.62
Novo-Icone -Tamanho 180 -Arquivo 'apple-touch-icon.png' -EscalaEmblema 0.80
