# Simple WS test script
param()

$nick = 'wsTester_' + (Get-Date -Format 'HHmmss')
Write-Output "Nick: $nick"

$login = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/chat/login' -ContentType 'application/json' -Body (ConvertTo-Json @{ nickname = $nick })
$userId = $login.data.user.id
$privateChannel = $login.data.privateChannel.id
Write-Output "Logged in userId=$userId privateChannel=$privateChannel"

# Connect to WebSocket
$ws = New-Object System.Net.WebSockets.ClientWebSocket
$uri = [Uri]::new("ws://localhost:8080/ws/chat?userId=$userId")
$ws.ConnectAsync($uri, [Threading.CancellationToken]::None).Wait()
Write-Output 'Connected WS'
Start-Sleep -Milliseconds 500

# Post a message
$msgBody = @{ channelId = $privateChannel; userId = $userId; content = 'Hello from wsTester at ' + (Get-Date -Format 'HH:mm:ss') } | ConvertTo-Json
Write-Output 'Posting message: ' + $msgBody
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/chat/messages' -ContentType 'application/json' -Body $msgBody | ConvertTo-Json | Write-Output

Write-Output 'Listening for WS frames for 6 seconds...'
$buffer = [System.Array]::CreateInstance([byte],65536)
$start = [DateTime]::UtcNow
while (([DateTime]::UtcNow - $start).TotalSeconds -lt 6) {
    $result = $ws.ReceiveAsync([System.ArraySegment[byte]]::new($buffer), [Threading.CancellationToken]::None).Result
    if ($result.Count -gt 0) {
        $msg = [System.Text.Encoding]::UTF8.GetString($buffer,0,$result.Count)
        Write-Output 'WS frame: ' + $msg
    }
}
$ws.Dispose()
Write-Output 'Done'
