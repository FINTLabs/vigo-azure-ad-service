# Vigo Azure AD Service

## Delete all users with status `PendingAcceptance`
`az ad user list | jq -r '.[] | select(.userState=="PendingAcceptance").objectId' | while read s; do az ad user delete --id $s; done`