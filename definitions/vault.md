# Vault
Vault is a Paper plugin that provides VaultAPI.

# VaultAPI
VaultAPI is an abstraction library that is implemented by Paper plugins ("Vault Implementers").
<br>
VaultAPI can be used by Paper plugins to access features Vault Implementers provide through VaultAPI ("Vault Users").
<br>

## Availability
Each of Vault's features must be implemented by an Implementer if Vault Users want to access that feature.
<br>
If there is no Implementer that implements a feature, that feature is "not available".
<br>
Conversely, if an Implementer implements one or more features, those features are "available".

## Economy
Economy is a feature provided by VaultAPI to add an economy into the Server.

### Balance
Each player is given a Balance.
<br>
This is typically interpreted as the amount of "money" the player has.