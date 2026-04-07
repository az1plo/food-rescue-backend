param()

$ErrorActionPreference = 'Stop'

function Get-EnvOrDefault {
    param(
        [string]$Name,
        [string]$DefaultValue
    )

    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        return $DefaultValue
    }
    return $value
}

function Get-BoolEnvOrDefault {
    param(
        [string]$Name,
        [bool]$DefaultValue
    )

    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        return $DefaultValue
    }

    $normalized = $value.Trim().ToLowerInvariant()
    return @('1', 'true', 'yes', 'y', 'on') -contains $normalized
}

$KeycloakUrl = (Get-EnvOrDefault -Name 'KEYCLOAK_URL' -DefaultValue 'http://localhost:8081').TrimEnd('/')
$AdminRealm = Get-EnvOrDefault -Name 'ADMIN_REALM' -DefaultValue 'master'
$AdminUser = Get-EnvOrDefault -Name 'ADMIN_USER' -DefaultValue 'admin'
$AdminPassword = Get-EnvOrDefault -Name 'ADMIN_PASSWORD' -DefaultValue 'admin'
$RealmName = Get-EnvOrDefault -Name 'REALM_NAME' -DefaultValue 'FSA'
$ClientId = Get-EnvOrDefault -Name 'CLIENT_ID' -DefaultValue 'fsa-client'
$PreferredClientSecret = Get-EnvOrDefault -Name 'CLIENT_SECRET' -DefaultValue 'fsa-client-secret'
$ForceRecreateRealm = Get-BoolEnvOrDefault -Name 'FORCE_RECREATE_REALM' -DefaultValue $true

$Roles = @('ADMIN', 'TEACHER', 'STUDENT')
$Users = @(
    @{ Username = 'admin@posam.sk'; Password = 'admin123'; Role = 'ADMIN'; FirstName = 'Workshop'; LastName = 'Admin' },
    @{ Username = 'teacher@posam.sk'; Password = 'teacher123'; Role = 'TEACHER'; FirstName = 'Workshop'; LastName = 'Teacher' },
    @{ Username = 'student@posam.sk'; Password = 'student123'; Role = 'STUDENT'; FirstName = 'Workshop'; LastName = 'Student' }
)

$script:AccessToken = $null
$script:ClientSecretValue = $null

function Write-Log {
    param([string]$Message)
    Write-Host "[keycloak-setup] $Message"
}

function Get-AuthHeaders {
    return @{ Authorization = "Bearer $($script:AccessToken)" }
}

function ConvertTo-JsonBody {
    param($Payload)
    return (ConvertTo-Json -InputObject $Payload -Depth 20 -Compress)
}

function ConvertTo-Array {
    param($Value)

    if ($null -eq $Value) {
        return @()
    }

    if ($Value -is [System.Array]) {
        return $Value
    }

    return @($Value)
}

function Invoke-ApiGet {
    param([string]$Path)

    return Invoke-RestMethod -Method Get -Uri "$KeycloakUrl$Path" -Headers (Get-AuthHeaders)
}

function Invoke-ApiPost {
    param(
        [string]$Path,
        $Payload
    )

    Invoke-WebRequest -Method Post -Uri "$KeycloakUrl$Path" -Headers (Get-AuthHeaders) -ContentType 'application/json' -Body (ConvertTo-JsonBody -Payload $Payload) | Out-Null
}

function Invoke-ApiPut {
    param(
        [string]$Path,
        $Payload
    )

    Invoke-WebRequest -Method Put -Uri "$KeycloakUrl$Path" -Headers (Get-AuthHeaders) -ContentType 'application/json' -Body (ConvertTo-JsonBody -Payload $Payload) | Out-Null
}

function Invoke-ApiDelete {
    param(
        [string]$Path,
        $Payload = $null
    )

    if ($null -ne $Payload) {
        Invoke-WebRequest -Method Delete -Uri "$KeycloakUrl$Path" -Headers (Get-AuthHeaders) -ContentType 'application/json' -Body (ConvertTo-JsonBody -Payload $Payload) | Out-Null
    } else {
        Invoke-WebRequest -Method Delete -Uri "$KeycloakUrl$Path" -Headers (Get-AuthHeaders) | Out-Null
    }
}

function Get-StatusCode {
    param(
        [string]$Method,
        [string]$Path,
        $Payload = $null
    )

    $params = @{
        Method  = $Method
        Uri     = "$KeycloakUrl$Path"
        Headers = (Get-AuthHeaders)
    }

    if ($null -ne $Payload) {
        $params['ContentType'] = 'application/json'
        $params['Body'] = (ConvertTo-JsonBody -Payload $Payload)
    }

    $supportsSkipHttpErrorCheck = (Get-Command Invoke-WebRequest).Parameters.ContainsKey('SkipHttpErrorCheck')
    if ($supportsSkipHttpErrorCheck) {
        $params['SkipHttpErrorCheck'] = $true
        $response = Invoke-WebRequest @params
        return [int]$response.StatusCode
    }

    try {
        $response = Invoke-WebRequest @params
        return [int]$response.StatusCode
    } catch {
        $httpResponse = $_.Exception.Response
        if ($null -ne $httpResponse -and ($httpResponse.PSObject.Properties.Name -contains 'StatusCode')) {
            return [int]$httpResponse.StatusCode
        }
        throw
    }
}

function Get-AdminToken {
    $tokenResponse = Invoke-RestMethod -Method Post -Uri "$KeycloakUrl/realms/$AdminRealm/protocol/openid-connect/token" -ContentType 'application/x-www-form-urlencoded' -Body @{
        client_id  = 'admin-cli'
        grant_type = 'password'
        username   = $AdminUser
        password   = $AdminPassword
    }

    if ([string]::IsNullOrWhiteSpace($tokenResponse.access_token)) {
        throw 'Unable to obtain admin access token. Check KEYCLOAK_URL/ADMIN_USER/ADMIN_PASSWORD.'
    }

    $script:AccessToken = $tokenResponse.access_token
}

function Ensure-Realm {
    $status = Get-StatusCode -Method 'GET' -Path "/admin/realms/$RealmName"

    if ($status -eq 200 -and $ForceRecreateRealm) {
        Write-Log "Realm $RealmName exists, deleting and recreating."
        Invoke-ApiDelete -Path "/admin/realms/$RealmName"
        $status = 404
    }

    if ($status -eq 404) {
        Write-Log "Creating realm $RealmName."
        Invoke-ApiPost -Path '/admin/realms' -Payload @{ realm = $RealmName; enabled = $true }
    } elseif ($status -eq 200) {
        Write-Log "Realm $RealmName already exists, reusing."
    } else {
        throw "Unexpected realm status code: $status"
    }
}

function Ensure-RealmRole {
    param([string]$RoleName)

    $encodedRole = [uri]::EscapeDataString($RoleName)
    $status = Get-StatusCode -Method 'GET' -Path "/admin/realms/$RealmName/roles/$encodedRole"

    if ($status -eq 404) {
        Write-Log "Creating role $RoleName."
        Invoke-ApiPost -Path "/admin/realms/$RealmName/roles" -Payload @{ name = $RoleName }
    } elseif ($status -eq 200) {
        Write-Log "Role $RoleName already exists."
    } else {
        throw "Unexpected role status code for ${RoleName}: $status"
    }
}

function Get-ClientUuid {
    $encodedClientId = [uri]::EscapeDataString($ClientId)
    $clients = ConvertTo-Array (Invoke-ApiGet -Path "/admin/realms/$RealmName/clients?clientId=$encodedClientId")
    if ($clients.Count -eq 0) {
        return $null
    }
    return $clients[0].id
}

function Ensure-Client {
    $clientUuid = Get-ClientUuid

    $payload = @{
        clientId                  = $ClientId
        name                      = $ClientId
        enabled                   = $true
        protocol                  = 'openid-connect'
        publicClient              = $false
        clientAuthenticatorType   = 'client-secret'
        secret                    = $PreferredClientSecret
        standardFlowEnabled       = $true
        directAccessGrantsEnabled = $true
        serviceAccountsEnabled    = $false
        implicitFlowEnabled       = $false
        redirectUris              = @('*')
        webOrigins                = @('*')
        attributes                = @{ 'post.logout.redirect.uris' = '*' }
    }

    if ([string]::IsNullOrWhiteSpace($clientUuid)) {
        Write-Log "Creating client $ClientId (confidential)."
        Invoke-ApiPost -Path "/admin/realms/$RealmName/clients" -Payload $payload
        $clientUuid = Get-ClientUuid
    } else {
        Write-Log "Updating existing client $ClientId."
        Invoke-ApiPut -Path "/admin/realms/$RealmName/clients/$clientUuid" -Payload $payload
    }

    if ([string]::IsNullOrWhiteSpace($clientUuid)) {
        throw "Client $ClientId was not found after create/update."
    }

    $secretPayload = Invoke-ApiGet -Path "/admin/realms/$RealmName/clients/$clientUuid/client-secret"
    $script:ClientSecretValue = $secretPayload.value
}

function Get-UserId {
    param([string]$Username)

    $encodedUsername = [uri]::EscapeDataString($Username)
    $users = ConvertTo-Array (Invoke-ApiGet -Path "/admin/realms/$RealmName/users?username=$encodedUsername&exact=true")
    if ($users.Count -eq 0) {
        return $null
    }

    return $users[0].id
}

function Set-UserPassword {
    param(
        [string]$UserId,
        [string]$Password
    )

    Invoke-ApiPut -Path "/admin/realms/$RealmName/users/$UserId/reset-password" -Payload @{
        type      = 'password'
        temporary = $false
        value     = $Password
    }
}

function Set-UserSingleRole {
    param(
        [string]$UserId,
        [string]$RoleName
    )

    $currentRoles = ConvertTo-Array (Invoke-ApiGet -Path "/admin/realms/$RealmName/users/$UserId/role-mappings/realm")
    if ($currentRoles.Count -gt 0) {
        Invoke-ApiDelete -Path "/admin/realms/$RealmName/users/$UserId/role-mappings/realm" -Payload $currentRoles
    }

    $roleRepresentation = Invoke-ApiGet -Path "/admin/realms/$RealmName/roles/$RoleName"
    Invoke-ApiPost -Path "/admin/realms/$RealmName/users/$UserId/role-mappings/realm" -Payload @($roleRepresentation)
}

function Resolve-Email {
    param([string]$Username)
    if ($Username.Contains('@')) {
        return $Username
    }
    return "$Username@fsa.local"
}

function Ensure-User {
    param(
        [hashtable]$User
    )

    $username = $User.Username
    $password = $User.Password
    $role = $User.Role
    $firstName = $User.FirstName
    $lastName = $User.LastName
    $email = Resolve-Email -Username $username

    $userId = Get-UserId -Username $username

    $userPayload = @{
        username        = $username
        email           = $email
        firstName       = $firstName
        lastName        = $lastName
        enabled         = $true
        emailVerified   = $true
        requiredActions = @()
    }

    if ([string]::IsNullOrWhiteSpace($userId)) {
        Write-Log "Creating user $username."
        Invoke-ApiPost -Path "/admin/realms/$RealmName/users" -Payload $userPayload
        $userId = Get-UserId -Username $username
    } else {
        Write-Log "User $username already exists, updating profile/password/role mapping."
    }

    if ([string]::IsNullOrWhiteSpace($userId)) {
        throw "Failed to resolve user id for $username"
    }

    Invoke-ApiPut -Path "/admin/realms/$RealmName/users/$userId" -Payload $userPayload
    Set-UserPassword -UserId $userId -Password $password
    Set-UserSingleRole -UserId $userId -RoleName $role
}

function Print-Summary {
    Write-Host ''
    Write-Host 'Setup completed.'
    Write-Host ''
    Write-Host "Realm: $RealmName"
    Write-Host "Client ID: $ClientId"
    Write-Host "Client secret: $script:ClientSecretValue"
    Write-Host ''
    Write-Host 'Users:'
    Write-Host '- admin@posam.sk / admin123 / ADMIN'
    Write-Host '- teacher@posam.sk / teacher123 / TEACHER'
    Write-Host '- student@posam.sk / student123 / STUDENT'
    Write-Host ''
    Write-Host 'Useful env overrides:'
    Write-Host '- KEYCLOAK_URL (default: http://localhost:8081)'
    Write-Host '- ADMIN_USER (default: admin)'
    Write-Host '- ADMIN_PASSWORD (default: admin)'
    Write-Host '- REALM_NAME (default: FSA)'
    Write-Host '- CLIENT_ID (default: fsa-client)'
    Write-Host '- CLIENT_SECRET (default: fsa-client-secret, Keycloak may keep existing one for existing client)'
    Write-Host '- FORCE_RECREATE_REALM=true|false (default: true)'
}

function Main {
    Get-AdminToken
    Ensure-Realm

    foreach ($role in $Roles) {
        Ensure-RealmRole -RoleName $role
    }

    Ensure-Client

    foreach ($user in $Users) {
        Ensure-User -User $user
    }

    Print-Summary
}

Main
