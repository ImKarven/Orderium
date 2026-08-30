## UPDATES
This version fixes a lot (I mean A LOT) of issues in the plugin.
<br>
Updating is recommended to provide a more stable experience.

### Fixes
- Similarity check only accepts valued data component types
- Possible exploit with interacting choose item GUI
- Race condition when logging money transactions
- Duplicating keys when storing money transactions
- Thread-safety issues with search GUI and delivery confirm dialog
- Possible undefined behavior because of access of `Bukkit.getOnlinePlayers()` asynchronously
- Thread-safety issues when modifying order values
- Possible race condition when deducting money (more like Vault Implementer's job but eh)
- Orders never get deleted in storage
- Items that represent orders in main and your orders GUI don't have expires time updated unless their values are changed
- Possible issue with byte array comparison for orders and order items
- Possible exception when orders failed to load
- Updates to orders are not atomic and don't have proper locking
- Connection changes are not rolled back on exception in storage

### News
- Item of an order now shows its tooltip on hover when sent as a text message

### License
License is now GPL-3.0 for both the GitHub repository and Modrinth project, as required by ConfigurationMaster's license.

### Disclosure
As to adapt to Modrinth's new project disclosures system, Orderium now has disclosures.
<br>
You can view this on the main page of the Modrinth project.