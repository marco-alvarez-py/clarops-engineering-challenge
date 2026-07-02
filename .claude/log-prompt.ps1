$json = [Console]::In.ReadToEnd()
try {
    $data = $json | ConvertFrom-Json -ErrorAction Stop
    $prompt = $data.prompt
} catch {
    exit 0
}

if ([string]::IsNullOrWhiteSpace($prompt)) { exit 0 }

$aiFile = "AI_USAGE.md"
try {
    $content = if (Test-Path $aiFile) { Get-Content $aiFile -Raw } else { "" }

    $count = ([regex]::Matches($content, "(?m)^## Prompts\s+\d+")).Count + 1

    $words = ($prompt.Trim() -split '\s+') | Select-Object -First 7
    $title = ($words -join ' ').TrimEnd('.', ',', ':', '?', '!')
    if ($title.Length -gt 60) { $title = $title.Substring(0, 57) + "..." }

    $lines = $prompt.Trim() -split "`r?`n"
    $blockquote = ($lines | ForEach-Object { "> $_" }) -join "`n"

    $entry = "`n## Prompts $count - $title`n`n$blockquote`n"
    Add-Content -Path $aiFile -Value $entry -Encoding utf8
} catch {
    exit 0
}