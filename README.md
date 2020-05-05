# Vigo Azure AD Service

# Configuration

| Key | Description | Default value |
|-----|---------------|-------------|
| fint.azure.client.id |  | |
| fint.azure.client.secret |  | |
| fint.azure.client.scopes | https | |
| fint.azure.tenant |  | |
| fint.azure.invite.base-url | http | |
| fint.azure.qlik.users.owner | hans@vigoikt.no | |
| fint.azure.qlik.redirect-url | https | |
| fint.azure.qlik.send-invitation | false | |
| fint.azure.qlik.allowed-domains |  | |
| fint.invite.mail.serviceaccount | serviceaccount.json | |
| fint.orgmonitor.gmail.scopes | https | |
| fint.orgmonitor.gmail.delegate | frode@fintlabs.no | |
| fint.orgmonitor.application-name | JADA | |


# Help full commands
## Delete all users with status `PendingAcceptance`
`az ad user list | jq -r '.[] | select(.userState=="PendingAcceptance").objectId' | while read s; do az ad user delete --id $s; done`